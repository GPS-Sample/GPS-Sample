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
        values.put( DAO.COLUMN_TEAM_ID, teamMember.team_id )
        values.put( DAO.COLUMN_TEAM_MEMBER_NAME, teamMember.name )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createTeamMember(cursor: Cursor): TeamMember
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val team_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_TEAM_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_TEAM_MEMBER_NAME))

        return TeamMember(id, team_id, name )
    }

    //--------------------------------------------------------------------------
    fun getTeamMember( id: Int ): TeamMember?
    {
        var teamMember: TeamMember? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM_MEMBER} WHERE ${DAO.COLUMN_ID} = $id"
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
    fun getTeamMembers( team_id: Int ): List<TeamMember>
    {
        val teamMembers = ArrayList<TeamMember>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_TEAM_MEMBER} WHERE ${DAO.COLUMN_TEAM_ID} = '$team_id'"
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
    fun deleteTeamMember( teamMember: TeamMember )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(teamMember.id.toString())

        db.delete(DAO.TABLE_TEAM_MEMBER, whereClause, args)
        db.close()
    }
}