/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CREATION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LOCATION_ALTITUDE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LOCATION_DESCRIPTION
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LOCATION_GPS_ACCURACY
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LOCATION_IMAGE_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LOCATION_IS_LANDMARK
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LOCATION_IS_MULTI_FAMILY
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LOCATION_LATITUDE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LOCATION_LONGITUDE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LOCATION_PROPERTIES
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_TIME_ZONE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_VERSION
import edu.gtri.gpssample.database.models.Location
import edu.gtri.gpssample.database.models.User
import edu.gtri.gpssample.extensions.toBoolean

class UserDAO(private var dao: DAO)
{
    fun createUser( user: User, version: String )
    {
        assert( user.uuid.isNotEmpty())

        user.version = version

        val values = ContentValues()

        putUser( user, values )

        dao.writableDatabase.insert(DAO.TABLE_USER, null, values)
    }

    fun putUser( user: User, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, user.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, user.creationDate )
        values.put( DAO.COLUMN_VERSION, user.version )
        values.put( DAO.COLUMN_USER_ROLE, user.role )
        values.put( DAO.COLUMN_USER_NAME, user.name )
        values.put( DAO.COLUMN_USER_RECOVERY_QUESTION, user.recoveryQuestion )
        values.put( DAO.COLUMN_USER_RECOVERY_ANSWER, user.recoveryAnswer )
    }

    fun getUser( name: String ): User?
    {
        var user: User? = null
        val query = "SELECT * FROM ${DAO.TABLE_USER} WHERE ${DAO.COLUMN_USER_NAME} = '$name'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            user = buildUser( cursor )
        }

        cursor.close()

        return user
    }

    @SuppressLint("Range")
    private fun buildUser(cursor: Cursor) : User
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_NAME))
        val role = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_ROLE))
        val recoveryQuestion = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_RECOVERY_QUESTION))
        val recoveryAnswer = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_USER_RECOVERY_ANSWER))
        val isOnline = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_USER_IS_ONLINE)).toBoolean()

        return User(uuid, creationDate, name, role, recoveryQuestion, recoveryAnswer, isOnline, version )
    }

    fun updateUser( user: User )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(user.uuid)
        val values = ContentValues()

        putUser( user, values )

        dao.writableDatabase.update(DAO.TABLE_USER, values, whereClause, args )
    }

    fun getUsers(): List<User>
    {
        val users = ArrayList<User>()
        val query = "SELECT * FROM ${DAO.TABLE_USER}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            users.add( buildUser( cursor ))
        }

        cursor.close()

        return users
    }

    companion object
    {
        val columnBindings = listOf(
            ColumnBinding<User>(DAO.COLUMN_CREATION_DATE, "INTEGER", User::creationDate),
            ColumnBinding<User>(DAO.COLUMN_VERSION,"TEXT",User::version ),
            ColumnBinding<User>(DAO.COLUMN_USER_ROLE,"TEXT",User::role ),
            ColumnBinding<User>(DAO.COLUMN_USER_NAME,"TEXT",User::name ),
            ColumnBinding<User>(DAO.COLUMN_USER_RECOVERY_QUESTION,"TEXT",User::recoveryQuestion ),
            ColumnBinding<User>(DAO.COLUMN_USER_RECOVERY_ANSWER,"TEXT",User::recoveryAnswer ),
            ColumnBinding<User>(DAO.COLUMN_USER_IS_ONLINE,"INTEGER",User::isOnline ),
        )
    }
}