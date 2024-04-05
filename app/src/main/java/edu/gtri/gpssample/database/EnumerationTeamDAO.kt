package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*

class EnumerationTeamDAO(private var dao: DAO)
{
    fun createOrUpdateTeam(enumerationTeam: EnumerationTeam) : EnumerationTeam?
    {
        if (exists( enumerationTeam ))
        {
            updateTeam( enumerationTeam )
        }
        else
        {
            enumerationTeam.id = null
            val values = ContentValues()
            putTeam( enumerationTeam, values )
            enumerationTeam.id = dao.writableDatabase.insert(DAO.TABLE_ENUMERATION_TEAM, null, values).toInt()
            enumerationTeam.id?.let { id ->
                Log.d( "xxx", "new Team id = ${id}")
            } ?: return null
        }

        enumerationTeam.id?.let {
            for (latLon in enumerationTeam.polygon)
            {
                DAO.latLonDAO.createOrUpdateLatLon(latLon,null, null)
            }
        }

        updateConnectorTable( enumerationTeam )

        return enumerationTeam
    }

    fun updateConnectorTable( enumerationTeam: EnumerationTeam )
    {
        enumerationTeam.id?.let { enumerationTeamId ->
            for (latLon in enumerationTeam.polygon)
            {
                latLon.id?.let { latLonId ->
                    val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM__LAT_LON} WHERE ${DAO.COLUMN_LAT_LON_ID} = $latLonId AND ${DAO.COLUMN_ENUMERATION_TEAM_ID} = $enumerationTeamId"
                    val cursor = dao.writableDatabase.rawQuery(query, null)
                    if (cursor.count == 0)
                    {
                        val values = ContentValues()
                        values.put( DAO.COLUMN_LAT_LON_ID, latLonId )
                        values.put( DAO.COLUMN_ENUMERATION_TEAM_ID, enumerationTeamId )
                        dao.writableDatabase.insert(DAO.TABLE_ENUMERATION_TEAM__LAT_LON, null, values)
                    }
                    cursor.close()
                }
            }

            for (location in enumerationTeam.locations)
            {
                location.id?.let { locationId ->
                    val query = "SELECT * FROM ${DAO.TABLE_LOCATION__ENUMERATION_TEAM} WHERE ${DAO.COLUMN_LOCATION_ID} = $locationId AND ${DAO.COLUMN_ENUMERATION_TEAM_ID} = $enumerationTeamId"
                    val cursor = dao.writableDatabase.rawQuery(query, null)
                    if (cursor.count == 0)
                    {
                        val values = ContentValues()
                        values.put( DAO.COLUMN_LOCATION_ID, locationId )
                        values.put( DAO.COLUMN_ENUMERATION_TEAM_ID, enumerationTeamId )
                        dao.writableDatabase.insert(DAO.TABLE_LOCATION__ENUMERATION_TEAM, null, values)
                    }
                    cursor.close()
                }
            }
        }
    }

    fun putTeam(enumerationTeam: EnumerationTeam, values: ContentValues )
    {
        enumerationTeam.id?.let { id ->
            Log.d( "xxx", "existing team id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_UUID, enumerationTeam.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, enumerationTeam.creationDate )
        values.put( DAO.COLUMN_ENUM_AREA_ID, enumerationTeam.enumerAreaId )
        values.put( DAO.COLUMN_ENUMERATION_TEAM_NAME, enumerationTeam.name )
    }

    fun exists( enumerationTeam: EnumerationTeam ): Boolean
    {
        getTeam( enumerationTeam.uuid )?.let {
            enumerationTeam.id = it.id
            return true
        } ?: return false
    }

    @SuppressLint("Range")
    private fun createTeam(cursor: Cursor): EnumerationTeam
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val enumAreaId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_TEAM_NAME))

        val enumerationTeam = EnumerationTeam(id, uuid, creationDate, enumAreaId, name, ArrayList<LatLon>(), ArrayList<Location>())

        enumerationTeam.polygon = DAO.latLonDAO.getLatLonsWithEnumerationTeamId( id )
        enumerationTeam.locations = DAO.locationDAO.getLocations( enumerationTeam )

        return enumerationTeam
    }

    fun updateTeam( enumerationTeam: EnumerationTeam )
    {
        enumerationTeam.id?.let { id ->
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putTeam( enumerationTeam, values )

            dao.writableDatabase.update(DAO.TABLE_ENUMERATION_TEAM, values, whereClause, args )
        }
    }

    fun getTeam( id: Int ): EnumerationTeam?
    {
        var enumerationTeam: EnumerationTeam? = null
        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumerationTeam = createTeam( cursor )
        }

        cursor.close()

        return enumerationTeam
    }

    fun getTeam( uuid: String ): EnumerationTeam?
    {
        var enumerationTeam: EnumerationTeam? = null
        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumerationTeam = createTeam( cursor )
        }

        cursor.close()

        return enumerationTeam
    }

    fun getEnumerationTeams( enumArea: EnumArea ): ArrayList<EnumerationTeam>
    {
        val enumerationTeams = ArrayList<EnumerationTeam>()

        enumArea.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $id"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                enumerationTeams.add( createTeam( cursor ))
            }

            cursor.close()
        }

        return enumerationTeams
    }

    fun getEnumerationTeams(): ArrayList<EnumerationTeam>
    {
        val enumerationTeams = ArrayList<EnumerationTeam>()
        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumerationTeams.add( createTeam( cursor ))
        }

        cursor.close()

        return enumerationTeams
    }

    fun deleteTeam(enumerationTeam: EnumerationTeam )
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(enumerationTeam.id.toString())

        dao.writableDatabase.delete(DAO.TABLE_ENUMERATION_TEAM, whereClause, args)
    }
}