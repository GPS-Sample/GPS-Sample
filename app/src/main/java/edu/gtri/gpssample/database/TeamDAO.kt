package edu.gtri.gpssample.database

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
        values.put( DAO.COLUMN_UUID, team.uuid )
        values.put( DAO.COLUMN_TEAM_ENUM_AREA_UUID, team.enumArea_uuid )
        values.put( DAO.COLUMN_TEAM_NAME, team.name )
    }

    //--------------------------------------------------------------------------
    private fun createTeam( cursor: Cursor): Team
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val enum_area_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_TEAM_ENUM_AREA_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_TEAM_NAME))

        return Team( uuid, enum_area_uuid, name )
    }

    //--------------------------------------------------------------------------
    fun updateTeam( team: Team )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(team.uuid)
        val values = ContentValues()

        putTeam( team, values )

        db.update(DAO.TABLE_TEAM, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getTeam( uuid: String ): Team?
    {
        var team: Team? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
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
    fun getTeams( enumArea_uuid: String ): List<Team>
    {
        val teams = ArrayList<Team>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM} WHERE ${DAO.COLUMN_TEAM_ENUM_AREA_UUID} = '$enumArea_uuid'"
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
    fun exists( uuid: String ) : Boolean
    {
        return getTeam( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun deleteTeam( team: Team )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(team.uuid.toString())

        db.delete(DAO.TABLE_TEAM, whereClause, args)
        db.close()
    }
}