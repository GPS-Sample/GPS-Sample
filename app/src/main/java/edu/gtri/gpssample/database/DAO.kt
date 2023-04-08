package edu.gtri.gpssample.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DAO(private var context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int )
    : SQLiteOpenHelper( context, DATABASE_NAME, factory, DATABASE_VERSION )
{
    //--------------------------------------------------------------------------
    override fun onCreate( db: SQLiteDatabase )
    {
        val createTableUser = ("CREATE TABLE " +
                TABLE_USER + "(" +
                COLUMN_ID + COLUMN_ID_TYPE + "," +
                COLUMN_UUID + " TEXT" + "," +
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
                COLUMN_ID + COLUMN_ID_TYPE + "," +
                COLUMN_UUID + " TEXT" + "," +
                COLUMN_CONFIG_NAME + " TEXT UNIQUE NOT NULL" + "," +

                // this needs to be a look up table
                COLUMN_CONFIG_DATE_FORMAT_INDEX + " INTEGER" + "," +
                COLUMN_CONFIG_TIME_FORMAT_INDEX + " INTEGER" + "," +
                COLUMN_CONFIG_DISTANCE_FORMAT_INDEX + " INTEGER" + "," +

                COLUMN_CONFIG_MIN_GPS_PRECISION + " INTEGER" +
                ")")
        db.execSQL(createTableConfig)

        val createTableStudy = ("CREATE TABLE " +
                TABLE_STUDY + "(" +
                COLUMN_ID + COLUMN_ID_TYPE + "," +
                COLUMN_UUID + " TEXT" + "," +
                COLUMN_STUDY_NAME + " TEXT" + "," +

                COLUMN_CONFIG_ID + " INTEGER" + "," +
                // REMOVE THIS
                COLUMN_STUDY_CONFIG_UUID + " TEXT" + "," +

                // this needs to be a look up table
                COLUMN_STUDY_SAMPLING_METHOD_INDEX + " INTEGER" + "," +
                COLUMN_STUDY_SAMPLE_SIZE + " INTEGER" + "," +

                // this needs to be a look up table
                COLUMN_STUDY_SAMPLE_SIZE_INDEX + " INTEGER" + "," +
                "FOREIGN KEY($COLUMN_CONFIG_ID) REFERENCES $TABLE_CONFIG($COLUMN_ID)" +
                ")")
        db.execSQL(createTableStudy)

        val createTableConfigStudy = ("CREATE TABLE " +
                TABLE_CONFIG_STUDY + "(" +
                COLUMN_ID + COLUMN_ID_TYPE + "," +
                COLUMN_CONFIG_ID + " INTEGER NOT NULL" + "," +
                COLUMN_STUDY_ID + " INTEGER NOT NULL" + "," +
                "FOREIGN KEY($COLUMN_CONFIG_ID) REFERENCES $TABLE_CONFIG($COLUMN_ID)" + "," +
                "FOREIGN KEY($COLUMN_STUDY_ID) REFERENCES $TABLE_STUDY($COLUMN_ID)" +
                ")")
        db.execSQL(createTableConfigStudy)

        val createTableField = ("CREATE TABLE " +
                TABLE_FIELD + "(" +
                COLUMN_ID + COLUMN_ID_TYPE + "," +
                COLUMN_UUID + " TEXT" + "," +
                COLUMN_FIELD_NAME + " TEXT" + "," +

                COLUMN_STUDY_ID + " INTEGER" + "," +

                // should be look up table
                COLUMN_FIELD_TYPE_INDEX + " INTEGER" + "," +
                COLUMN_FIELD_PII + " BOOLEAN" + "," +
                COLUMN_FIELD_REQUIRED + " BOOLEAN" + "," +
                COLUMN_FIELD_INTEGER_ONLY + " BOOLEAN" + "," +
                COLUMN_FIELD_DATE + " BOOLEAN" + "," +
                COLUMN_FIELD_TIME + " BOOLEAN" + "," +
                COLUMN_FIELD_OPTION_1 + " TEXT" + "," +
                COLUMN_FIELD_OPTION_2 + " TEXT" + "," +
                COLUMN_FIELD_OPTION_3 + " TEXT" + "," +
                COLUMN_FIELD_OPTION_4 + " TEXT" + "," +
                "FOREIGN KEY($COLUMN_STUDY_ID) REFERENCES $TABLE_STUDY($COLUMN_ID)" +
                ")")
        db.execSQL(createTableField)

        val createTableRule = ("CREATE TABLE " +
                TABLE_RULE + "(" +
                COLUMN_ID + COLUMN_ID_TYPE + "," +
                COLUMN_UUID + " TEXT" + "," +

                // i think we can just use one key here.  if a field is connected to a study and
                // a rule is connected to a field
                // this needs to be a foreign key
                COLUMN_FIELD_ID + " INTEGER" + "," +
                COLUMN_RULE_NAME + " TEXT" + "," +

                // TODO:  this should be a look up table
                COLUMN_OPERATOR_ID + " INTEGER" + "," +
                COLUMN_RULE_VALUE + " TEXT" + "," +
                "FOREIGN KEY($COLUMN_FIELD_ID) REFERENCES $TABLE_FIELD($COLUMN_ID)" +
                ")")
        db.execSQL(createTableRule)

        val createTableFilter = ("CREATE TABLE " +
                TABLE_FILTER + "(" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT " + "," +
                COLUMN_UUID + " TEXT" + "," +
                // this needs to be a foreign key
                COLUMN_FILTER_STUDY_UUID + " TEXT" + "," +
                COLUMN_FILTER_NAME + " TEXT" + "," +
                COLUMN_FILTER_SAMPLE_SIZE + " INTEGER" + "," +
                COLUMN_FILTER_SAMPLE_SIZE_INDEX + " INTEGER" +
                ")")
        db.execSQL(createTableFilter)

        // connector table
        // this is a logic chain
        val createTableFilterRule = ("CREATE TABLE " +
                TABLE_FILTERRULE + "(" +
                COLUMN_UUID + " TEXT" + "," +
                COLUMN_FILTERRULE_STUDY_UUID + " TEXT" + "," +
                COLUMN_FILTER_ID + " INTEGER" + "," +
                COLUMN_RULE_ID + " INTEGER" + "," +
                COLUMN_FILTERRULE_CONNECTOR_INDEX + " INTEGER" + "," +
                "FOREIGN KEY($COLUMN_FILTER_ID) REFERENCES $TABLE_FILTER($COLUMN_ID)" + "," +
                "FOREIGN KEY($COLUMN_RULE_ID) REFERENCES $TABLE_RULE($COLUMN_ID)" +
                ")")
        db.execSQL(createTableFilterRule)

        val createTableSample = ("CREATE TABLE " +
                TABLE_SAMPLE + "(" +
                COLUMN_ID + COLUMN_ID_TYPE + "," +
                COLUMN_UUID + " TEXT" + "," +

                COLUMN_STUDY_ID + " INTEGER" + "," +
                // this needs to be a foreign key
                COLUMN_SAMPLE_STUDY_UUID + " TEXT" + "," +

                COLUMN_SAMPLE_NAME + " TEXT" + "," +
                COLUMN_SAMPLE_NUM_ENUMERATORS + " INTEGER" + "," +
                "FOREIGN KEY($COLUMN_STUDY_ID) REFERENCES $TABLE_STUDY($COLUMN_ID)" +
                ")")
        db.execSQL(createTableSample)

        val createTableEnumArea = ("CREATE TABLE " +
                TABLE_ENUM_AREA + "(" +
                COLUMN_ID + COLUMN_ID_TYPE + "," +
                COLUMN_CONFIG_ID + " INTEGER" + "," +
                COLUMN_ENUM_AREA_NAME + " TEXT" + "," +
                COLUMN_ENUM_AREA_TL_LAT + " REAL" + "," +
                COLUMN_ENUM_AREA_TL_LON + " REAL" + "," +
                COLUMN_ENUM_AREA_TR_LAT + " REAL" + "," +
                COLUMN_ENUM_AREA_TR_LON + " REAL" + "," +
                COLUMN_ENUM_AREA_BR_LAT + " REAL" + "," +
                COLUMN_ENUM_AREA_BR_LON + " REAL" + "," +
                COLUMN_ENUM_AREA_BL_LAT + " REAL" + "," +
                COLUMN_ENUM_AREA_BL_LON + " REAL" + "," +
                "FOREIGN KEY($COLUMN_CONFIG_ID) REFERENCES $TABLE_CONFIG($COLUMN_ID)" +
                ")")
        db.execSQL(createTableEnumArea)

        val createTableTeam = ("CREATE TABLE " +
                TABLE_TEAM + "(" +
                COLUMN_ID + COLUMN_ID_TYPE + "," +
                COLUMN_ENUM_AREA_ID + " INTEGER" + "," +
                COLUMN_TEAM_NAME + " TEXT" + "," +
                "FOREIGN KEY($COLUMN_ENUM_AREA_ID) REFERENCES $TABLE_ENUM_AREA($COLUMN_ID)" +
                ")")
        db.execSQL(createTableTeam)

        val createTableTeamMember = ("CREATE TABLE " +
                TABLE_TEAM_MEMBER + "(" +
                COLUMN_ID + COLUMN_ID_TYPE + "," +
                COLUMN_TEAM_ID + " INTEGER" + "," +
                COLUMN_TEAM_MEMBER_NAME + " TEXT" + "," +
                "FOREIGN KEY($COLUMN_TEAM_ID) REFERENCES $TABLE_TEAM($COLUMN_ID)" +
                ")")
        db.execSQL(createTableTeamMember)
    }

    //--------------------------------------------------------------------------
    override fun onUpgrade( db: SQLiteDatabase, oldVersion: Int, newVersion: Int )
    {
        // clear all tables
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONFIG")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STUDY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONFIG_STUDY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FIELD")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RULE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FILTER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FILTERRULE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAMPLE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAV_PLAN")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ENUM_AREA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TEAM")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TEAM_MEMBER")

        onCreate(db)
    }

    //--------------------------------------------------------------------------
    companion object
    {
        private const val DATABASE_NAME = "GPSSampleDB.db"

        const val COLUMN_ID = "id"
        const val COLUMN_UUID = "uuid"
        const val COLUMN_ID_TYPE = " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"
        // foreign key columns
        const val COLUMN_CONFIG_ID = "config_id"
        const val COLUMN_STUDY_ID = "study_id"
        const val COLUMN_FIELD_ID = "field_id"
        const val COLUMN_RULE_ID = "rule_id"
        const val COLUMN_FILTER_ID = "filter_id"
        const val COLUMN_ENUM_AREA_ID = "enum_area_id"
        const val COLUMN_TEAM_ID = "team_id"
        const val COLUMN_OPERATOR_ID = "operator_id"
        // Connector Tables
        const val TABLE_CONFIG_STUDY = "config_study_conn"

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
        const val COLUMN_CONFIG_DISTANCE_FORMAT_INDEX = "config_distance_format_index"
        const val COLUMN_CONFIG_DATE_FORMAT_INDEX = "config_date_format_index"
        const val COLUMN_CONFIG_TIME_FORMAT_INDEX = "config_time_format_index"
        const val COLUMN_CONFIG_MIN_GPS_PRECISION = "config_min_gps_precision"

        // Study Table
        const val TABLE_STUDY = "study"
        const val COLUMN_STUDY_NAME = "study_name"

        const val COLUMN_STUDY_CONFIG_UUID = "study_config_id"
        const val COLUMN_STUDY_SAMPLING_METHOD_INDEX = "study_sampling_method_index"
        const val COLUMN_STUDY_SAMPLE_SIZE = "study_sample_size"
        const val COLUMN_STUDY_SAMPLE_SIZE_INDEX = "study_sample_size_index"

        // Field Table
        const val TABLE_FIELD = "field"
        const val COLUMN_FIELD_NAME = "field_name"
        const val COLUMN_FIELD_STUDY_UUID = "field_study_id"
        const val COLUMN_FIELD_TYPE_INDEX = "field_type_index"
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
        const val COLUMN_FILTERRULE_CONNECTOR_INDEX = "filterrule_connector_index"

        // Sample Table
        const val TABLE_SAMPLE = "sample"
        const val COLUMN_SAMPLE_STUDY_UUID = "sample_study_id"
        const val COLUMN_SAMPLE_NAME = "sample_name"
        const val COLUMN_SAMPLE_NUM_ENUMERATORS = "sample_num_enumerators"

        // NavigationPlan Table
        const val TABLE_NAV_PLAN = "nav_plan"
        const val COLUMN_NAV_PLAN_SAMPLE_UUID = "nav_plan_sample_id"
        const val COLUMN_NAV_PLAN_NAME = "nav_plan_name"

        // EnumArea Table
        const val TABLE_ENUM_AREA = "enum_area"
        const val COLUMN_ENUM_AREA_CONFIG_UUID = "enum_area_config_uuid"
        const val COLUMN_ENUM_AREA_NAME = "enum_area_name"
        const val COLUMN_ENUM_AREA_TL_LAT = "enum_area_tl_lat"
        const val COLUMN_ENUM_AREA_TL_LON = "enum_area_tl_lon"
        const val COLUMN_ENUM_AREA_TR_LAT = "enum_area_tr_lat"
        const val COLUMN_ENUM_AREA_TR_LON = "enum_area_tr_lon"
        const val COLUMN_ENUM_AREA_BR_LAT = "enum_area_br_lat"
        const val COLUMN_ENUM_AREA_BR_LON = "enum_area_br_lon"
        const val COLUMN_ENUM_AREA_BL_LAT = "enum_area_bl_lat"
        const val COLUMN_ENUM_AREA_BL_LON = "enum_area_bl_lon"

        // Team Table
        const val TABLE_TEAM = "team"
        const val COLUMN_TEAM_NAME = "team_name"

        // Team Member Table
        const val TABLE_TEAM_MEMBER = "team_member"
        const val COLUMN_TEAM_MEMBER_NAME = "team_member_name"

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
        lateinit var enumAreaDAO: EnumAreaDAO
        lateinit var teamDAO: TeamDAO
        lateinit var teamMemberDAO: TeamMemberDAO

        // creation/access methods

        private var instance: DAO? = null

        fun createSharedInstance( context: Context ): DAO
        {
            if (instance == null)
            {
                instance = DAO( context, null, null, DATABASE_VERSION )

                userDAO = UserDAO( instance!! )
                configDAO = ConfigDAO( instance!! )
                studyDAO = StudyDAO( instance!! )
                fieldDAO = FieldDAO( instance!! )
                ruleDAO = RuleDAO( instance!! )
                filterDAO = FilterDAO( instance!! )
                filterRuleDAO = FilterRuleDAO( instance!! )
                sampleDAO = SampleDAO( instance!! )
                navPlanDAO = NavPlanDAO( instance!! )
                enumAreaDAO = EnumAreaDAO( instance!! )
                teamDAO = TeamDAO( instance!! )
                teamMemberDAO = TeamMemberDAO( instance!! )
            }

            return instance!!
        }

        private const val DATABASE_VERSION = 76
    }
}