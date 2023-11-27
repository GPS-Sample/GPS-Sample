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
    fun createOrUpdateLocation( location: Location, geoArea : GeoArea ) : Location?
    {
        if (exists( location ))
        {
            Log.d( "xxx", "update location id = ${location.id!!}")
            updateLocation( location, geoArea )
        }
        else
        {
            val values = ContentValues()
            putLocation( location, geoArea, values )
            location.id = dao.writableDatabase.insert(DAO.TABLE_LOCATION, null, values).toInt()
            location.id?.let {
                Log.d( "xxx", "new Location id = ${it}")
            }
        }

        location.id?.let { id ->
            if (geoArea is EnumArea)
            {
                updateConnectorTable( location, geoArea )
            }
            else if (geoArea is SampleArea)
            {
                updateConnectorTable( location, geoArea )
            }

            for (enumerationItem in location.enumerationItems)
            {
                DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location, true )
            }

            DAO.enumerationItemDAO.performBatchUpdate()

        } ?: return null

        return location
    }

    private fun updateConnectorTable( location : Location, enumArea : EnumArea )
    {
        location.id?.let { locationId ->
            enumArea.id?.let { enumAreaId ->
                val query = "SELECT * FROM ${DAO.TABLE_LOCATION__ENUM_AREA} WHERE ${DAO.COLUMN_LOCATION_ID} = $locationId AND ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId"
                val cursor = dao.writableDatabase.rawQuery(query, null)
                if (cursor.count == 0)
                {
                    val values = ContentValues()
                    values.put( DAO.COLUMN_LOCATION_ID, locationId )
                    values.put( DAO.COLUMN_ENUM_AREA_ID, enumAreaId )
                    dao.writableDatabase.insert(DAO.TABLE_LOCATION__ENUM_AREA, null, values)
                }
                cursor.close()
            }
        }
    }

    fun updateConnectorTable( location : Location, sampleArea : SampleArea )
    {
        location.id?.let { locationId ->
            sampleArea.id?.let { sampleAreaId ->
                val query = "SELECT * FROM ${DAO.TABLE_LOCATION__SAMPLE_AREA} WHERE ${DAO.COLUMN_LOCATION_ID} = $locationId AND ${DAO.COLUMN_SAMPLE_AREA_ID} = $sampleAreaId"
                val cursor = dao.writableDatabase.rawQuery(query, null)
                if (cursor.count == 0)
                {
                    val values = ContentValues()
                    values.put( DAO.COLUMN_LOCATION_ID, locationId )
                    values.put( DAO.COLUMN_SAMPLE_AREA_ID, sampleAreaId )
                    dao.writableDatabase.insert(DAO.TABLE_LOCATION__SAMPLE_AREA, null, values)
                }
                cursor.close()
            }
        }
    }

    fun importLocation( location: Location, geoArea : GeoArea )
    {
        val existingLocation = getLocation( location.uuid )

        if (existingLocation != null)
        {
            // make sure that the location id is based on the existing, id not the import id
            location.id = existingLocation.id
            updateLocation( location, geoArea )

            for (enumerationItem in location.enumerationItems)
            {
                DAO.enumerationItemDAO.importEnumerationItem( enumerationItem, location, geoArea, true )
            }

            DAO.enumerationItemDAO.performBatchUpdate()
        }
        else
        {
            location.id = null
            for (enumerationItem in location.enumerationItems)
            {
                enumerationItem.id = null
            }
            createOrUpdateLocation( location, geoArea )
        }
    }

    fun exists( location: Location ): Boolean
    {
        val locations = getLocations()

        for (existingLocation in locations)
        {
            if (location.uuid == existingLocation.uuid)
            {
                return true
            }
        }

        return false
    }

    fun updateLocation( location: Location, geoArea: GeoArea )
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(location.id!!.toString())
        val values = ContentValues()

        putLocation( location, geoArea, values )

        dao.writableDatabase.update(DAO.TABLE_LOCATION, values, whereClause, args )
    }

    fun putLocation( location: Location, geoArea : GeoArea, values: ContentValues)
    {
        location.id?.let { id ->
            Log.d( "xxx", "existing Location id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        location.isMultiFamily?.let {
            values.put( DAO.COLUMN_LOCATION_IS_MULTI_FAMILY, it.toInt())
        }

        values.put( DAO.COLUMN_CREATION_DATE, location.creationDate )
        values.put( DAO.COLUMN_UUID, location.uuid )
        values.put( DAO.COLUMN_LOCATION_TYPE_ID, LocationTypeConverter.toIndex(location.type) )
        values.put( DAO.COLUMN_LOCATION_LATITUDE, location.latitude )
        values.put( DAO.COLUMN_LOCATION_LONGITUDE, location.longitude )
        values.put( DAO.COLUMN_LOCATION_IS_LANDMARK, location.isLandmark.toInt())
        values.put( DAO.COLUMN_LOCATION_DESCRIPTION, location.description)
        values.put( DAO.COLUMN_LOCATION_IMAGE_DATA, location.imageData)
    }

    @SuppressLint("Range")
    private fun createLocation(cursor: Cursor): Location
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val locationTypeId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_TYPE_ID))
        val latitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LOCATION_LATITUDE))
        val longitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LOCATION_LONGITUDE))
        val isLandmark = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_IS_LANDMARK)).toBoolean()
        val description = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_LOCATION_DESCRIPTION))
        val imageData = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_LOCATION_IMAGE_DATA))
        val isMultiFamilyValue = cursor.getIntOrNull(cursor.getColumnIndex(DAO.COLUMN_LOCATION_IS_MULTI_FAMILY))

        var isMultiFamily: Boolean? = null

        isMultiFamilyValue?.let {
            isMultiFamily = it.toBoolean()
        }

        return Location( id, creationDate, uuid, LocationTypeConverter.fromIndex(locationTypeId), latitude, longitude, isLandmark, description, imageData, isMultiFamily, ArrayList<EnumerationItem>())
    }

    fun getLocation( uuid: String ) : Location?
    {
        var location : Location? = null

        val query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            location = createLocation( cursor )
        }

        cursor.close()

        return location
    }

    fun getLocation( id: Int ) : Location?
    {
        var location: Location? = null

        val query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            location = createLocation( cursor )
            location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
        }

        cursor.close()

        return location
    }

    fun getLocations( enumerationTeam: EnumerationTeam ) : ArrayList<Location>
    {
        val locations = ArrayList<Location>()

        enumerationTeam.id?.let { id ->
            val query = "SELECT LL.* FROM ${DAO.TABLE_LOCATION} AS LL, ${DAO.TABLE_LOCATION__ENUMERATION_TEAM} ELL WHERE" +
                    " ELL.${DAO.COLUMN_ENUMERATION_TEAM_ID} = $id AND LL.ID = ELL.${DAO.COLUMN_LOCATION_ID}"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val location = createLocation( cursor )
                location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
                locations.add( location )
            }

            cursor.close()
        }

        return locations
    }

    fun getLocations( collectionTeam: CollectionTeam ) : ArrayList<Location>
    {
        val locations = ArrayList<Location>()

        collectionTeam.id?.let { id ->
            val query = "SELECT LL.* FROM ${DAO.TABLE_LOCATION} AS LL, ${DAO.TABLE_LOCATION__COLLECTION_TEAM} ELL WHERE" +
                    " ELL.${DAO.COLUMN_COLLECTION_TEAM_ID} = $id AND LL.ID = ELL.${DAO.COLUMN_LOCATION_ID}"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val location = createLocation( cursor )
                location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
                locations.add( location )
            }

            cursor.close()
        }

        return locations
    }

    fun getLocations( enumArea: EnumArea ): ArrayList<Location>
    {
        val locations = ArrayList<Location>()

        enumArea.id?.let { enumAreaId ->
            val query = "SELECT LL.* FROM ${DAO.TABLE_LOCATION} AS LL, ${DAO.TABLE_LOCATION__ENUM_AREA} ELL WHERE" +
                    " ELL.${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND LL.ID = ELL.${DAO.COLUMN_LOCATION_ID}"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val location = createLocation( cursor )
                location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
                locations.add( location )
            }

            cursor.close()
        }

        return locations
    }

    fun getLocations( sampleArea: SampleArea ): ArrayList<Location>
    {
        val locations = ArrayList<Location>()

        sampleArea.id?.let { sampleAreaId ->
            val query = "SELECT LL.* FROM ${DAO.TABLE_LOCATION} AS LL, ${DAO.TABLE_LOCATION__SAMPLE_AREA} ELL WHERE" +
                    " ELL.${DAO.COLUMN_SAMPLE_AREA_ID} = $sampleAreaId AND LL.ID = ELL.${DAO.COLUMN_LOCATION_ID}"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val location = createLocation( cursor )
                location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
                locations.add( location )
            }

            cursor.close()
        }

        return locations
    }

    fun getLocations() : ArrayList<Location>
    {
        val locations = ArrayList<Location>()

        val query = "SELECT * FROM ${DAO.TABLE_LOCATION}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val location = createLocation( cursor )
           // location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
            locations.add( location )
        }

        cursor.close()

        return locations
    }

    fun delete( location: Location )
    {
        location.id?.let { id ->
            Log.d( "xxx", "deleting location with ID $id" )

//            for (enumerationItem in location.enumerationItems)
//            {
//                DAO.enumerationItemDAO.delete( enumerationItem )
//            }

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            dao.writableDatabase.delete(DAO.TABLE_LOCATION, whereClause, args)
        }
    }
}