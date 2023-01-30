package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_USER_PIN
import edu.gtri.gpssample.database.models.User

class UserDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createUser( user: User) : Int
    {
        val values = ContentValues()

        values.put( DAO.COLUMN_USER_UUID, user.uuid )
        values.put( DAO.COLUMN_USER_ROLE, user.role.toString() )
        values.put( DAO.COLUMN_USER_NAME, user.name )
        values.put( DAO.COLUMN_USER_PIN, user.pin )
        values.put( DAO.COLUMN_USER_RECOVERY_QUESTION, user.recoveryQuestion )
        values.put( DAO.COLUMN_USER_RECOVERY_ANSWER, user.recoveryAnswer )

        return dao.writableDatabase.insert(DAO.TABLE_USER, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun getUser( id: Int ): User?
    {
        var user: User? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_USER} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            user = createUserModel( cursor )
        }

        cursor.close()
        db.close()

        return user
    }

    //--------------------------------------------------------------------------
    fun getUser( name: String, pin: String ): User?
    {
        var user: User? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_USER} WHERE ${DAO.COLUMN_USER_NAME} = '$name' AND ${COLUMN_USER_PIN} = $pin"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            user = createUserModel( cursor )
        }

        cursor.close()
        db.close()

        return user
    }

    //--------------------------------------------------------------------------
    private fun createUserModel( cursor: Cursor) : User
    {
        val user = User()

        user.id = Integer.parseInt(cursor.getString(0))
        user.uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_UUID))
        user.role = Role.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_ROLE)))
        user.name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_NAME))
        user.pin = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_USER_PIN))
        user.recoveryQuestion = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_RECOVERY_QUESTION))
        user.recoveryAnswer = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_RECOVERY_ANSWER))

        return user
    }

    //--------------------------------------------------------------------------
    fun getUsers(): List<User>
    {
        val users = ArrayList<User>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_USER}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            users.add( createUserModel( cursor ))
        }

        cursor.close()
        db.close()

        return users
    }
}