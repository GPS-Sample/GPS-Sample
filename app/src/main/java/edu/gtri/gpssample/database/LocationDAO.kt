package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.database.getIntOrNull
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.LocationTypeConverter
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.extensions.toInt
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.*
import kotlin.collections.ArrayList

class LocationDAO(private var dao: DAO)
{
    fun createOrUpdateLocation( location: Location, enumArea : EnumArea ) : Location?
    {
        val existingLocation = getLocation( location.uuid )

        if (existingLocation != null)
        {
            if (location.doesNotEqual( existingLocation ))
            {
                updateLocation( location, enumArea )
                Log.d( "xxx", "Updated Location with ID ${location.uuid}" )
            }
        }
        else
        {
            val values = ContentValues()
            putLocation( location, enumArea, values )
            if (dao.writableDatabase.insert(DAO.TABLE_LOCATION, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "Created Location with ID ${location.uuid}" )
        }

        updateConnectorTable( location, enumArea )

        for (enumerationItem in location.enumerationItems)
        {
            enumerationItem.locationUuid = location.uuid
            DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
        }

        return location
    }

    private fun updateConnectorTable( location : Location, enumArea : EnumArea )
    {
        val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_LOCATION__ENUM_AREA} WHERE ${DAO.COLUMN_LOCATION_UUID} = '${location.uuid}' AND ${DAO.COLUMN_ENUM_AREA_UUID} = '${enumArea.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)
        if (cursor.count == 0)
        {
            val values = ContentValues()
            values.put( DAO.COLUMN_LOCATION_UUID, location.uuid )
            values.put( DAO.COLUMN_ENUM_AREA_UUID, enumArea.uuid )
            dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_LOCATION__ENUM_AREA, null, values)
        }
        cursor.close()
    }

    fun exists( location: Location ): Boolean
    {
        getLocation( location.uuid )?.let {
            return true
        } ?: return false
    }

    fun updateLocation( location: Location, enumArea: EnumArea )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(location.uuid)
        val values = ContentValues()

        putLocation( location, enumArea, values )

        dao.writableDatabase.update(DAO.TABLE_LOCATION, values, whereClause, args )
    }

    fun putLocation( location: Location, enumArea : EnumArea, values: ContentValues)
    {
        location.isMultiFamily?.let {
            values.put( DAO.COLUMN_LOCATION_IS_MULTI_FAMILY, it.toInt())
        }

        values.put( DAO.COLUMN_UUID, location.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, location.creationDate )
        values.put( DAO.COLUMN_LOCATION_TIME_ZONE, location.timeZone )
        values.put( DAO.COLUMN_LOCATION_TYPE_ID, LocationTypeConverter.toIndex(location.type) )
        values.put( DAO.COLUMN_LOCATION_GPS_ACCURACY, location.gpsAccuracy )
        values.put( DAO.COLUMN_LOCATION_LATITUDE, location.latitude )
        values.put( DAO.COLUMN_LOCATION_LONGITUDE, location.longitude )
        values.put( DAO.COLUMN_LOCATION_ALTITUDE, location.altitude )
        values.put( DAO.COLUMN_LOCATION_IS_LANDMARK, location.isLandmark.toInt())
        values.put( DAO.COLUMN_LOCATION_DESCRIPTION, location.description)
        values.put( DAO.COLUMN_LOCATION_IMAGE_DATA, location.imageData)
    }

    @SuppressLint("Range")
    private fun buildLocation(cursor: Cursor): Location
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val timeZone = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_TIME_ZONE))
        val locationTypeId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_TYPE_ID))
        val gpsAccuracy = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_GPS_ACCURACY))
        val latitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LOCATION_LATITUDE))
        val longitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LOCATION_LONGITUDE))
        val altitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LOCATION_ALTITUDE))
        val isLandmark = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_IS_LANDMARK)).toBoolean()
        val description = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_LOCATION_DESCRIPTION))
        val imageData = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_LOCATION_IMAGE_DATA))
        val isMultiFamily = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_IS_MULTI_FAMILY)).toBoolean()

        return Location( uuid, creationDate, timeZone, 0.0, "", LocationTypeConverter.fromIndex(locationTypeId), gpsAccuracy, latitude, longitude, altitude, isLandmark, description, imageData, isMultiFamily, ArrayList<EnumerationItem>())
    }

    fun getLocation( uuid: String ) : Location?
    {
        var location : Location? = null

        val query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            location = buildLocation( cursor )
        }

        cursor.close()

        return location
    }

    @SuppressLint("Range")
    fun getEnumerationTeamLocationUuids( enumerationTeam: EnumerationTeam ) : ArrayList<String>
    {
        val uuids = ArrayList<String>()

        val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_LOCATION__ENUMERATION_TEAM} WHERE ${DAO.COLUMN_ENUMERATION_TEAM_UUID} = '${enumerationTeam.uuid}'"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            uuids.add( cursor.getString(cursor.getColumnIndex(DAO.COLUMN_LOCATION_UUID)))
        }

        cursor.close()

        return uuids
    }

    fun getLocations( enumerationTeam: EnumerationTeam ) : ArrayList<Location>
    {
        val locations = ArrayList<Location>()

        val query = "SELECT location.*, conn.${DAO.COLUMN_LOCATION_UUID}, conn.${DAO.COLUMN_ENUMERATION_TEAM_UUID} FROM ${DAO.TABLE_LOCATION} AS location, " +
                "${DAO.CONNECTOR_TABLE_LOCATION__ENUMERATION_TEAM} AS conn WHERE location.${DAO.COLUMN_UUID} = conn.${DAO.COLUMN_LOCATION_UUID} AND conn.${DAO.COLUMN_ENUMERATION_TEAM_UUID} = '${enumerationTeam.uuid}'"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val location = buildLocation( cursor )
            location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
            locations.add( location )
        }

        cursor.close()

        return locations
    }

    fun getLocations( collectionTeam: CollectionTeam ) : ArrayList<Location>
    {
        val locations = ArrayList<Location>()

        val query = "SELECT location.*, conn.${DAO.COLUMN_LOCATION_UUID}, conn.${DAO.COLUMN_COLLECTION_TEAM_UUID} FROM ${DAO.TABLE_LOCATION} AS location, " +
                "${DAO.CONNECTOR_TABLE_LOCATION__COLLECTION_TEAM} AS conn WHERE location.${DAO.COLUMN_UUID} = conn.${DAO.COLUMN_LOCATION_UUID} AND conn.${DAO.COLUMN_COLLECTION_TEAM_UUID} = '${collectionTeam.uuid}'"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val location = buildLocation( cursor )
            location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
            locations.add( location )
        }

        cursor.close()

        return locations
    }

    fun getLocations( enumArea: EnumArea ): ArrayList<Location>
    {
        val locations = ArrayList<Location>()

        val query = "SELECT location.*, conn.${DAO.COLUMN_LOCATION_UUID}, conn.${DAO.COLUMN_ENUM_AREA_UUID} FROM ${DAO.TABLE_LOCATION} AS location, " +
                "${DAO.CONNECTOR_TABLE_LOCATION__ENUM_AREA} AS conn WHERE location.${DAO.COLUMN_UUID} = conn.${DAO.COLUMN_LOCATION_UUID} AND conn.${DAO.COLUMN_ENUM_AREA_UUID} = '${enumArea.uuid}'"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val location = buildLocation( cursor )
            location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
            locations.add( location )
        }

        cursor.close()

        return locations
    }

    fun getLocations() : ArrayList<Location>
    {
        val locations = ArrayList<Location>()

        val query = "SELECT * FROM ${DAO.TABLE_LOCATION}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val location = buildLocation( cursor )
            location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
            locations.add( location )
        }

        cursor.close()

        return locations
    }

    fun delete( location: Location )
    {
        var whereClause = "${DAO.COLUMN_UUID} = ?"
        var args = arrayOf(location.uuid)
        dao.writableDatabase.delete(DAO.TABLE_LOCATION, whereClause, args)

        // delete this location from all connector tables

        whereClause = "${DAO.COLUMN_LOCATION_UUID} = ?"
        args = arrayOf(location.uuid)

        dao.writableDatabase.delete(DAO.CONNECTOR_TABLE_LOCATION__ENUM_AREA, whereClause, args)
        dao.writableDatabase.delete(DAO.CONNECTOR_TABLE_LOCATION__COLLECTION_TEAM, whereClause, args)
        dao.writableDatabase.delete(DAO.CONNECTOR_TABLE_LOCATION__ENUMERATION_TEAM, whereClause, args)
    }
}