package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.*

class TeamMemberDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createTeamMember( teamMember: TeamMember) : Int
    {
        val values = ContentValues()

        putTeamMember( teamMember, values )

        return dao.writableDatabase.insert(DAO.TABLE_TEAM_MEMBER, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putTeamMember(teamMember: TeamMember, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, teamMember.uuid )
        values.put( DAO.COLUMN_TEAM_MEMBER_TEAM_UUID, teamMember.team_uuid )
        values.put( DAO.COLUMN_TEAM_MEMBER_NAME, teamMember.name )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createTeamMember(cursor: Cursor): TeamMember
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val team_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_TEAM_MEMBER_TEAM_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_TEAM_MEMBER_NAME))

        return TeamMember(id, uuid, team_uuid, name )
    }

    //--------------------------------------------------------------------------
    fun updateTeamMember( teamMember: TeamMember )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(teamMember.uuid)
        val values = ContentValues()

        putTeamMember( teamMember, values )

        db.update(DAO.TABLE_TEAM_MEMBER, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getTeamMember( uuid: String ): TeamMember?
    {
        var teamMember: TeamMember? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM_MEMBER} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            teamMember = createTeamMember( cursor )
        }

        cursor.close()
        db.close()

        return teamMember
    }

    //--------------------------------------------------------------------------
    fun getTeamMembers( team_uuid: String ): List<TeamMember>
    {
        val teamMembers = ArrayList<TeamMember>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM_MEMBER} WHERE ${DAO.COLUMN_TEAM_MEMBER_TEAM_UUID} = '$team_uuid'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            teamMembers.add( createTeamMember( cursor ))
        }

        cursor.close()
        db.close()

        return teamMembers
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getTeamMember( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun deleteTeamMember( teamMember: TeamMember )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(teamMember.uuid.toString())

        db.delete(DAO.TABLE_TEAM_MEMBER, whereClause, args)
        db.close()
    }
}