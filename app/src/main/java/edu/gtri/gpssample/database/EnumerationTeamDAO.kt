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
            val values = ContentValues()
            putTeam( enumerationTeam, values )
            enumerationTeam.id = dao.writableDatabase.insert(DAO.TABLE_ENUMERATION_TEAM, null, values).toInt()
            enumerationTeam.id?.let { id ->
                Log.d( "xxx", "new Team id = ${id}")
            } ?: return null
        }

        updateConnectorTable( enumerationTeam )

        return enumerationTeam
    }

    fun updateConnectorTable( enumerationTeam: EnumerationTeam )
    {
        enumerationTeam.id?.let { enumerationTeamId ->
            for (location in enumerationTeam.locations)
            {
                location.id?.let { locationId ->
                    val values = ContentValues()
                    values.put( DAO.COLUMN_LOCATION_ID, locationId )
                    values.put( DAO.COLUMN_ENUMERATION_TEAM_ID, enumerationTeamId )
                    dao.writableDatabase.insert(DAO.TABLE_LOCATION__ENUMERATION_TEAM, null, values)
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

        values.put( DAO.COLUMN_CREATION_DATE, enumerationTeam.creationDate )
        values.put( DAO.COLUMN_ENUM_AREA_ID, enumerationTeam.enumerAreaId )
        values.put( DAO.COLUMN_ENUMERATION_TEAM_NAME, enumerationTeam.name )
    }

    fun exists(enumerationTeam: EnumerationTeam ): Boolean
    {
        enumerationTeam.id?.let { id ->
            getTeam( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    @SuppressLint("Range")
    private fun createTeam(cursor: Cursor): EnumerationTeam
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val enumAreaId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_TEAM_NAME))

        val enumerationTeam = EnumerationTeam(id, creationDate, enumAreaId, name, ArrayList<Location>())

        enumerationTeam.locations = DAO.locationDAO.getLocations( enumerationTeam )

        return enumerationTeam
    }

    fun updateTeam( enumerationTeam: EnumerationTeam )
    {
        val db = dao.writableDatabase

        enumerationTeam.id?.let { id ->
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putTeam( enumerationTeam, values )

            db.update(DAO.TABLE_ENUMERATION_TEAM, values, whereClause, args )
        }

        db.close()
    }

    fun getTeam( id: Int ): EnumerationTeam?
    {
        var enumerationTeam: EnumerationTeam? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            enumerationTeam = createTeam( cursor )
        }

        cursor.close()
        db.close()

        return enumerationTeam
    }

    fun getEnumerationTeams( enumArea: EnumArea ): ArrayList<EnumerationTeam>
    {
        val enumerationTeams = ArrayList<EnumerationTeam>()
        val db = dao.writableDatabase

        enumArea.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $id"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                enumerationTeams.add( createTeam( cursor ))
            }

            cursor.close()
        }

        db.close()

        return enumerationTeams
    }

    fun getTeams(): ArrayList<EnumerationTeam>
    {
        val enumerationTeams = ArrayList<EnumerationTeam>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumerationTeams.add( createTeam( cursor ))
        }

        cursor.close()
        db.close()

        return enumerationTeams
    }

    fun deleteTeam(enumerationTeam: EnumerationTeam )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(enumerationTeam.id.toString())

        db.delete(DAO.TABLE_ENUMERATION_TEAM, whereClause, args)
        db.close()
    }
}