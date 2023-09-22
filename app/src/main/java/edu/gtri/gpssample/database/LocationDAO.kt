package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.LocationTypeConverter
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.extensions.toInt

class LocationDAO(private var dao: DAO)
{
    fun createOrUpdateLocation( location: Location, geoArea : GeoArea) : Location?
    {
        if (exists( location ))
        {
            updateLocation( location, geoArea )

            for (enumerationItem in location.enumerationItems)
            {
                DAO.enumerationItemDAO.createOrUpdateEnumerationItem(enumerationItem, location)
            }

            for (sampledItem in location.sampledItems)
            {
                DAO.sampledItemDAO.createOrUpdateSampledItem(sampledItem)
            }
        }
        else
        {
            val values = ContentValues()
            putLocation( location, geoArea, values )

            location.id = dao.writableDatabase.insert(DAO.TABLE_LOCATION, null, values).toInt()
            location.id?.let { id ->

                for (enumerationItem in location.enumerationItems)
                {
                    DAO.enumerationItemDAO.createOrUpdateEnumerationItem(enumerationItem, location)
                    for (fieldData in enumerationItem.fieldDataList)
                    {
                        DAO.fieldDataDAO.createOrUpdateFieldData(fieldData, enumerationItem)
                    }
                }

                for (sampledItem in location.sampledItems)
                {
                    sampledItem.location = location
                    DAO.sampledItemDAO.createOrUpdateSampledItem(sampledItem)
                }
            } ?: return null
        }

        return location
    }

    fun importLocation( location: Location, enumArea : EnumArea ) : Location?
    {
        val existingLocation = getLocation( location.uuid )

        existingLocation?.let {
            delete( it )
        }

        val values = ContentValues()

        location.id = null
        putLocation( location, enumArea,  values )

        location.id = dao.writableDatabase.insert(DAO.TABLE_LOCATION, null, values).toInt()
        location.id?.let { id ->
            Log.d( "xxx", "new location id = ${id}")

            for (enumerationItem in location.enumerationItems)
            {
                enumerationItem.fieldDataList?.let { fieldDataList ->
                    for (fieldData in fieldDataList)
                    {
//                        fieldData.id = null

                        DAO.fieldDataDAO.createOrUpdateFieldData( fieldData, enumerationItem )
                    }
                }
                DAO.enumerationItemDAO.createOrUpdateEnumerationItem( enumerationItem, location )
            }
        } ?: return null

        return location
    }

    fun exists( location: Location ): Boolean
    {
        location.id?.let { id ->
            getLocation( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    fun updateLocation( location: Location, geoArea: GeoArea )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(location.id!!.toString())
        val values = ContentValues()

        putLocation( location, geoArea, values )

        db.update(DAO.TABLE_LOCATION, values, whereClause, args )
        db.close()
    }

    fun putLocation( location: Location, geoArea : GeoArea, values: ContentValues)
    {
        location.id?.let { id ->
            Log.d( "xxx", "existing Location id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        if (geoArea is EnumArea)
        {
            values.put( DAO.COLUMN_ENUM_AREA_ID, geoArea.id )
        }
        else if (geoArea is SampleArea)
        {
            values.put( DAO.COLUMN_SAMPLE_AREA_ID, geoArea.id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, location.creationDate )
        values.put( DAO.COLUMN_UUID, location.uuid )
        values.put( DAO.COLUMN_LOCATION_TYPE_ID, LocationTypeConverter.toIndex(location.type) )
        values.put( DAO.COLUMN_LOCATION_LATITUDE, location.latitude )
        values.put( DAO.COLUMN_LOCATION_LONGITUDE, location.longitude )
        values.put( DAO.COLUMN_LOCATION_IS_LANDMARK, location.isLandmark.toInt())
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

        return Location( id, creationDate, uuid,LocationTypeConverter.fromIndex(locationTypeId), latitude, longitude, isLandmark, ArrayList<EnumerationItem>(), ArrayList<SampledItem>() )
    }

    fun getLocation( uuid: String ) : Location?
    {
        var location : Location? = null
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            location = createLocation( cursor )
        }

        cursor.close()
        db.close()

        return location
    }

    fun getLocation( id: Int ) : Location?
    {
        var location: Location? = null

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            location = createLocation( cursor )
            location.sampledItems = DAO.sampledItemDAO.getSampledItems( location )
            location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
        }

        cursor.close()
        db.close()

        return location
    }

    fun getLocations( enumArea: EnumArea ) : ArrayList<Location>
    {
        val locations = ArrayList<Location>()
        val db = dao.writableDatabase

        enumArea.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $id"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val location = createLocation( cursor )
                location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
                location.sampledItems = DAO.sampledItemDAO.getSampledItems( location )

                locations.add( location )
            }
            cursor.close()
        }
        db.close()

        return locations
    }

    fun getLocations( enumArea: EnumArea, team: Team ) : ArrayList<Location>
    {
        val locations = ArrayList<Location>()
        val db = dao.writableDatabase

        enumArea.id?.let { enumAreaId ->
            team.id?.let { teamId ->
                var query = ""

                if (team.isEnumerationTeam)
                {
                    query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND ${DAO.COLUMN_ENUMERATION_TEAM_ID} = $teamId"
                }
                else
                {
                    query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND ${DAO.COLUMN_COLLECTION_TEAM_ID} = $teamId"
                }

                val cursor = db.rawQuery(query, null)

                while (cursor.moveToNext())
                {
                    val location = createLocation( cursor )
                   // location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
                    locations.add( location )
                }

                cursor.close()
            }
        }

        db.close()

        return locations
    }

    fun getEnumeratedLocations( enumArea: EnumArea, team: Team ) : ArrayList<Location>
    {
        val locations = ArrayList<Location>()
        val db = dao.writableDatabase

        enumArea.id?.let { enumAreaId ->
            team.id?.let { teamId ->
                var query = ""

                if (team.isEnumerationTeam)
                {
                    query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND ${DAO.COLUMN_ENUMERATION_TEAM_ID} = $teamId"
                }
                else
                {
                    query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND ${DAO.COLUMN_COLLECTION_TEAM_ID} = $teamId"
                }

                val cursor = db.rawQuery(query, null)

                while (cursor.moveToNext())
                {
                    val location = createLocation( cursor )
//                    location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
//                    for (enumerationItem in location.enumerationItems)
//                    {
//                        if (enumerationItem.enumerationState == EnumerationState.Enumerated)
//                        {
//                            locations.add( location )
//                            break
//                        }
//                    }
                }

                cursor.close()
            }
        }

        db.close()

        return locations
    }

    fun getLocations() : ArrayList<Location>
    {
        val locations = ArrayList<Location>()
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_LOCATION}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val location = createLocation( cursor )
           // location.enumerationItems = DAO.enumerationItemDAO.getEnumerationItems( location )
            locations.add( location )
        }

        cursor.close()

        db.close()

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

            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            db.delete(DAO.TABLE_LOCATION, whereClause, args)
            db.close()
        }
    }
}