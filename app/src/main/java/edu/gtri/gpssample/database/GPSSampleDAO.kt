package edu.gtri.gpssample.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.models.User

class GPSSampleDAO( context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int )
    : SQLiteOpenHelper( context, DATABASE_NAME, factory, DATABASE_VERSION )
{
    override fun onCreate(db: SQLiteDatabase)
    {
        val createIntendedLocationTable = ("CREATE TABLE " +
                TABLE_USER + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_USER_ROLE + " TEXT," +
                COLUMN_USER_NAME + " TEXT," +
                COLUMN_USER_PIN + " INTEGER," +
                COLUMN_USER_RECOVERY_QUESTION + " TEXT," +
                COLUMN_USER_RECOVERY_ANSWER + " TEXT" +
                ")")
        db.execSQL(createIntendedLocationTable)
    }

    override fun onUpgrade( db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER)
        onCreate(db)
    }

    fun createUser( role: String, name: String, pin: Int, recoveryQuestion: String, recoveryAnswer: String ) : Int
    {
        val values = ContentValues()

        values.put( COLUMN_USER_ROLE, role )
        values.put( COLUMN_USER_NAME, name )
        values.put( COLUMN_USER_PIN, pin )
        values.put( COLUMN_USER_RECOVERY_QUESTION, recoveryQuestion )
        values.put( COLUMN_USER_RECOVERY_ANSWER, recoveryAnswer )

        return this.writableDatabase.insert(TABLE_USER, null, values).toInt()
    }

    fun getUser( id: Int ): User?
    {
        try {
            val db = this.writableDatabase
            val query = "SELECT * FROM $TABLE_USER WHERE ID = ${id}"

            val cursor = db.rawQuery(query, null)

            if (cursor.count > 0)
            {
                cursor.moveToNext()
                val user = User()

                user.id = Integer.parseInt(cursor.getString(0))
                user.role = Role.valueOf( cursor.getString(cursor.getColumnIndex(COLUMN_USER_ROLE)))
                user.name = cursor.getString(cursor.getColumnIndex(COLUMN_USER_NAME))
                user.pin = cursor.getInt(cursor.getColumnIndex(COLUMN_USER_PIN))
                user.recoveryQuestion = cursor.getString(cursor.getColumnIndex(COLUMN_USER_RECOVERY_QUESTION))
                user.recoveryAnswer = cursor.getString(cursor.getColumnIndex(COLUMN_USER_RECOVERY_ANSWER))

                cursor.close()
                db.close()

                return user
            }
        }
        catch( ex: Exception )
        {
            println( ex.localizedMessage )
            ex.printStackTrace()
        }

        return null
    }

    companion object
    {
        private const val DATABASE_VERSION = 3
        private const val DATABASE_NAME = "GPSSampleDB.db"
        private const val COLUMN_ID = "id"

        private const val TABLE_USER = "user"
        private const val COLUMN_USER_ROLE = "role"
        private const val COLUMN_USER_NAME = "name"
        private const val COLUMN_USER_PIN = "pin"
        private const val COLUMN_USER_RECOVERY_QUESTION = "recover_question"
        private const val COLUMN_USER_RECOVERY_ANSWER = "recovery_answer"

        private var instance: GPSSampleDAO? = null

        fun sharedInstance(): GPSSampleDAO
        {
            return instance!!
        }

        fun createSharedInstance( context: Context ): GPSSampleDAO
        {
            if (instance == null)
            {
                instance = GPSSampleDAO( context, null, null, DATABASE_VERSION )
            }

            return instance!!
        }
    }
}