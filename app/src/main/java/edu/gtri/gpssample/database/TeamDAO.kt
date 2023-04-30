package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.*

class TeamDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createTeam( team: Team) : Int
    {
        val values = ContentValues()

        putTeam( team, values )

        return dao.writableDatabase.insert(DAO.TABLE_TEAM, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putTeam(team: Team, values: ContentValues )
    {
        values.put( DAO.COLUMN_ENUM_AREA_ID, team.enumArea_id )
        values.put( DAO.COLUMN_TEAM_NAME, team.name )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createTeam(cursor: Cursor): Team
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val enum_area_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_TEAM_NAME))

        return Team(id, enum_area_id, name )
    }

    //--------------------------------------------------------------------------
    fun updateTeam( team: Team )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(team.id.toString())
        val values = ContentValues()

        putTeam( team, values )

        db.update(DAO.TABLE_TEAM, values, whereClause, args )
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
    fun getTeams( enumArea_id: Int ): List<Team>
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