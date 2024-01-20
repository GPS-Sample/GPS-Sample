package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Location

class EnumAreaDAO(private var dao: DAO)
{
    fun createOrUpdateEnumArea( enumArea: EnumArea, config : Config ) : EnumArea?
    {
        if (exists( enumArea ))
        {
            updateEnumArea( enumArea, config )
            updateEnumAreaElements(enumArea)
        }
        else
        {
            enumArea.id = null
            val values = ContentValues()
            putEnumArea( enumArea, config, values )
            enumArea.id = dao.writableDatabase.insert(DAO.TABLE_ENUM_AREA, null, values).toInt()

            updateEnumAreaElements(enumArea)
        }

        return enumArea
    }

    private fun updateEnumAreaElements(enumArea : EnumArea) : EnumArea?
    {
        enumArea.id?.let {id ->
            Log.d( "xxx", "new enumArea id = ${id}")

            for (latLon in enumArea.vertices)
            {
                DAO.latLonDAO.createOrUpdateLatLon(latLon, enumArea, null)
            }

            for (location in enumArea.locations)
            {
                // location id's may be updated here
                DAO.locationDAO.createOrUpdateLocation(location, enumArea)
            }

            for (enumerationTeam in enumArea.enumerationTeams)
            {
                // update the team location id's, if necc.
                for (teamLocation in enumerationTeam.locations)
                {
                    for (location in DAO.locationDAO.getLocations())
                    {
                        if (teamLocation.uuid == location.uuid && teamLocation.id != location.id)
                        {
                            teamLocation.id = location.id
                        }
                    }
                }

                DAO.enumerationTeamDAO.createOrUpdateTeam( enumerationTeam )
            }

            return enumArea
        } ?: return null
    }

    fun putEnumArea( enumArea: EnumArea, config : Config, values: ContentValues )
    {
        enumArea.id?.let { id ->
            Log.d( "xxx", "existing enumArea id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_UUID, enumArea.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, enumArea.creationDate )
        values.put( DAO.COLUMN_CONFIG_ID, config.id )
        values.put( DAO.COLUMN_ENUM_AREA_NAME, enumArea.name )
        values.put( DAO.COLUMN_ENUMERATION_TEAM_ID, enumArea.selectedEnumerationTeamId )
    }

    @SuppressLint("Range")
    private fun createEnumArea(cursor: Cursor): EnumArea
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_NAME))
        val selectedEnumerationTeamId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_TEAM_ID))

        return EnumArea( id, uuid, creationDate, name, selectedEnumerationTeamId )
    }

    fun exists( enumArea: EnumArea ): Boolean
    {
        getEnumArea( enumArea.uuid )?.let {
            return true
        } ?: return false
    }

    fun updateEnumArea( enumArea: EnumArea, config : Config )
    {
        enumArea.id?.let{ id ->
            Log.d( "xxx", "update enumArea id ${id}")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putEnumArea( enumArea, config, values )

            dao.writableDatabase.update(DAO.TABLE_ENUM_AREA, values, whereClause, args )
        }
    }

    fun getEnumArea( id: Int ): EnumArea?
    {
        var enumArea: EnumArea? = null
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumArea = createEnumArea( cursor )
            enumArea.id?.let { id ->
                enumArea.vertices = DAO.latLonDAO.getLatLonsWithEnumAreaId( id )
                enumArea.locations = DAO.locationDAO.getLocations( enumArea )
                enumArea.enumerationTeams = DAO.enumerationTeamDAO.getEnumerationTeams( enumArea )
            }
        }

        cursor.close()

        return enumArea
    }

    fun getEnumArea( uuid: String ): EnumArea?
    {
        var enumArea: EnumArea? = null
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumArea = createEnumArea( cursor )
            enumArea.id?.let { id ->
                enumArea.vertices = DAO.latLonDAO.getLatLonsWithEnumAreaId( id )
                enumArea.locations = DAO.locationDAO.getLocations( enumArea )
                enumArea.enumerationTeams = DAO.enumerationTeamDAO.getEnumerationTeams( enumArea )
            }
        }

        cursor.close()

        return enumArea
    }

    fun getEnumAreas( config: Config ): ArrayList<EnumArea>
    {
        val enumAreas = ArrayList<EnumArea>()

        config.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_CONFIG_ID} = $id"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val enumArea = createEnumArea( cursor )
                enumArea.id?.let { id ->
                    enumArea.vertices = DAO.latLonDAO.getLatLonsWithEnumAreaId( id )
                    enumArea.locations = DAO.locationDAO.getLocations( enumArea )
                    enumArea.enumerationTeams = DAO.enumerationTeamDAO.getEnumerationTeams( enumArea )
                    enumAreas.add( enumArea )
                }
            }

            cursor.close()
        }

        return enumAreas
    }

    fun getEnumAreas(): ArrayList<EnumArea>
    {
        val enumAreas = ArrayList<EnumArea>()

        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val enumArea = createEnumArea( cursor )
            enumArea.id?.let { id ->
                enumArea.vertices = DAO.latLonDAO.getLatLonsWithEnumAreaId( id )
                enumArea.locations = DAO.locationDAO.getLocations( enumArea )
                enumArea.enumerationTeams = DAO.enumerationTeamDAO.getEnumerationTeams( enumArea )
                enumAreas.add( enumArea )
            }
        }

        cursor.close()

        return enumAreas
    }

    fun delete( enumArea: EnumArea )
    {
        enumArea.id?.let {enumAreaId ->

            // latLon's are dependent on EnumAreas
//            DAO.latLonDAO.getAllLatLonsWithEnumAreaId(enumAreaId).map {
//                DAO.latLonDAO.delete( it )
//            }

            // locations's are dependent on EnumAreas
            DAO.locationDAO.getLocations(enumArea).map {
                DAO.locationDAO.delete( it )
            }

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(enumAreaId.toString())

            dao.writableDatabase.delete(DAO.TABLE_ENUM_AREA, whereClause, args)
        }
    }
}