package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*

class LatLonDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateLatLon( latLon: LatLon, area : GeoArea?, team : Team? ) : LatLon?
    {
        if (exists( latLon ))
        {
            updateLatLon( latLon )
        }
        else
        {
            var values = ContentValues()

            putLatLon( latLon, values )

            latLon.id = dao.writableDatabase.insert(DAO.TABLE_LAT_LON, null, values).toInt()
            latLon.id?.let { id ->
                // insert into conenctor

                area?.let{area ->
                    values.clear()
                    if (area is EnumArea)
                    {
                        area.id?.let{area_id->
                            putLatLonEnumArea(id, area_id, values)
                            dao.writableDatabase.insert(DAO.TABLE_ENUM_AREA_LAT_LON, null, values)
                        }
                    }
                }
                team?.id?.let{team_id->
                    values.clear()
                    val ll = latLon
                    Log.d("xxx", "the lat lon ${ll.latitude}, ${ll.longitude} team id ${team_id} and id $id")
                    putLatLonTeam(id, team_id, values)
                    dao.writableDatabase.insert(DAO.TABLE_TEAM_LAT_LON, null, values)

                }
//                Log.d( "xxx", "new LatLon id = ${id}")
            } ?: return null
        }

        return latLon
    }

    private fun putLatLonTeam(llID : Int, teamId : Int, values : ContentValues)
    {
        values.put( DAO.COLUMN_LAT_LON_ID, llID )
        values.put( DAO.COLUMN_TEAM_ID, teamId )
    }
    private fun putLatLonEnumArea(llID : Int, enumAreaId: Int, values : ContentValues)
    {
        values.put( DAO.COLUMN_LAT_LON_ID, llID )
        values.put( DAO.COLUMN_ENUM_AREA_ID, enumAreaId )
    }
    //--------------------------------------------------------------------------
    private fun putLatLon(latLon: LatLon, values: ContentValues)
    {
        latLon.id?.let { id ->
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_LAT, latLon.latitude )
        values.put( DAO.COLUMN_LON, latLon.longitude )
    }

    //--------------------------------------------------------------------------
    fun exists( latLon: LatLon ): Boolean
    {
        latLon.id?.let { id ->
            getLatLon( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createLatLon(cursor: Cursor): LatLon
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LAT))
        val lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LON))

        return LatLon( id, lat, lon )
    }

    //--------------------------------------------------------------------------
    fun getLatLon( id : Int ): LatLon?
    {
        var latLon: LatLon? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_LAT_LON} where id=${id}"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            latLon = createLatLon( cursor )
        }

        cursor.close()
        db.close()

        return latLon
    }

    fun getAllLatLonsWithEnumAreaId( enumAreaId: Int ): ArrayList<LatLon>
    {
        var latLons = ArrayList<LatLon>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_LAT_LON} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            latLons.add( createLatLon( cursor ))
        }

        cursor.close()
        db.close()

        return latLons
    }

    fun getLatLonsWithEnumAreaId( enumAreaId: Int ): ArrayList<LatLon>
    {
        var latLons = ArrayList<LatLon>()
        val db = dao.writableDatabase
        val query = "SELECT LL.* FROM ${DAO.TABLE_LAT_LON} AS LL, ${DAO.TABLE_ENUM_AREA_LAT_LON} ELL WHERE" +
                " ELL.${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND LL.ID = ELL.${DAO.COLUMN_LAT_LON_ID}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = createLatLon(cursor)

            latLons.add( latlon )
        }

        cursor.close()
        db.close()

        return latLons
    }

    fun getLatLonsWithTeamId( teamId: Int ): ArrayList<LatLon>
    {
        var latLons = ArrayList<LatLon>()
        val db = dao.writableDatabase
        val query = "SELECT LL.* FROM ${DAO.TABLE_LAT_LON} AS LL, ${DAO.TABLE_TEAM_LAT_LON} ELL WHERE" +
                " ELL.${DAO.COLUMN_TEAM_ID} = $teamId AND LL.${DAO.COLUMN_ID} = ELL.${DAO.COLUMN_LAT_LON_ID}"
        val cursor = db.rawQuery(query, null)

        Log.d("XXXXXSQL", "$query")
        while (cursor.moveToNext())
        {
            latLons.add( createLatLon( cursor ))
        }

        cursor.close()
        db.close()

        return latLons
    }

    fun getLatLons(): ArrayList<LatLon>
    {
        var latLons = ArrayList<LatLon>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_LAT_LON}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            latLons.add( createLatLon( cursor ))
        }

        cursor.close()
        db.close()

        return latLons
    }

    //--------------------------------------------------------------------------
    fun updateLatLon( latLon: LatLon )
    {
        val db = dao.writableDatabase

        latLon.id?.let{ id ->
            Log.d( "xxx", "update latLon id ${id}")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putLatLon( latLon, values )

            db.update(DAO.TABLE_LAT_LON, values, whereClause, args )
        }

        db.close()
    }

    //--------------------------------------------------------------------------
    fun delete( latLon: LatLon )
    {
        latLon.id?.let {lat_lon_id ->

            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(lat_lon_id.toString())

            db.delete(DAO.TABLE_LAT_LON, whereClause, args)
            db.close()
        }
    }
}
