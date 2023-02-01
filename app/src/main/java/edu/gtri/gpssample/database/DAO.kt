package edu.gtri.gpssample.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import edu.gtri.gpssample.constants.Key

class DAO(private var context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int )
    : SQLiteOpenHelper( context, DATABASE_NAME, factory, DATABASE_VERSION )
{
    //--------------------------------------------------------------------------
    override fun onCreate( db: SQLiteDatabase )
    {
        val createTableUser = ("CREATE TABLE " +
                TABLE_USER + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY" + "," +
                COLUMN_USER_UUID + " TEXT" +  "," +
                COLUMN_USER_ROLE + " TEXT" +  "," +
                COLUMN_USER_NAME + " TEXT" + "," +
                COLUMN_USER_PIN + " INTEGER" + "," +
                COLUMN_USER_RECOVERY_QUESTION + " TEXT" + "," +
                COLUMN_USER_RECOVERY_ANSWER + " TEXT" + "," +
                COLUMN_USER_IS_ONLINE + " BOOLEAN" +
                ")")
        db.execSQL(createTableUser)

        val createTableConfig = ("CREATE TABLE " +
                TABLE_CONFIG + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY" + "," +
                COLUMN_CONFIG_NAME + " TEXT" + "," +
                COLUMN_CONFIG_DATE_FORMAT + " TEXT" + "," +
                COLUMN_CONFIG_TIME_FORMAT + " TEXT" + "," +
                COLUMN_CONFIG_DISTANCE_FORMAT + " TEXT" + "," +
                COLUMN_CONFIG_MIN_GPS_PRECISION + " INTEGER" +
                ")")
        db.execSQL(createTableConfig)

        val createTableStudy = ("CREATE TABLE " +
                TABLE_STUDY + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY" + "," +
                COLUMN_STUDY_NAME + " TEXT" + "," +
                COLUMN_STUDY_CONFIG_ID + " INTEGER" + "," +
                COLUMN_STUDY_IS_VALID + " BOOLEAN" +
                ")")
        db.execSQL(createTableStudy)

        val createTableField = ("CREATE TABLE " +
                TABLE_FIELD + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY" + "," +
                COLUMN_FIELD_NAME + " TEXT" + "," +
                COLUMN_FIELD_STUDY_ID + " INTEGER" + "," +
                COLUMN_FIELD_TYPE + " STRING" + "," +
                COLUMN_FIELD_PII + " BOOLEAN" + "," +
                COLUMN_FIELD_REQUIRED + " BOOLEAN" + "," +
                COLUMN_FIELD_INTEGER_ONLY + " BOOLEAN" + "," +
                COLUMN_FIELD_DATE + " BOOLEAN" + "," +
                COLUMN_FIELD_TIME + " BOOLEAN" + "," +
                COLUMN_FIELD_OPTION_1 + " STRING" + "," +
                COLUMN_FIELD_OPTION_2 + " STRING" + "," +
                COLUMN_FIELD_OPTION_3 + " STRING" + "," +
                COLUMN_FIELD_OPTION_4 + " STRING" +
                ")")
        db.execSQL(createTableField)
    }

    //--------------------------------------------------------------------------
    override fun onUpgrade( db: SQLiteDatabase, oldVersion: Int, newVersion: Int )
    {
        // clear all tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONFIG)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDY)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FIELD)
        onCreate(db)
    }

    //--------------------------------------------------------------------------
    companion object
    {
        private const val DATABASE_NAME = "GPSSampleDB.db"
        const val COLUMN_ID = "id"

        // User Table
        const val TABLE_USER = "user"
        const val COLUMN_USER_UUID = "user_uuid"
        const val COLUMN_USER_ROLE = "user_role"
        const val COLUMN_USER_NAME = "user_name"
        const val COLUMN_USER_PIN = "user_pin"
        const val COLUMN_USER_RECOVERY_QUESTION = "user_recover_question"
        const val COLUMN_USER_RECOVERY_ANSWER = "user_recovery_answer"
        const val COLUMN_USER_IS_ONLINE = "user_is_online"

        // Config Table
        const val TABLE_CONFIG = "config"
        const val COLUMN_CONFIG_NAME = "config_name"
        const val COLUMN_CONFIG_DISTANCE_FORMAT = "config_distance_format"
        const val COLUMN_CONFIG_DATE_FORMAT = "config_date_format"
        const val COLUMN_CONFIG_TIME_FORMAT = "config_time_format"
        const val COLUMN_CONFIG_MIN_GPS_PRECISION = "config_min_gps_precision"

        // Study Table
        const val TABLE_STUDY = "study"
        const val COLUMN_STUDY_NAME = "study_name"
        const val COLUMN_STUDY_CONFIG_ID = "study_config_id"
        const val COLUMN_STUDY_IS_VALID = "study_is_valid"

        // Field Table
        const val TABLE_FIELD = "field"
        const val COLUMN_FIELD_NAME = "field_name"
        const val COLUMN_FIELD_STUDY_ID = "field_study_id"
        const val COLUMN_FIELD_TYPE = "field_type"
        const val COLUMN_FIELD_PII = "field_pii"
        const val COLUMN_FIELD_REQUIRED = "field_required"
        const val COLUMN_FIELD_INTEGER_ONLY = "field_integer_only"
        const val COLUMN_FIELD_DATE = "field_date"
        const val COLUMN_FIELD_TIME = "field_time"
        const val COLUMN_FIELD_OPTION_1 = "field_option_1"
        const val COLUMN_FIELD_OPTION_2 = "field_option_2"
        const val COLUMN_FIELD_OPTION_3 = "field_option_3"
        const val COLUMN_FIELD_OPTION_4 = "field_option_4"

        lateinit var userDAO: UserDAO
        lateinit var configDAO: ConfigDAO
        lateinit var studyDAO: StudyDAO
        lateinit var fieldDAO: FieldDAO

        // creation/access methods

        private var instance: DAO? = null

        fun createSharedInstance( context: Context ): DAO
        {
            if (instance == null)
            {
                instance = DAO( context, null, null, DATABASE_VERSION )

                userDAO = UserDAO( instance!! )
                configDAO = ConfigDAO( instance!! )
                studyDAO = StudyDAO(( instance!! ))
                fieldDAO = FieldDAO(( instance!! ))
            }

            return instance!!
        }

        fun sharedInstance(): DAO
        {
            return instance!!
        }

        private const val DATABASE_VERSION = 25
    }
}