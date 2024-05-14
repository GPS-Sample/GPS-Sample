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
    fun createOrUpdateEnumArea( enumArea: EnumArea ) : EnumArea?
    {
        if (exists( enumArea ))
        {
            updateEnumArea( enumArea )
            Log.d( "xxx", "updated EnumerationArea with ID ${enumArea.uuid}" )
        }
        else
        {
            val values = ContentValues()
            putEnumArea( enumArea, values )
            if (dao.writableDatabase.insert(DAO.TABLE_ENUM_AREA, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "created EnumerationArea with ID ${enumArea.uuid}" )
        }

        for (latLon in enumArea.vertices) {
            DAO.latLonDAO.createOrUpdateLatLon(latLon, enumArea, null)
        }

        for (location in enumArea.locations) {
            DAO.locationDAO.createOrUpdateLocation(location, enumArea)
        }

        for (enumerationTeam in enumArea.enumerationTeams) {
            DAO.enumerationTeamDAO.createOrUpdateEnumerationTeam( enumerationTeam )
        }

        for (collectionTeam in enumArea.collectionTeams) {
            DAO.collectionTeamDAO.createOrUpdateCollectionTeam( collectionTeam )
        }

        return enumArea
    }

    fun putEnumArea( enumArea: EnumArea, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, enumArea.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, enumArea.creationDate )
        values.put( DAO.COLUMN_CONFIG_UUID, enumArea.configUuid )
        values.put( DAO.COLUMN_ENUM_AREA_NAME, enumArea.name )
        values.put( DAO.COLUMN_ENUMERATION_TEAM_UUID, enumArea.selectedEnumerationTeamUuid )
        values.put( DAO.COLUMN_COLLECTION_TEAM_UUID, enumArea.selectedCollectionTeamUuid )
    }

    @SuppressLint("Range")
    private fun buildEnumArea(cursor: Cursor): EnumArea
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val configUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_NAME))
        val selectedEnumerationTeamUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_TEAM_UUID))
        val selectedCollectionTeamUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_COLLECTION_TEAM_UUID))

        return EnumArea( uuid, creationDate, configUuid, name, selectedEnumerationTeamUuid, selectedCollectionTeamUuid )
    }

    fun exists( enumArea: EnumArea ): Boolean
    {
        getEnumArea( enumArea.uuid )?.let {
            return true
        } ?: return false
    }

    fun updateEnumArea( enumArea: EnumArea )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(enumArea.uuid)
        val values = ContentValues()

        putEnumArea( enumArea, values )

        dao.writableDatabase.update(DAO.TABLE_ENUM_AREA, values, whereClause, args )
    }

    fun getEnumArea( uuid: String ): EnumArea?
    {
        var enumArea: EnumArea? = null
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumArea = buildEnumArea( cursor )
            enumArea.vertices = DAO.latLonDAO.getLatLonsWithEnumAreaUuid( enumArea.uuid )
            enumArea.locations = DAO.locationDAO.getLocations( enumArea )
            enumArea.enumerationTeams = DAO.enumerationTeamDAO.getEnumerationTeams( enumArea )
            enumArea.collectionTeams = DAO.collectionTeamDAO.getCollectionTeams( enumArea )
        }

        cursor.close()

        return enumArea
    }

    fun getEnumAreas( config: Config ): ArrayList<EnumArea>
    {
        val enumAreas = ArrayList<EnumArea>()

        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_CONFIG_UUID} = '${config.uuid}'"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val enumArea = buildEnumArea( cursor )
            enumArea.vertices = DAO.latLonDAO.getLatLonsWithEnumAreaUuid( enumArea.uuid )
            enumArea.locations = DAO.locationDAO.getLocations( enumArea )
            enumArea.enumerationTeams = DAO.enumerationTeamDAO.getEnumerationTeams( enumArea )
            enumArea.collectionTeams = DAO.collectionTeamDAO.getCollectionTeams( enumArea )
            enumAreas.add( enumArea )
        }

        cursor.close()

        return enumAreas
    }

    fun getEnumAreas(): ArrayList<EnumArea>
    {
        val enumAreas = ArrayList<EnumArea>()

        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val enumArea = buildEnumArea( cursor )
            enumArea.vertices = DAO.latLonDAO.getLatLonsWithEnumAreaUuid( enumArea.uuid )
            enumArea.locations = DAO.locationDAO.getLocations( enumArea )
            enumArea.enumerationTeams = DAO.enumerationTeamDAO.getEnumerationTeams( enumArea )
            enumArea.collectionTeams = DAO.collectionTeamDAO.getCollectionTeams( enumArea )
            enumAreas.add( enumArea )
        }

        cursor.close()

        return enumAreas
    }

    fun delete( enumArea: EnumArea )
    {
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

        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(enumArea.uuid)

        dao.writableDatabase.delete(DAO.TABLE_ENUM_AREA, whereClause, args)
    }
}