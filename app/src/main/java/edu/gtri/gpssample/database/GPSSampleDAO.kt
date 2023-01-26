package edu.gtri.gpssample.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.core.content.contentValuesOf
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.models.Configuration
import edu.gtri.gpssample.models.Field
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
                COLUMN_STUDY_CONFIG_ID + " INTEGER," +
                COLUMN_STUDY_IS_VALID + " BOOLEAN" +
                ")")
        db.execSQL(createTableStudy)

        val createTableField = ("CREATE TABLE " +
                TABLE_FIELD + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY," +
                COLUMN_FIELD_NAME + " TEXT," +
                COLUMN_FIELD_STUDY_ID + " INTEGER," +
                COLUMN_FIELD_TYPE + " STRING," +
                COLUMN_FIELD_PII + " BOOLEAN," +
                COLUMN_FIELD_REQUIRED + " BOOLEAN," +
                COLUMN_FIELD_INTEGER_ONLY + " BOOLEAN," +
                COLUMN_FIELD_DATE + " BOOLEAN," +
                COLUMN_FIELD_TIME + " BOOLEAN," +
                COLUMN_FIELD_OPTION_1 + " STRING," +
                COLUMN_FIELD_OPTION_2 + " STRING," +
                COLUMN_FIELD_OPTION_3 + " STRING," +
                COLUMN_FIELD_OPTION_4 + " STRING" +
                ")")
        db.execSQL(createTableField)
    }

    //--------------------------------------------------------------------------
    override fun onUpgrade( db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
    {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONFIG)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDY)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FIELD)
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
        val query = "SELECT * FROM $TABLE_USER WHERE $COLUMN_ID = $id"

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
    fun createUser( cursor: Cursor ) : User
    {
        val user = User()

        user.id = Integer.parseInt(cursor.getString(0))
        user.role = Role.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_USER_ROLE)))
        user.name = cursor.getString(cursor.getColumnIndex(COLUMN_USER_NAME))
        user.pin = cursor.getInt(cursor.getColumnIndex(COLUMN_USER_PIN))
        user.recoveryQuestion = cursor.getString(cursor.getColumnIndex(COLUMN_USER_RECOVERY_QUESTION))
        user.recoveryAnswer = cursor.getString(cursor.getColumnIndex(COLUMN_USER_RECOVERY_ANSWER))

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
            users.add( createUser( cursor ))
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
        var config: Configuration? = null

        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_CONFIG WHERE $COLUMN_ID = $id"

        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            config = createConfiguration( cursor )
        }

        cursor.close()
        db.close()

        return config
    }

    //--------------------------------------------------------------------------
    fun createConfiguration( cursor: Cursor ) : Configuration
    {
        val config = Configuration()

        config.id = Integer.parseInt(cursor.getString(0))
        config.name = cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_NAME))
        config.distanceFormat = DistanceFormat.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_DISTANCE_FORMAT)))
        config.dateFormat = DateFormat.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_DATE_FORMAT)))
        config.timeFormat = TimeFormat.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_CONFIG_TIME_FORMAT)))
        config.minGpsPrecision = cursor.getInt(cursor.getColumnIndex(COLUMN_CONFIG_MIN_GPS_PRECISION))

        return config
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
            configurations.add( createConfiguration( cursor ))
        }

        cursor.close()
        db.close()

        return configurations
    }

    //--------------------------------------------------------------------------
    fun updateConfiguration( configuration: Configuration )
    {
        val db = this.writableDatabase
        val whereClause = "$COLUMN_ID = ?"
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

        putStudy( study, values )

        return this.writableDatabase.insert(TABLE_STUDY, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putStudy( study: Study, values: ContentValues )
    {
        values.put( COLUMN_STUDY_NAME, study.name )
        values.put( COLUMN_STUDY_CONFIG_ID, study.configId )
        values.put( COLUMN_STUDY_IS_VALID, study.isValid )
    }

    //--------------------------------------------------------------------------
    fun updateStudy( study: Study )
    {
        val db = this.writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val args: Array<String> = arrayOf(study.id.toString())

        val values = ContentValues()

        putStudy( study, values )

        db.update( TABLE_STUDY, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getStudy( id: Int ): Study?
    {
        var study: Study? = null

        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_STUDY WHERE $COLUMN_ID = $id AND $COLUMN_STUDY_IS_VALID = 1"

        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            study = createStudy( cursor )
        }

        cursor.close()
        db.close()

        return study
    }

    //--------------------------------------------------------------------------
    fun createStudy( cursor: Cursor ): Study
    {
        val study = Study()

        study.id = Integer.parseInt(cursor.getString(0))
        study.name = cursor.getString(cursor.getColumnIndex(COLUMN_STUDY_NAME))
        study.configId = cursor.getInt(cursor.getColumnIndex(COLUMN_STUDY_CONFIG_ID))
        study.isValid = cursor.getInt(cursor.getColumnIndex(COLUMN_STUDY_IS_VALID)).toBoolean()

        return study
    }

    //--------------------------------------------------------------------------
    fun getStudies( configId: Int ): List<Study>
    {
        val studies = ArrayList<Study>()
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_STUDY WHERE $COLUMN_STUDY_CONFIG_ID = $configId AND $COLUMN_STUDY_IS_VALID = 1"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            studies.add( createStudy( cursor ))
        }

        cursor.close()
        db.close()

        return studies
    }

    //--------------------------------------------------------------------------
    fun deleteStudy( study: Study )
    {
        val db = this.writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val args = arrayOf(study.id.toString())
        db.delete(TABLE_STUDY, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun createField( field: Field ) : Int
    {
        val values = ContentValues()

        putField( field, values )

        return this.writableDatabase.insert(TABLE_FIELD, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putField( field: Field, values: ContentValues )
    {
        values.put( COLUMN_FIELD_NAME, field.name )
        values.put( COLUMN_FIELD_STUDY_ID, field.studyId )
        values.put( COLUMN_FIELD_TYPE, field.type.toString() )
        values.put( COLUMN_FIELD_PII, field.pii )
        values.put( COLUMN_FIELD_REQUIRED, field.required )
        values.put( COLUMN_FIELD_INTEGER_ONLY, field.integerOnly )
        values.put( COLUMN_FIELD_DATE, field.date )
        values.put( COLUMN_FIELD_TIME, field.time )
        values.put( COLUMN_FIELD_OPTION_1, field.option1 )
        values.put( COLUMN_FIELD_OPTION_2, field.option2 )
        values.put( COLUMN_FIELD_OPTION_3, field.option3 )
        values.put( COLUMN_FIELD_OPTION_4, field.option4 )
    }

    //--------------------------------------------------------------------------
    fun updateField( field: Field )
    {
        val db = this.writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val args: Array<String> = arrayOf(field.id.toString())

        val values = ContentValues()

        putField( field, values )

        db.update( TABLE_FIELD, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getField( fieldId: Int ): Field?
    {
        var field: Field? = null
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_FIELD WHERE $COLUMN_ID = $fieldId"

        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            field = createField( cursor )
        }

        cursor.close()
        db.close()

        return field
    }

    //--------------------------------------------------------------------------
    fun  createField( cursor: Cursor): Field
    {
        val field = Field()

        field.id = cursor.getInt(cursor.getColumnIndex(COLUMN_ID))
        field.name = cursor.getString(cursor.getColumnIndex(COLUMN_FIELD_NAME))
        field.studyId = cursor.getInt(cursor.getColumnIndex(COLUMN_FIELD_STUDY_ID))
        field.type = FieldType.valueOf(cursor.getString(cursor.getColumnIndex(COLUMN_FIELD_TYPE)))
        field.pii = cursor.getInt(cursor.getColumnIndex(COLUMN_FIELD_PII)).toBoolean()
        field.required = cursor.getInt(cursor.getColumnIndex(COLUMN_FIELD_REQUIRED)).toBoolean()
        field.integerOnly = cursor.getInt(cursor.getColumnIndex(COLUMN_FIELD_INTEGER_ONLY)).toBoolean()
        field.date = cursor.getInt(cursor.getColumnIndex(COLUMN_FIELD_DATE)).toBoolean()
        field.time = cursor.getInt(cursor.getColumnIndex(COLUMN_FIELD_TIME)).toBoolean()
        field.option1 = cursor.getString(cursor.getColumnIndex(COLUMN_FIELD_OPTION_1))
        field.option2 = cursor.getString(cursor.getColumnIndex(COLUMN_FIELD_OPTION_2))
        field.option3 = cursor.getString(cursor.getColumnIndex(COLUMN_FIELD_OPTION_3))
        field.option4 = cursor.getString(cursor.getColumnIndex(COLUMN_FIELD_OPTION_4))

        return field
    }

    //--------------------------------------------------------------------------
    fun getFields( studyId: Int ): List<Field>
    {
        val fields = ArrayList<Field>()
        val db = this.writableDatabase
        val query = "SELECT * FROM $TABLE_FIELD WHERE $COLUMN_FIELD_STUDY_ID = $studyId"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            fields.add( createField( cursor ))
        }

        cursor.close()
        db.close()

        return fields
    }

    //--------------------------------------------------------------------------
    fun deleteField( field: Field )
    {
        val db = this.writableDatabase
        val whereClause = "$COLUMN_ID = ?"
        val args = arrayOf(field.id.toString())
        db.delete(TABLE_FIELD, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    companion object
    {
        private const val DATABASE_VERSION = 14
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
        private const val COLUMN_STUDY_IS_VALID = "study_is_valid"

        // Field Table
        private const val TABLE_FIELD = "field"
        private const val COLUMN_FIELD_NAME = "field_name"
        private const val COLUMN_FIELD_STUDY_ID = "field_study_id"
        private const val COLUMN_FIELD_TYPE = "field_type"
        private const val COLUMN_FIELD_PII = "field_pii"
        private const val COLUMN_FIELD_REQUIRED = "field_required"
        private const val COLUMN_FIELD_INTEGER_ONLY = "field_integer_only"
        private const val COLUMN_FIELD_DATE = "field_date"
        private const val COLUMN_FIELD_TIME = "field_time"
        private const val COLUMN_FIELD_OPTION_1 = "field_option_1"
        private const val COLUMN_FIELD_OPTION_2 = "field_option_2"
        private const val COLUMN_FIELD_OPTION_3 = "field_option_3"
        private const val COLUMN_FIELD_OPTION_4 = "field_option_4"

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