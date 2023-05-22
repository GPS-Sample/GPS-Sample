package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*

class TeamDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateTeam( team: Team) : Team?
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
        values.put( DAO.COLUMN_ENUM_AREA_ID, team.enumAreaId )
        values.put( DAO.COLUMN_TEAM_NAME, team.name )
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
        val enum_area_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_TEAM_NAME))

        return Team(id, creationDate, enum_area_id, name )
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
    fun getTeams( enumArea_id: Int ): ArrayList<Team>
    {
        val teams = ArrayList<Team>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumArea_id"
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

        db.delete(DAO.TABLE_TEAM, whereClause, args)
        db.close()
    }
}