package edu.gtri.gpssample.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.models.Configuration
import edu.gtri.gpssample.models.Study
import edu.gtri.gpssample.models.User

class GPSSampleDAO( context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int )
    : SQLiteOpenHelper( context, DATABASE_NAME, factory, DATABASE_VERSION )
{
    //--------------------------------------------------------------------------
    override fun onCreate(db: SQLiteDatabase)
    {
        val createTableUser = ("CREATE TABLE " +
                TABLE_USER + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_USER_ROLE + " TEXT," +
                COLUMN_USER_NAME + " TEXT," +
                COLUMN_USER_PIN + " INTEGER," +
                COLUMN_USER_RECOVERY_QUESTION + " TEXT," +
                COLUMN_USER_RECOVERY_ANSWER + " TEXT" +
                ")")
        db.execSQL(createTableUser)

        val createTableConfig = ("CREATE TABLE " +
                TABLE_CONFIG + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_CONFIG_NAME + " TEXT," +
                COLUMN_CONFIG_DISTANCE_FORMAT + " TEXT," +
                COLUMN_CONFIG_DATE_FORMAT + " TEXT," +
                COLUMN_CONFIG_TIME_FORMAT + " TEXT," +
                COLUMN_CONFIG_MIN_GPS_PRECISION + " INTEGER" +
                ")")
        db.execSQL(createTableConfig)

        val createTableStudy = ("CREATE TABLE " +
                TABLE_STUDY + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_STUDY_NAME + " TEXT," +
                COLUMN_STUDY_CONFIG_ID + " INTEGER" +
                ")")
        db.execSQL(createTableStudy)
    }

    //--------------------------------------------------------------------------
    override fun onUpgrade( db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONFIG)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDY)
        onCreate(db)
    }

    //--------------------------------------------------------------------------
    fun createUser( user: User ) : Int
    {
        val values = ContentValues()

        values.put( COLUMN_USER_ROLE, user.role.toString() )
        values.put( COLUMN_USER_NAME, user.name )
        values.put( COLUMN_USER_PIN, user.pin )
        values.put( COLUMN_USER_RECOVERY_QUESTION, user.recoveryQuestion )
        values.put( COLUMN_USER_RECOVERY_ANSWER, user.recoveryAnswer )

        return this.writableDatabase.insert(TABLE_USER, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun getUser( id: Int ): User?
    {
        var user: User? = null
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_USER WHERE ID = ${id}"

        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            user = User()

            user.id = Integer.parseInt(cursor.getString(0))
            user.role = Role.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_USER_ROLE)))
            user.name = cursor.getString(cursor.getColumnIndex(COLUMN_USER_NAME))
            user.pin = cursor.getInt(cursor.getColumnIndex(COLUMN_USER_PIN))
            user.recoveryQuestion = cursor.getString(cursor.getColumnIndex(COLUMN_USER_RECOVERY_QUESTION))
            user.recoveryAnswer = cursor.getString(cursor.getColumnIndex(COLUMN_USER_RECOVERY_ANSWER))
        }

        cursor.close()
        db.close()

        return user
    }

    //--------------------------------------------------------------------------
    fun getUsers(): List<User>
    {
        val users = ArrayList<User>()
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_USER"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val user = User()

            user.id = Integer.parseInt(cursor.getString(0))
            user.role = Role.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_USER_ROLE)))
            user.name = cursor.getString(cursor.getColumnIndex(COLUMN_USER_NAME))
            user.pin = cursor.getInt(cursor.getColumnIndex(COLUMN_USER_PIN))
            user.recoveryQuestion = cursor.getString(cursor.getColumnIndex(COLUMN_USER_RECOVERY_QUESTION))
            user.recoveryAnswer = cursor.getString(cursor.getColumnIndex(COLUMN_USER_RECOVERY_ANSWER))

            users.add( user )
        }

        cursor.close()
        db.close()

        return users
    }

    //--------------------------------------------------------------------------
    fun createConfiguration( configuration: Configuration ) : Int
    {
        val values = ContentValues()

        values.put( COLUMN_CONFIG_NAME, configuration.name )
        values.put( COLUMN_CONFIG_DISTANCE_FORMAT, configuration.distanceFormat.toString() )
        values.put( COLUMN_CONFIG_DATE_FORMAT, configuration.dateFormat.toString() )
        values.put( COLUMN_CONFIG_TIME_FORMAT, configuration.timeFormat.toString() )
        values.put( COLUMN_CONFIG_MIN_GPS_PRECISION, configuration.minGpsPrecision )

        return this.writableDatabase.insert(TABLE_CONFIG, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun getConfiguration( id: Int ): Configuration?
    {
        var configuration: Configuration? = null

        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_CONFIG WHERE ID = ${id}"

        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            configuration = Configuration()

            configuration.id = Integer.parseInt(cursor.getString(0))
            configuration.name = cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_NAME))
            configuration.distanceFormat = DistanceFormat.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_DISTANCE_FORMAT)))
            configuration.dateFormat = DateFormat.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_DATE_FORMAT)))
            configuration.timeFormat = TimeFormat.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_TIME_FORMAT)))
            configuration.minGpsPrecision = cursor.getInt(cursor.getColumnIndex(COLUMN_CONFIG_MIN_GPS_PRECISION))
        }

        cursor.close()
        db.close()

        return configuration
    }

    //--------------------------------------------------------------------------
    fun getConfigurations(): List<Configuration>
    {
        val configurations = ArrayList<Configuration>()
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_CONFIG"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val configuration = Configuration()

            configuration.id = Integer.parseInt(cursor.getString(0))
            configuration.name = cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_NAME))
            configuration.distanceFormat = DistanceFormat.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_DISTANCE_FORMAT)))
            configuration.dateFormat = DateFormat.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_DATE_FORMAT)))
            configuration.timeFormat = TimeFormat.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_TIME_FORMAT)))
            configuration.minGpsPrecision = cursor.getInt(cursor.getColumnIndex(COLUMN_CONFIG_MIN_GPS_PRECISION))

            configurations.add( configuration )
        }

        cursor.close()
        db.close()

        return configurations
    }

    //--------------------------------------------------------------------------
    fun updateConfiguration( configuration: Configuration )
    {
        val db = this.writableDatabase
        val whereClause = "${COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(configuration.id.toString())

        val values = ContentValues()

        values.put( COLUMN_CONFIG_NAME, configuration.name )
        values.put( COLUMN_CONFIG_DISTANCE_FORMAT, configuration.distanceFormat.toString())
        values.put( COLUMN_CONFIG_DATE_FORMAT, configuration.dateFormat.toString())
        values.put( COLUMN_CONFIG_TIME_FORMAT, configuration.timeFormat.toString())
        values.put( COLUMN_CONFIG_MIN_GPS_PRECISION, configuration.minGpsPrecision )

        db.update( TABLE_CONFIG, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteConfiguration( configuration: Configuration )
    {
        val studies = getStudies( configuration.id )

        for (study in studies)
        {
            deleteStudy( study )
        }

        val db = this.writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val args = arrayOf(configuration.id.toString())

        db.delete(TABLE_CONFIG, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun createStudy( study: Study) : Int
    {
        val values = ContentValues()

        values.put( COLUMN_STUDY_NAME, study.name )
        values.put( COLUMN_STUDY_CONFIG_ID, study.configId )

        return this.writableDatabase.insert(TABLE_STUDY, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun getStudy( id: Int ): Study?
    {
        var study: Study? = null

        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_STUDY WHERE ID = ${id}"

        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            study = Study()

            study.id = Integer.parseInt(cursor.getString(0))
            study.name = cursor.getString(cursor.getColumnIndex(COLUMN_STUDY_NAME))
            study.configId = cursor.getInt(cursor.getColumnIndex(COLUMN_STUDY_CONFIG_ID))
        }

        cursor.close()
        db.close()

        return study
    }

    //--------------------------------------------------------------------------
    fun getStudies(): List<Study>
    {
        val studies = ArrayList<Study>()
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_STUDY"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val study = Study()

            study.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
            study.name = cursor.getString(cursor.getColumnIndex(COLUMN_STUDY_NAME))
            study.configId = cursor.getInt(cursor.getColumnIndex(COLUMN_STUDY_CONFIG_ID))

            studies.add( study )
        }

        cursor.close()
        db.close()

        return studies
    }

    //--------------------------------------------------------------------------
    fun getStudies( configId: Int ): List<Study>
    {
        val studies = ArrayList<Study>()
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_STUDY WHERE $COLUMN_STUDY_CONFIG_ID = $configId"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val study = Study()

            study.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
            study.name = cursor.getString(cursor.getColumnIndex(COLUMN_STUDY_NAME))
            study.configId = cursor.getInt(cursor.getColumnIndex(COLUMN_STUDY_CONFIG_ID))

            studies.add( study )
        }

        cursor.close()
        db.close()

        return studies
    }

    //--------------------------------------------------------------------------
    fun updateStudy( study: Study )
    {
        val db = this.writableDatabase
        val whereClause = "${COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(study.id.toString())

        val values = ContentValues()

        values.put( COLUMN_STUDY_NAME, study.name )
        values.put( COLUMN_STUDY_CONFIG_ID, study.configId )

        db.update( TABLE_STUDY, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteStudy( study: Study )
    {
        val db = this.writableDatabase
        val whereClause = "${COLUMN_ID} = ?"
        val args = arrayOf(study.id.toString())
        db.delete(TABLE_STUDY, whereClause, args)
        db.close()
    }

    companion object
    {
        private const val DATABASE_VERSION = 11
        private const val DATABASE_NAME = "GPSSampleDB.db"
        private const val COLUMN_ID = "id"

        // User Table
        private const val TABLE_USER = "user"
        private const val COLUMN_USER_ROLE = "user_role"
        private const val COLUMN_USER_NAME = "user_name"
        private const val COLUMN_USER_PIN = "user_pin"
        private const val COLUMN_USER_RECOVERY_QUESTION = "user_recover_question"
        private const val COLUMN_USER_RECOVERY_ANSWER = "user_recovery_answer"

        // Config Table
        private const val TABLE_CONFIG = "config"
        private const val COLUMN_CONFIG_NAME = "config_name"
        private const val COLUMN_CONFIG_DISTANCE_FORMAT = "config_distance_format"
        private const val COLUMN_CONFIG_DATE_FORMAT = "config_date_format"
        private const val COLUMN_CONFIG_TIME_FORMAT = "config_time_format"
        private const val COLUMN_CONFIG_MIN_GPS_PRECISION = "config_min_gps_precision"

        // Study Table
        private const val TABLE_STUDY = "study"
        private const val COLUMN_STUDY_NAME = "study_name"
        private const val COLUMN_STUDY_CONFIG_ID = "study_config_id"

        // creation/access methods

        private var instance: GPSSampleDAO? = null

        fun createSharedInstance( context: Context ): GPSSampleDAO
        {
            if (instance == null)
            {
                instance = GPSSampleDAO( context, null, null, DATABASE_VERSION )
            }

            return instance!!
        }

        fun sharedInstance(): GPSSampleDAO
        {
            return instance!!
        }
    }
}