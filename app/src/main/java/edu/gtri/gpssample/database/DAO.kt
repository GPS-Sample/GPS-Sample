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
                COLUMN_UUID + " TEXT PRIMARY KEY" + "," +
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
                COLUMN_UUID + " TEXT PRIMARY KEY" + "," +
                COLUMN_CONFIG_NAME + " TEXT" + "," +
                COLUMN_CONFIG_DATE_FORMAT + " TEXT" + "," +
                COLUMN_CONFIG_TIME_FORMAT + " TEXT" + "," +
                COLUMN_CONFIG_DISTANCE_FORMAT + " TEXT" + "," +
                COLUMN_CONFIG_MIN_GPS_PRECISION + " INTEGER" +
                ")")
        db.execSQL(createTableConfig)

        val createTableStudy = ("CREATE TABLE " +
                TABLE_STUDY + "(" +
                COLUMN_UUID + " TEXT PRIMARY KEY" + "," +
                COLUMN_STUDY_NAME + " TEXT" + "," +
                COLUMN_STUDY_CONFIG_UUID + " TEXT" + "," +
                COLUMN_STUDY_SAMPLING_METHOD + " TEXT" + "," +
                COLUMN_STUDY_SAMPLE_SIZE + " INTEGER" + "," +
                COLUMN_STUDY_SAMPLE_SIZE_INDEX + " INTEGER" +
                ")")
        db.execSQL(createTableStudy)

        val createTableField = ("CREATE TABLE " +
                TABLE_FIELD + "(" +
                COLUMN_UUID + " TEXT PRIMARY KEY" + "," +
                COLUMN_FIELD_NAME + " TEXT" + "," +
                COLUMN_FIELD_STUDY_UUID + " TEXT" + "," +
                COLUMN_FIELD_TYPE + " TEXT" + "," +
                COLUMN_FIELD_PII + " BOOLEAN" + "," +
                COLUMN_FIELD_REQUIRED + " BOOLEAN" + "," +
                COLUMN_FIELD_INTEGER_ONLY + " BOOLEAN" + "," +
                COLUMN_FIELD_DATE + " BOOLEAN" + "," +
                COLUMN_FIELD_TIME + " BOOLEAN" + "," +
                COLUMN_FIELD_OPTION_1 + " TEXT" + "," +
                COLUMN_FIELD_OPTION_2 + " TEXT" + "," +
                COLUMN_FIELD_OPTION_3 + " TEXT" + "," +
                COLUMN_FIELD_OPTION_4 + " TEXT" +
                ")")
        db.execSQL(createTableField)

        val createTableRule = ("CREATE TABLE " +
                TABLE_RULE + "(" +
                COLUMN_UUID + " TEXT PRIMARY KEY" + "," +
                COLUMN_RULE_STUDY_UUID + " TEXT" + "," +
                COLUMN_RULE_FIELD_UUID + " TEXT" + "," +
                COLUMN_RULE_NAME + " TEXT" + "," +
                COLUMN_RULE_OPERATOR + " TEXT" + "," +
                COLUMN_RULE_VALUE + " TEXT" +
                ")")
        db.execSQL(createTableRule)

        val createTableFilter = ("CREATE TABLE " +
                TABLE_FILTER + "(" +
                COLUMN_UUID + " TEXT PRIMARY KEY" + "," +
                COLUMN_FILTER_STUDY_UUID + " TEXT" + "," +
                COLUMN_FILTER_NAME + " TEXT" + "," +
                COLUMN_FILTER_SAMPLE_SIZE + " INTEGER" + "," +
                COLUMN_FILTER_SAMPLE_SIZE_INDEX + " INTEGER" +
                ")")
        db.execSQL(createTableFilter)

        val createTableFilterRule = ("CREATE TABLE " +
                TABLE_FILTERRULE + "(" +
                COLUMN_UUID + " TEXT PRIMARY KEY" + "," +
                COLUMN_FILTERRULE_STUDY_UUID + " TEXT" + "," +
                COLUMN_FILTERRULE_FILTER_UUID + " TEXT" + "," +
                COLUMN_FILTERRULE_RULE_UUID + " TEXT" + "," +
                COLUMN_FILTERRULE_CONNECTOR + " TEXT" +
                ")")
        db.execSQL(createTableFilterRule)

        val createTableSample = ("CREATE TABLE " +
                TABLE_SAMPLE + "(" +
                COLUMN_UUID + " TEXT PRIMARY KEY" + "," +
                COLUMN_SAMPLE_STUDY_UUID + " TEXT" + "," +
                COLUMN_SAMPLE_NAME + " TEXT" + "," +
                COLUMN_SAMPLE_NUM_ENUMERATORS + " INTEGER" +
                ")")
        db.execSQL(createTableSample)

        val createTableNavigationPlan = ("CREATE TABLE " +
                TABLE_NAV_PLAN + "(" +
                COLUMN_UUID + " TEXT PRIMARY KEY" + "," +
                COLUMN_NAV_PLAN_SAMPLE_UUID + " TEXT" + "," +
                COLUMN_NAV_PLAN_NAME + " TEXT" +
                ")")
        db.execSQL(createTableNavigationPlan)
    }

    //--------------------------------------------------------------------------
    override fun onUpgrade( db: SQLiteDatabase, oldVersion: Int, newVersion: Int )
    {
        // clear all tables
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CONFIG)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STUDY)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FIELD)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RULE)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FILTER)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_FILTERRULE)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SAMPLE)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAV_PLAN)

        onCreate(db)
    }

    //--------------------------------------------------------------------------
    companion object
    {
        private const val DATABASE_NAME = "GPSSampleDB.db"
        const val COLUMN_UUID = "uuid"

        // User Table
        const val TABLE_USER = "user"
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
        const val COLUMN_STUDY_CONFIG_UUID = "study_config_id"
        const val COLUMN_STUDY_SAMPLING_METHOD = "study_sampling_method"
        const val COLUMN_STUDY_SAMPLE_SIZE = "study_sample_size"
        const val COLUMN_STUDY_SAMPLE_SIZE_INDEX = "study_sample_size_index"

        // Field Table
        const val TABLE_FIELD = "field"
        const val COLUMN_FIELD_NAME = "field_name"
        const val COLUMN_FIELD_STUDY_UUID = "field_study_id"
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

        // Rule Table
        const val TABLE_RULE = "rule"
        const val COLUMN_RULE_STUDY_UUID = "rule_study_id"
        const val COLUMN_RULE_FIELD_UUID = "rule_field_id"
        const val COLUMN_RULE_NAME = "rule_name"
        const val COLUMN_RULE_OPERATOR = "rule_operator"
        const val COLUMN_RULE_VALUE = "rule_value"

        // Filter Table
        const val TABLE_FILTER = "filter"
        const val COLUMN_FILTER_STUDY_UUID = "filter_study_id"
        const val COLUMN_FILTER_NAME = "filter_name"
        const val COLUMN_FILTER_SAMPLE_SIZE = "filter_sample_size"
        const val COLUMN_FILTER_SAMPLE_SIZE_INDEX = "filter_sample_size_index"

        // FilterRule Table
        const val TABLE_FILTERRULE = "filterrule"
        const val COLUMN_FILTERRULE_STUDY_UUID = "filterrule_study_id"
        const val COLUMN_FILTERRULE_FILTER_UUID = "filterrule_filter_id"
        const val COLUMN_FILTERRULE_RULE_UUID = "filterrule_rule_id"
        const val COLUMN_FILTERRULE_CONNECTOR = "filterrule_connector"

        // Sample Table
        const val TABLE_SAMPLE = "sample"
        const val COLUMN_SAMPLE_STUDY_UUID = "sample_study_id"
        const val COLUMN_SAMPLE_NAME = "sample_name"
        const val COLUMN_SAMPLE_NUM_ENUMERATORS = "sample_num_enumerators"

        // NavigationPlan Table
        const val TABLE_NAV_PLAN = "nav_plan"
        const val COLUMN_NAV_PLAN_SAMPLE_UUID = "nav_plan_sample_id"
        const val COLUMN_NAV_PLAN_NAME = "nav_plan_name"

        // DAO's
        lateinit var userDAO: UserDAO
        lateinit var configDAO: ConfigDAO
        lateinit var studyDAO: StudyDAO
        lateinit var fieldDAO: FieldDAO
        lateinit var ruleDAO: RuleDAO
        lateinit var filterDAO: FilterDAO
        lateinit var filterRuleDAO: FilterRuleDAO
        lateinit var sampleDAO: SampleDAO
        lateinit var navPlanDAO: NavPlanDAO

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
                ruleDAO = RuleDAO(( instance!! ))
                filterDAO = FilterDAO(( instance!! ))
                filterRuleDAO = FilterRuleDAO(( instance!! ))
                sampleDAO = SampleDAO(( instance!! ))
                navPlanDAO = NavPlanDAO(( instance!! ))
            }

            return instance!!
        }

        private const val DATABASE_VERSION = 45
    }
}