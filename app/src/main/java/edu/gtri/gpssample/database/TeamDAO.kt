package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.extensions.toInt

class TeamDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateTeam( team: Team ) : Team?
    {
        if (exists( team ))
        {
            updateTeam( team )
        }
        else
        {
            val values = ContentValues()
            putTeam( team, values )
            team.id = dao.writableDatabase.insert(DAO.TABLE_TEAM, null, values).toInt()
            team.id?.let { id ->
                Log.d( "xxx", "new Team id = ${id}")
            } ?: return null
        }

        team.id?.let {
            for (latLon in team.polygon)
            {
                Log.d("xxxx", "")
                DAO.latLonDAO.createOrUpdateLatLon(latLon,null, team)
            }
        }

        return team
    }

    //--------------------------------------------------------------------------
    fun putTeam(team: Team, values: ContentValues )
    {
        team.id?.let { id ->
            Log.d( "xxx", "existing team id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, team.creationDate )
        values.put( DAO.COLUMN_STUDY_ID, team.studyId )
        values.put( DAO.COLUMN_ENUM_AREA_ID, team.enumAreaId )
        values.put( DAO.COLUMN_TEAM_NAME, team.name )
        values.put( DAO.COLUMN_TEAM_IS_ENUMERATION_TEAM, team.isEnumerationTeam.toInt())
    }

    //--------------------------------------------------------------------------
    fun exists( team: Team ): Boolean
    {
        team.id?.let { id ->
            getTeam( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createTeam(cursor: Cursor): Team
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val study_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val enum_area_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_TEAM_NAME))
        val isEnumerationTeam = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_TEAM_IS_ENUMERATION_TEAM)).toBoolean()

        return Team(id, creationDate, study_id, enum_area_id, name, isEnumerationTeam, DAO.latLonDAO.getLatLonsWithTeamId( id ))
    }

    //--------------------------------------------------------------------------
    fun updateTeam( team: Team )
    {
        val db = dao.writableDatabase

        team.id?.let { id ->
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putTeam( team, values )

            db.update(DAO.TABLE_TEAM, values, whereClause, args )
        }

        db.close()
    }

    //--------------------------------------------------------------------------
    fun getTeam( id: Int ): Team?
    {
        var team: Team? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            team = createTeam( cursor )
        }

        cursor.close()
        db.close()

        return team
    }

    //--------------------------------------------------------------------------
    fun getEnumerationTeams( enumAreaId: Int ): ArrayList<Team>
    {
        val teams = ArrayList<Team>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND ${DAO.COLUMN_TEAM_IS_ENUMERATION_TEAM} = 1"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            teams.add( createTeam( cursor ))
        }

        cursor.close()
        db.close()

        return teams
    }

    //--------------------------------------------------------------------------
    fun getCollectionTeams( enumArea_id: Int ): ArrayList<Team>
    {
        val teams = ArrayList<Team>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumArea_id AND ${DAO.COLUMN_TEAM_IS_ENUMERATION_TEAM} = 0"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            teams.add( createTeam( cursor ))
        }

        cursor.close()
        db.close()

        return teams
    }

    fun getTeams(): ArrayList<Team>
    {
        val teams = ArrayList<Team>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            teams.add( createTeam( cursor ))
        }

        cursor.close()
        db.close()

        return teams
    }

    //--------------------------------------------------------------------------
    fun deleteTeam( team: Team )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(team.id.toString())

//        team.polygon.map{
//            DAO.latLonDAO.delete(it)
//        }

        db.delete(DAO.TABLE_TEAM, whereClause, args)
        db.close()
    }
}