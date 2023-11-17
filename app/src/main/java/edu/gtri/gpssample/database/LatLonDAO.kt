package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*

class LatLonDAO(private var dao: DAO)
{
    fun createOrUpdateLatLon(latLon: LatLon, geoArea : GeoArea? ) : LatLon?
    {
        if (exists( latLon ))
        {
            updateLatLon( latLon )
        }
        else
        {
            val values = ContentValues()
            putLatLon( latLon, values )
            latLon.id = dao.writableDatabase.insert(DAO.TABLE_LAT_LON, null, values).toInt()
        }

        latLon.id?.let { latLonId ->
            geoArea?.let { geoArea ->
                val values = ContentValues()
                when (geoArea) {
                    is EnumArea ->
                    {
                        geoArea.id?.let{ geo_area_id->
                            val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA__LAT_LON} WHERE ${DAO.COLUMN_LAT_LON_ID} = $latLonId AND ${DAO.COLUMN_ENUM_AREA_ID} = $geo_area_id"
                            val cursor = dao.writableDatabase.rawQuery(query, null)
                            if (cursor.count == 0)
                            {
                                putLatLonEnumArea( latLonId, geo_area_id, values)
                                dao.writableDatabase.insert(DAO.TABLE_ENUM_AREA__LAT_LON, null, values)
                            }
                            cursor.close()
                        }
                    }
                    is SampleArea ->
                    {
                        geoArea.id?.let{ geo_area_id->
                            val query = "SELECT * FROM ${DAO.TABLE_SAMPLE_AREA__LAT_LON} WHERE ${DAO.COLUMN_LAT_LON_ID} = $latLonId AND ${DAO.COLUMN_SAMPLE_AREA_ID} = $geo_area_id"
                            val cursor = dao.writableDatabase.rawQuery(query, null)
                            if (cursor.count == 0)
                            {
                                putLatLonSampleArea( latLonId, geo_area_id, values)
                                dao.writableDatabase.insert(DAO.TABLE_SAMPLE_AREA__LAT_LON, null, values)
                            }
                            cursor.close()
                        }
                    }
                    else -> {
                    }
                }
            }
        } ?: return null

        return latLon
    }

    private fun putLatLonTeam(llID : Int, teamId : Int, values : ContentValues)
    {
        values.put( DAO.COLUMN_LAT_LON_ID, llID )
        values.put( DAO.COLUMN_ENUMERATION_TEAM_ID, teamId )
    }

    private fun putLatLonEnumArea(llID : Int, enumAreaId: Int, values : ContentValues)
    {
        values.put( DAO.COLUMN_LAT_LON_ID, llID )
        values.put( DAO.COLUMN_ENUM_AREA_ID, enumAreaId )
    }

    private fun putLatLonSampleArea(llID : Int, sampleAreaId: Int, values : ContentValues)
    {
        values.put( DAO.COLUMN_LAT_LON_ID, llID )
        values.put( DAO.COLUMN_SAMPLE_AREA_ID, sampleAreaId )
    }

    private fun putLatLon(latLon: LatLon, values: ContentValues)
    {
        latLon.id?.let { id ->
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_LAT, latLon.latitude )
        values.put( DAO.COLUMN_LON, latLon.longitude )
    }

    fun exists( latLon: LatLon ): Boolean
    {
        latLon.id?.let { id ->
            getLatLon( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    @SuppressLint("Range")
    private fun createLatLon(cursor: Cursor): LatLon
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LAT))
        val lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LON))

        return LatLon( id, lat, lon )
    }

    fun updateLatLon( latLon: LatLon )
    {
        latLon.id?.let{ id ->
            Log.d( "xxx", "update latLon id ${id}")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putLatLon( latLon, values )

            dao.writableDatabase.update(DAO.TABLE_LAT_LON, values, whereClause, args )
        }
    }

    fun getLatLon( id : Int ): LatLon?
    {
        var latLon: LatLon? = null
        val query = "SELECT * FROM ${DAO.TABLE_LAT_LON} where id=${id}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            latLon = createLatLon( cursor )
        }

        cursor.close()

        return latLon
    }

    fun getLatLonsWithEnumAreaId( enumAreaId: Int ): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()
        val query = "SELECT LL.* FROM ${DAO.TABLE_LAT_LON} AS LL, ${DAO.TABLE_ENUM_AREA__LAT_LON} ELL WHERE" +
                " ELL.${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND LL.ID = ELL.${DAO.COLUMN_LAT_LON_ID}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = createLatLon(cursor)

            latLons.add( latlon )
        }

        cursor.close()

        return latLons
    }

    fun getLatLonsWithSampleAreaId( sampleAreaId: Int ): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()
        val query = "SELECT LL.* FROM ${DAO.TABLE_LAT_LON} AS LL, ${DAO.TABLE_SAMPLE_AREA__LAT_LON} ELL WHERE" +
                " ELL.${DAO.COLUMN_SAMPLE_AREA_ID} = $sampleAreaId AND LL.ID = ELL.${DAO.COLUMN_LAT_LON_ID}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = createLatLon(cursor)

            latLons.add( latlon )
        }

        cursor.close()

        return latLons
    }

    fun getLatLonsWithEnumerationTeamId( teamId: Int ): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()
        val query = "SELECT LL.* FROM ${DAO.TABLE_LAT_LON} AS LL, ${DAO.TABLE_ENUMERATION_TEAM__LAT_LON} ELL WHERE" +
                " ELL.${DAO.COLUMN_ENUMERATION_TEAM_ID} = $teamId AND LL.ID = ELL.${DAO.COLUMN_LAT_LON_ID}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = createLatLon(cursor)

            latLons.add( latlon )
        }

        cursor.close()

        return latLons
    }

    fun getLatLonsWithCollectionTeamId( teamId: Int ): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()
        val query = "SELECT LL.* FROM ${DAO.TABLE_LAT_LON} AS LL, ${DAO.TABLE_COLLECTION_TEAM__LAT_LON} ELL WHERE" +
                " ELL.${DAO.COLUMN_COLLECTION_TEAM_ID} = $teamId AND LL.ID = ELL.${DAO.COLUMN_LAT_LON_ID}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = createLatLon(cursor)

            latLons.add( latlon )
        }

        cursor.close()

        return latLons
    }

    fun getLatLons(): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()
        val query = "SELECT * FROM ${DAO.TABLE_LAT_LON}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            latLons.add( createLatLon( cursor ))
        }

        cursor.close()

        return latLons
    }

    fun delete( latLon: LatLon )
    {
        latLon.id?.let {lat_lon_id ->

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(lat_lon_id.toString())

            dao.writableDatabase.delete(DAO.TABLE_LAT_LON, whereClause, args)
        }
    }
}
