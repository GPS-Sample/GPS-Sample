package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_USER_PIN
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.extensions.toBoolean

class UserDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createUser( user: User) : Int
    {
        val values = ContentValues()

        values.put( DAO.COLUMN_UUID, user.uuid )
        values.put( DAO.COLUMN_USER_ROLE, user.role )
        values.put( DAO.COLUMN_USER_NAME, user.name )
        values.put( DAO.COLUMN_USER_PIN, user.pin )
        values.put( DAO.COLUMN_USER_RECOVERY_QUESTION, user.recoveryQuestion )
        values.put( DAO.COLUMN_USER_RECOVERY_ANSWER, user.recoveryAnswer )

        return dao.writableDatabase.insert(DAO.TABLE_USER, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun getUser( uuid: String ): User?
    {
        var user: User? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_USER} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            user = createUser( cursor )
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

            user = createUser( cursor )
        }

        cursor.close()
        db.close()

        return user
    }

    //--------------------------------------------------------------------------
    private fun createUser( cursor: Cursor) : User
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_NAME))
        val pin = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_USER_PIN))
        val role = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_ROLE))
        val recoveryQuestion = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_RECOVERY_QUESTION))
        val recoveryAnswer = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_RECOVERY_ANSWER))
        val isOnline = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_USER_IS_ONLINE)).toBoolean()

        return User( uuid, name, pin, role, recoveryQuestion, recoveryAnswer, isOnline )
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
            users.add( createUser( cursor ))
        }

        cursor.close()
        db.close()

        return users
    }
}