package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*

class LatLonDAO(private var dao: DAO)
{
    fun createOrUpdateLatLon( latLon: LatLon, enumArea : EnumArea?, config: Config? ) : LatLon?
    {
        if (exists( latLon ))
        {
            updateLatLon( latLon )
        }
        else
        {
            val values = ContentValues()
            putLatLon( latLon, values )
            if (dao.writableDatabase.insert(DAO.TABLE_LAT_LON, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "created LatLon with ID ${latLon.uuid}" )
        }

        enumArea?.let { enumArea ->
            val values = ContentValues()
            val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_ENUM_AREA__LAT_LON} WHERE ${DAO.COLUMN_LAT_LON_UUID} = '${latLon.uuid}' AND ${DAO.COLUMN_ENUM_AREA_UUID} = '${enumArea.uuid}'"
            val cursor = dao.writableDatabase.rawQuery(query, null)
            if (cursor.count == 0)
            {
                putLatLonEnumArea( latLon.uuid, enumArea.uuid, values)
                dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_ENUM_AREA__LAT_LON, null, values)
            }
            cursor.close()
        }

        return latLon
    }

    private fun putLatLonConfig(llID : String, configUUID: String, values : ContentValues)
    {
        values.put( DAO.COLUMN_LAT_LON_UUID, llID )
        values.put( DAO.COLUMN_CONFIG_UUID, configUUID )
    }

    private fun putLatLonEnumArea(llID : String, enumAreaUuid: String, values : ContentValues)
    {
        values.put( DAO.COLUMN_LAT_LON_UUID, llID )
        values.put( DAO.COLUMN_ENUM_AREA_UUID, enumAreaUuid )
    }

    private fun putLatLon(latLon: LatLon, values: ContentValues)
    {
        values.put( DAO.COLUMN_UUID, latLon.uuid )
        values.put( DAO.COLUMN_INDEX, latLon.index )
        values.put( DAO.COLUMN_LAT, latLon.latitude )
        values.put( DAO.COLUMN_LON, latLon.longitude )
    }

    fun exists( latLon: LatLon ): Boolean
    {
        getLatLon( latLon.uuid )?.let {
            return true
        } ?: return false
    }

    @SuppressLint("Range")
    private fun buildLatLon(cursor: Cursor): LatLon
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val index = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_INDEX))
        val lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LAT))
        val lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LON))

        return LatLon( uuid, index, lat, lon )
    }

    fun updateLatLon( latLon: LatLon )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(latLon.uuid)
        val values = ContentValues()

        putLatLon( latLon, values )

        dao.writableDatabase.update(DAO.TABLE_LAT_LON, values, whereClause, args )
    }

    fun getLatLon( uuid: String ): LatLon?
    {
        var latLon: LatLon? = null
        val query = "SELECT * FROM ${DAO.TABLE_LAT_LON} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            latLon = buildLatLon( cursor )
        }

        cursor.close()

        return latLon
    }

    fun getLatLonsWithEnumAreaUuid( enumAreaUuid: String ): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()

        val query = "SELECT latlon.*, conn.${DAO.COLUMN_LAT_LON_UUID}, conn.${DAO.COLUMN_ENUM_AREA_UUID} FROM ${DAO.TABLE_LAT_LON} AS latlon, " +
                "${DAO.CONNECTOR_TABLE_ENUM_AREA__LAT_LON} AS conn WHERE latlon.${DAO.COLUMN_UUID} = conn.${DAO.COLUMN_LAT_LON_UUID} AND conn.${DAO.COLUMN_ENUM_AREA_UUID} = '${enumAreaUuid}' " +
                "ORDER BY ${DAO.COLUMN_INDEX} ASC"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = buildLatLon(cursor)

            latLons.add( latlon )
        }

        cursor.close()

        return latLons
    }

    fun getLatLonsWithEnumerationTeamId( teamUuid: String ): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()

        val query = "SELECT latlon.*, conn.${DAO.COLUMN_LAT_LON_UUID}, conn.${DAO.COLUMN_ENUMERATION_TEAM_UUID} FROM ${DAO.TABLE_LAT_LON} AS latlon, " +
                "${DAO.CONNECTOR_TABLE_ENUMERATION_TEAM__LAT_LON} AS conn WHERE latlon.${DAO.COLUMN_UUID} = conn.${DAO.COLUMN_LAT_LON_UUID} AND conn.${DAO.COLUMN_ENUMERATION_TEAM_UUID} = '${teamUuid}'" +
                "ORDER BY ${DAO.COLUMN_INDEX} ASC"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = buildLatLon(cursor)

            latLons.add( latlon )
        }

        cursor.close()

        return latLons
    }

    fun getLatLonsWithCollectionTeamId( teamUuid: String ): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()

        val query = "SELECT latlon.*, conn.${DAO.COLUMN_LAT_LON_UUID}, conn.${DAO.COLUMN_COLLECTION_TEAM_UUID} FROM ${DAO.TABLE_LAT_LON} AS latlon, " +
                "${DAO.CONNECTOR_TABLE_COLLECTION_TEAM__LAT_LON} AS conn WHERE latlon.${DAO.COLUMN_UUID} = conn.${DAO.COLUMN_LAT_LON_UUID} AND conn.${DAO.COLUMN_COLLECTION_TEAM_UUID} = '${teamUuid}'" +
                "ORDER BY ${DAO.COLUMN_INDEX} ASC"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = buildLatLon(cursor)

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
            latLons.add( buildLatLon( cursor ))
        }

        cursor.close()

        return latLons
    }

    fun delete( latLon: LatLon )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(latLon.uuid)

        dao.writableDatabase.delete(DAO.TABLE_LAT_LON, whereClause, args)
    }
}
