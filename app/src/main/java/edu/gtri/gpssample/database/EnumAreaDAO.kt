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
            enumArea.id?.let {
                Log.d( "xxx", "updated EnumerationArea with ID ${it}" )
            }
        }
        else
        {
            enumArea.id = null
            val values = ContentValues()
            putEnumArea( enumArea, config, values )
            enumArea.id = dao.writableDatabase.insert(DAO.TABLE_ENUM_AREA, null, values).toInt()
            enumArea.id?.let {
                Log.d( "xxx", "created EnumerationArea with ID ${it}" )
            }
        }

        enumArea.id?.let { id ->
            for (latLon in enumArea.vertices) {
                DAO.latLonDAO.createOrUpdateLatLon(latLon, enumArea, null)
            }

            for (location in enumArea.locations) {
                // location id's may be updated here
                DAO.locationDAO.createOrUpdateLocation(location, enumArea)
            }

            val locations = DAO.locationDAO.getLocations()

            for (enumerationTeam in enumArea.enumerationTeams)
            {
                // update the team location id's, if necc.
                for (teamLocation in enumerationTeam.locations)
                {
                    val filteredLocations = locations.filter {
                        it.uuid == teamLocation.uuid
                    }

                    if (filteredLocations.isNotEmpty() && teamLocation.id != filteredLocations[0].id)
                    {
                        teamLocation.id = filteredLocations[0].id
                    }
                }

                enumerationTeam.enumerAreaId = id

                // the teamId may change, make sure that we update the enumArea.selectedEnumerationTeam, if necessary

                val oldTeamId = enumerationTeam.id

                DAO.enumerationTeamDAO.createOrUpdateTeam(enumerationTeam)?.let { newTeam ->
                    newTeam.id?.let { newTeamId ->
                        oldTeamId?.let { oldTeamId ->
                            if (enumArea.selectedEnumerationTeamId == oldTeamId) {
                                enumArea.selectedEnumerationTeamId = newTeamId
                                updateEnumArea(enumArea, config)
                            }
                        }
                    }
                }
            }
        }

        return enumArea
    }

    fun putEnumArea( enumArea: EnumArea, config : Config, values: ContentValues )
    {
        enumArea.id?.let { id ->
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
            enumArea.id = it.id
            return true
        } ?: return false
    }

    fun updateEnumArea( enumArea: EnumArea, config : Config )
    {
        enumArea.id?.let{ id ->
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

            for (vertice in enumArea.vertices)
            {
                DAO.latLonDAO.delete( vertice )
            }

            for (enumerationTeam in enumArea.enumerationTeams)
            {
                DAO.enumerationTeamDAO.deleteTeam( enumerationTeam )
            }

            for (location in enumArea.locations)
            {
                DAO.locationDAO.delete( location )
            }

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(enumAreaId.toString())

            dao.writableDatabase.delete(DAO.TABLE_ENUM_AREA, whereClause, args)
        }
    }
}