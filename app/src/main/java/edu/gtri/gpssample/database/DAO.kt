package edu.gtri.gpssample.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DAO(private var context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int )
    : SQLiteOpenHelper( context, DATABASE_NAME, factory, DATABASE_VERSION )
{
    override fun onCreate( db: SQLiteDatabase )
    {
        try {
            val createTableUser = ("CREATE TABLE " +
                    TABLE_USER + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_USER_ROLE + " TEXT" + "," +
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
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_CONFIG_NAME + " TEXT UNIQUE NOT NULL" + "," +

                    // this needs to be a look up table
                    COLUMN_CONFIG_DATE_FORMAT_INDEX + " INTEGER" + "," +
                    COLUMN_CONFIG_TIME_FORMAT_INDEX + " INTEGER" + "," +
                    COLUMN_CONFIG_DISTANCE_FORMAT_INDEX + " INTEGER" + "," +
                    COLUMN_CONFIG_MIN_GPS_PRECISION + " INTEGER" + "," +
                    COLUMN_ENUM_AREA_ID + " INTEGER" + "," +
                    COLUMN_STUDY_ID + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_ID) REFERENCES $TABLE_ENUM_AREA($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_STUDY_ID) REFERENCES $TABLE_STUDY($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableConfig)

            val createTableStudy = ("CREATE TABLE " +
                    TABLE_STUDY + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_STUDY_NAME + " TEXT" + "," +
                    COLUMN_CONFIG_ID + " INTEGER" + "," +

                    // this needs to be a look up table
                    COLUMN_STUDY_SAMPLING_METHOD_INDEX + " INTEGER" + "," +
                    COLUMN_STUDY_SAMPLE_SIZE + " INTEGER" + "," +

                    // this needs to be a look up table
                    COLUMN_STUDY_SAMPLE_SIZE_INDEX + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_CONFIG_ID) REFERENCES $TABLE_CONFIG($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableStudy)

            val createTableStudyEnumArea = ("CREATE TABLE " +
                    TABLE_STUDY_ENUM_AREA + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_STUDY_ID + " INTEGER NOT NULL" + "," +
                    COLUMN_ENUM_AREA_ID + " INTEGER NOT NULL" + "," +
                    "FOREIGN KEY($COLUMN_STUDY_ID) REFERENCES $TABLE_STUDY($COLUMN_ID)" +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_ID) REFERENCES $TABLE_ENUM_AREA($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableStudyEnumArea)

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
                    COLUMN_STUDY_ID + " INTEGER" + "," +
                    COLUMN_FIELD_NAME + " TEXT" + "," +
                    COLUMN_FIELD_BLOCK_CONTAINER + " INTEGER" + "," +
                    COLUMN_FIELD_BLOCK_UUID + " TEXT" + "," +

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

            val createTableFieldOption = ("CREATE TABLE " +
                    TABLE_FIELD_OPTION + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_FIELD_OPTION_NAME + " TEXT" +
                    ")")
            db.execSQL(createTableFieldOption)

            val createTableField__FieldOption = ("CREATE TABLE " +
                    TABLE_FIELD__FIELD_OPTION + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_FIELD_ID + " INTEGER" + "," +
                    COLUMN_FIELD_OPTION_ID + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_ID) REFERENCES $TABLE_FIELD($COLUMN_ID)" +
                    "FOREIGN KEY($COLUMN_FIELD_OPTION_ID) REFERENCES $TABLE_FIELD_OPTION($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableField__FieldOption)

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
                    COLUMN_FILTEROPERATOR_ID + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_FILTEROPERATOR_ID) REFERENCES $TABLE_FILTEROPERATOR($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_ID) REFERENCES $TABLE_FIELD($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableRule)

            val createTableFilter = ("CREATE TABLE " +
                    TABLE_FILTER + "(" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT " + "," +
                    COLUMN_STUDY_ID + " INTEGER" + "," +
                    COLUMN_FILTER_NAME + " TEXT" + "," +
                    COLUMN_FILTER_SAMPLE_SIZE + " INTEGER" + "," +
                    COLUMN_FILTER_SAMPLE_TYPE_INDEX + " INTEGER" + "," +
                    COLUMN_RULE_ID + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_STUDY_ID) REFERENCES $TABLE_STUDY($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_RULE_ID) REFERENCES $TABLE_RULE($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableFilter)

            val createTableFilterOperator = ("CREATE TABLE " +
                    TABLE_FILTEROPERATOR + " (" +
                    COLUMN_ID + " INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT " + "," +
                    COLUMN_CONNECTOR +  " INTEGER NOT NULL" + "," +
                    COLUMN_RULE_ID +  " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_RULE_ID) REFERENCES $TABLE_RULE($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableFilterOperator)

            val createTableEnumArea = ("CREATE TABLE " +
                    TABLE_ENUM_AREA + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_CONFIG_ID + " INTEGER" + "," +
                    COLUMN_ENUM_AREA_NAME + " TEXT" + "," +
                    COLUMN_TEAM_ID + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_CONFIG_ID) REFERENCES $TABLE_CONFIG($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_TEAM_ID) REFERENCES $TABLE_TEAM($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableEnumArea)

            val createTableSampleArea = ("CREATE TABLE " +
                    TABLE_SAMPLE_AREA + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_STUDY_ID + " INTEGER" + "," +
                    COLUMN_TEAM_ID + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_STUDY_ID) REFERENCES $TABLE_STUDY($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_TEAM_ID) REFERENCES $TABLE_TEAM($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableSampleArea)

            val createTableTeam = ("CREATE TABLE " +
                    TABLE_TEAM + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_STUDY_ID + " INTEGER" + "," +
                    COLUMN_ENUM_AREA_ID + " INTEGER" + "," +
                    COLUMN_SAMPLE_AREA_ID + " INTEGER" + "," +
                    COLUMN_TEAM_NAME + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_STUDY_ID) REFERENCES $TABLE_STUDY($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_ID) REFERENCES $TABLE_ENUM_AREA($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_SAMPLE_AREA_ID) REFERENCES $TABLE_SAMPLE_AREA($COLUMN_ID)" +
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

            val createTableLocation = ("CREATE TABLE " +
                    TABLE_LOCATION + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_UUID + " TEXT" + "," +
                    COLUMN_LOCATION_TYPE_ID + " INTEGER" + "," +
                    COLUMN_LOCATION_LATITUDE + " REAL" + "," +
                    COLUMN_LOCATION_LONGITUDE + " REAL" + "," +
                    COLUMN_LOCATION_IS_LANDMARK + " INTEGER" + "," +
                    COLUMN_LOCATION_DESCRIPTION + " TEXT" +
                    ")")
            db.execSQL(createTableLocation)

            val createTableLocationEnumArea = ("CREATE TABLE " +
                    TABLE_LOCATION__ENUM_AREA + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_LOCATION_ID + " INTEGER" + "," +
                    COLUMN_ENUM_AREA_ID + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_LOCATION_ID) REFERENCES $TABLE_LOCATION($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_ID) REFERENCES $TABLE_ENUM_AREA($COLUMN_ID)" + "," +
                    "UNIQUE ($COLUMN_LOCATION_ID, $COLUMN_ENUM_AREA_ID)" +
                    ")")
            db.execSQL(createTableLocationEnumArea)

            val createTableLocationSampleArea = ("CREATE TABLE " +
                    TABLE_LOCATION__SAMPLE_AREA + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_LOCATION_ID + " INTEGER" + "," +
                    COLUMN_SAMPLE_AREA_ID + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_LOCATION_ID) REFERENCES $TABLE_LOCATION($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_SAMPLE_AREA_ID) REFERENCES $TABLE_SAMPLE_AREA($COLUMN_ID)" + "," +
                    "UNIQUE ($COLUMN_LOCATION_ID, $COLUMN_SAMPLE_AREA_ID)" +
                    ")")
            db.execSQL(createTableLocationSampleArea)

            val createTableEnumerationItem = ("CREATE TABLE " +
                    TABLE_ENUMERATION_ITEM + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_UUID + " TEXT" + "," +
                    COLUMN_LOCATION_ID + " INTEGER" + "," +
                    COLUMN_ENUMERATION_ITEM_SUB_ADDRESS + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_SAMPLING_STATE + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_COLLECTION_STATE + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_INCOMPLETE_REASON + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_NOTES + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_LOCATION_ID) REFERENCES $TABLE_LOCATION($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableEnumerationItem)

            val createTableFieldData = ("CREATE TABLE " +
                    TABLE_FIELD_DATA + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_UUID + " TEXT" + "," +
                    COLUMN_FIELD_ID + " INTEGER" + "," +
                    COLUMN_ENUMERATION_ITEM_ID + " INTEGER" + "," +
                    COLUMN_FIELD_NAME + " TEXT" + "," +
                    COLUMN_FIELD_TYPE_INDEX + " INTEGER" + "," +
                    COLUMN_FIELD_DATA_TEXT_VALUE + " TEXT" + "," +
                    COLUMN_FIELD_DATA_NUMBER_VALUE + " REAL" + "," +
                    COLUMN_FIELD_DATA_DATE_VALUE + " INTEGER" + "," +
                    COLUMN_FIELD_DATA_DROPDOWN_INDEX + " INTEGER" + "," +
                    COLUMN_FIELD_DATA_BLOCK_NUMBER + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_ID) REFERENCES $TABLE_FIELD($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_ENUMERATION_ITEM_ID) REFERENCES $TABLE_ENUMERATION_ITEM($COLUMN_ID)" +
                    ")")
            db.execSQL(createTableFieldData)

            val createTableFieldDataOption = ("CREATE TABLE " +
                    TABLE_FIELD_DATA_OPTION + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_FIELD_DATA_OPTION_NAME + " TEXT" + "," +
                    COLUMN_FIELD_DATA_OPTION_VALUE + " INTEGER" +
                    ")")
            db.execSQL(createTableFieldDataOption)

            val createTableFieldData__FieldDataOption = ("CREATE TABLE " +
                    TABLE_FIELD_DATA__FIELD_DATA_OPTION + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_FIELD_DATA_ID + " INTEGER" + "," +
                    COLUMN_FIELD_DATA_OPTION_ID + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_DATA_ID) REFERENCES $TABLE_FIELD_DATA($COLUMN_ID)" +
                    "FOREIGN KEY($COLUMN_FIELD_DATA_OPTION_ID) REFERENCES $TABLE_FIELD_DATA_OPTION($COLUMN_ID)" +
                    ")")
            val x = db.execSQL(createTableFieldData__FieldDataOption)

            val createTableLatLon = ("CREATE TABLE " +
                    TABLE_LAT_LON + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_LAT + " REAL" + "," +
                    COLUMN_LON + " REAL" +
                    ")")
            val y = db.execSQL(createTableLatLon)

            val createTableEnumAreaLatLon = ("CREATE TABLE " +
                    TABLE_ENUM_AREA_LAT_LON + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_ENUM_AREA_ID + " INTEGER " + "," +
                    COLUMN_LAT_LON_ID + " INTEGER " + "," +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_ID) REFERENCES $TABLE_ENUM_AREA($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_LAT_LON_ID) REFERENCES $TABLE_LAT_LON($COLUMN_ID)" + "," +
                    "UNIQUE ($COLUMN_ENUM_AREA_ID, $COLUMN_LAT_LON_ID)" +
                    ")")
            db.execSQL(createTableEnumAreaLatLon)

            val createTableSampleAreaLatLon = ("CREATE TABLE " +
                    TABLE_SAMPLE_AREA_LAT_LON + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_SAMPLE_AREA_ID + " INTEGER " + "," +
                    COLUMN_LAT_LON_ID + " INTEGER " + "," +
                    "FOREIGN KEY($COLUMN_SAMPLE_AREA_ID) REFERENCES $TABLE_SAMPLE_AREA($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_LAT_LON_ID) REFERENCES $TABLE_LAT_LON($COLUMN_ID)" + "," +
                    "UNIQUE ($COLUMN_SAMPLE_AREA_ID, $COLUMN_LAT_LON_ID)" +
                    ")")
            db.execSQL(createTableSampleAreaLatLon)

            val createTableTeamLatLon = ("CREATE TABLE " +
                    TABLE_TEAM_LAT_LON + "(" +
                    COLUMN_ID + COLUMN_ID_TYPE + "," +
                    COLUMN_TEAM_ID + " INTEGER " + "," +
                    COLUMN_LAT_LON_ID + " INTEGER " + "," +
                    "FOREIGN KEY($COLUMN_TEAM_ID) REFERENCES $TABLE_TEAM($COLUMN_ID)" + "," +
                    "FOREIGN KEY($COLUMN_LAT_LON_ID) REFERENCES $TABLE_LAT_LON($COLUMN_ID)" + "," +
                    "UNIQUE ($COLUMN_TEAM_ID, $COLUMN_LAT_LON_ID)" +
                    ")")
            db.execSQL(createTableTeamLatLon)

        }catch(ex: Exception)
        {
            Log.d("xxx", "the problem ${ex.toString()}")
        }
    }

    override fun onUpgrade( db: SQLiteDatabase, oldVersion: Int, newVersion: Int )
    {
        // clear all tables
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONFIG")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STUDY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CONFIG_STUDY")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_STUDY_ENUM_AREA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FIELD")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_RULE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FILTER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FILTERRULE")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FILTEROPERATOR")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ENUM_AREA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAMPLE_AREA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TEAM")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TEAM_MEMBER")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FIELD_DATA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LAT_LON")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ENUMERATION_ITEM")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SAMPLE_AREA_LAT_LON")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ENUM_AREA_LAT_LON")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TEAM_LAT_LON")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_FIELD_OPTION")

        db.execSQL("DROP TABLE IF EXISTS $TABLE_FIELD__FIELD_OPTION")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATION__ENUM_AREA")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATION__SAMPLE_AREA")

        onCreate(db)
    }

    companion object
    {
        private const val DATABASE_NAME = "GPSSampleDB.db"

        const val COLUMN_ID = "id"
        const val COLUMN_ID_TYPE = " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"

        const val COLUMN_UUID = "uuid"
        const val COLUMN_CREATION_DATE = "creation_date"

        // foreign key columns
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_CONFIG_ID = "config_id"
        const val COLUMN_STUDY_ID = "study_id"
        const val COLUMN_FIELD_ID = "field_id"
        const val COLUMN_FIELD_OPTION_ID = "field_option_id"
        const val COLUMN_FIELD_DATA_ID = "field_data_id"
        const val COLUMN_FIELD_DATA_OPTION_ID = "field_data_option_id"
        const val COLUMN_RULE_ID = "rule_id"
        const val COLUMN_FILTER_ID = "filter_id"
        const val COLUMN_ENUM_AREA_ID = "enum_area_id"
        const val COLUMN_SAMPLE_AREA_ID = "sample_area_id"
        const val COLUMN_TEAM_ID = "team_id"
        const val COLUMN_OPERATOR_ID = "operator_id"
        const val COLUMN_LOCATION_ID = "location_id"
        const val COLUMN_ENUMERATION_ITEM_ID = "enumeration_item_id"
        const val COLUMN_LAT_LON_ID = "lat_lon_id"

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

        // Study EnumArea connector table
        const val TABLE_STUDY_ENUM_AREA = "study_enum_area"
        // columns are defined in foreign key area

        // Study Table
        const val TABLE_STUDY = "study"
        const val COLUMN_STUDY_NAME = "study_name"
        const val COLUMN_STUDY_SAMPLING_METHOD_INDEX = "study_sampling_method_index"
        const val COLUMN_STUDY_SAMPLE_SIZE = "study_sample_size"
        const val COLUMN_STUDY_SAMPLE_SIZE_INDEX = "study_sample_size_index"

        // Field Table
        const val TABLE_FIELD = "field"
        const val COLUMN_FIELD_NAME = "field_name"
        const val COLUMN_FIELD_BLOCK_CONTAINER = "field_block_container"
        const val COLUMN_FIELD_BLOCK_UUID = "field_block_uuid"
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

        const val TABLE_FIELD_OPTION = "field_option"
        const val COLUMN_FIELD_OPTION_NAME = "field_option_name"

        // connector table, field to field_option
        const val TABLE_FIELD__FIELD_OPTION = "field__field_option"

        // Rule Table
        const val TABLE_RULE = "rule"
        const val COLUMN_RULE_NAME = "rule_name"
        const val COLUMN_RULE_VALUE = "rule_value"
        const val COLUMN_FILTEROPERATOR_ID = "filter_operator_id"
        // Filter Table
        const val TABLE_FILTER = "filter"
        const val COLUMN_FILTER_NAME = "filter_name"
        const val COLUMN_FILTER_SAMPLE_SIZE = "filter_sample_size"
        const val COLUMN_FILTER_SAMPLE_TYPE_INDEX = "filter_sample_type_index"

        // FilterRule Table
        const val TABLE_FILTERRULE = "filterrule"
        const val COLUMN_FILTERRULE_ORDER = "filterrule_order"
        const val COLUMN_FILTERRULE_CONNECTOR_INDEX = "filterrule_connector_index"

        // FilterOperator
        const val TABLE_FILTEROPERATOR = "filteroperator"
        const val COLUMN_CONNECTOR = "connector"

        // EnumArea Table
        const val TABLE_ENUM_AREA = "enum_area"
        const val COLUMN_ENUM_AREA_NAME = "enum_area_name"

        const val TABLE_SAMPLE_AREA = "sample_area"

        // Team Table
        const val TABLE_TEAM = "team"
        const val COLUMN_TEAM_NAME = "team_name"

        // Team Member Table
        const val TABLE_TEAM_MEMBER = "team_member"
        const val COLUMN_TEAM_MEMBER_NAME = "team_member_name"

        // Location Table
        const val TABLE_LOCATION = "location"
        const val COLUMN_LOCATION_TYPE_ID = "location_type_id"
        const val COLUMN_LOCATION_LATITUDE = "location_latitude"
        const val COLUMN_LOCATION_LONGITUDE = "location_longitude"
        const val COLUMN_LOCATION_IS_LANDMARK = "location_is_landmark"
        const val COLUMN_LOCATION_DESCRIPTION = "location_description"

        // connector table, location to EnumArea
        const val TABLE_LOCATION__ENUM_AREA = "location__enum_area"

        // connector table, location to SampleArea
        const val TABLE_LOCATION__SAMPLE_AREA = "location__sample_area"

        // EnumerationItem Table
        const val TABLE_ENUMERATION_ITEM = "enumeration_item"
        const val COLUMN_ENUMERATION_ITEM_SUB_ADDRESS = "enumeration_item_sub_address"
        const val COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE = "enumeration_item_enumeration_state"
        const val COLUMN_ENUMERATION_ITEM_SAMPLING_STATE = "enumeration_item_sampling_state"
        const val COLUMN_ENUMERATION_ITEM_COLLECTION_STATE = "enumeration_item_collection_state"
        const val COLUMN_ENUMERATION_ITEM_INCOMPLETE_REASON = "enumeration_item_incomplete_reason"
        const val COLUMN_ENUMERATION_ITEM_NOTES = "enumeration_item_notes"

        const val TABLE_FIELD_DATA = "field_data"
        const val COLUMN_FIELD_DATA_TEXT_VALUE = "field_data_text_value"
        const val COLUMN_FIELD_DATA_NUMBER_VALUE = "field_data_number_value"
        const val COLUMN_FIELD_DATA_DATE_VALUE = "field_data_date_value"
        const val COLUMN_FIELD_DATA_DROPDOWN_INDEX = "field_data_dropdown_index"
        const val COLUMN_FIELD_DATA_BLOCK_NUMBER = "field_data_block_number"

        const val TABLE_FIELD_DATA_OPTION = "field_data_option"
        const val COLUMN_FIELD_DATA_OPTION_NAME = "field_data_option_name"
        const val COLUMN_FIELD_DATA_OPTION_VALUE = "field_data_option_value"

        // connector table, fieldData to fieldDataOption
        const val TABLE_FIELD_DATA__FIELD_DATA_OPTION = "field_data__field_data_option"

        const val TABLE_LAT_LON = "lat_lon"
        const val COLUMN_LAT = "lat"
        const val COLUMN_LON = "lon"

        // connector tables for lat lon
        const val TABLE_ENUM_AREA_LAT_LON = "enum_area_lat_lon"
        const val TABLE_SAMPLE_AREA_LAT_LON = "sample_area_lat_lon"
        const val TABLE_TEAM_LAT_LON = "team_lat_lon"
        // DAO's
        lateinit var userDAO: UserDAO
        lateinit var configDAO: ConfigDAO
        lateinit var studyDAO: StudyDAO
        lateinit var fieldDAO: FieldDAO
        lateinit var fieldOptionDAO: FieldOptionDAO
        lateinit var ruleDAO: RuleDAO
        lateinit var filterDAO: FilterDAO
        //lateinit var filterRuleDAO: FilterRuleDAO
        lateinit var enumAreaDAO: EnumAreaDAO
        lateinit var sampleAreaDAO: SampleAreaDAO
        lateinit var teamDAO: TeamDAO
        lateinit var teamMemberDAO: TeamMemberDAO
        lateinit var fieldDataDAO: FieldDataDAO
        lateinit var fieldDataOptionDAO: FieldDataOptionDAO
        lateinit var latLonDAO: LatLonDAO
        lateinit var locationDAO: LocationDAO
        lateinit var enumerationItemDAO: EnumerationItemDAO

        // creation/access methods

        private var instance: DAO? = null

        fun showAll()
        {
//            deleteAll()
            Log.d( "xxx", "configs: ${DAO.configDAO.getConfigs()}")
            Log.d( "xxx", "studies: ${DAO.studyDAO.getStudies()}")
            Log.d( "xxx", "fields: ${DAO.fieldDAO.getFields()}")
            Log.d( "xxx", "rules: ${DAO.ruleDAO.getRules()}")
            Log.d( "xxx", "filters: ${DAO.filterDAO.getFilters()}")
            Log.d( "xxx", "fieldData: ${DAO.fieldDataDAO.getFieldData()}")
            Log.d( "xxx", "enumAreas: ${DAO.enumAreaDAO.getEnumAreas()}")
            Log.d( "xxx", "teams: ${DAO.teamDAO.getTeams()}")
            Log.d( "xxx", "latLons: ${DAO.latLonDAO.getLatLons()}")
            Log.d( "xxx", "locations: ${DAO.locationDAO.getLocations()}")
            Log.d( "xxx", "enumerationItems: ${DAO.enumerationItemDAO.getEnumerationItems()}")
        }

        fun deleteAll()
        {
            instance?.let {
                val db = it.writableDatabase
                db.delete(TABLE_CONFIG, null, null )
                db.delete(TABLE_STUDY, null, null)
                db.delete(TABLE_CONFIG_STUDY, null, null)
                db.delete(TABLE_STUDY_ENUM_AREA, null, null)
                db.delete(TABLE_FIELD, null, null)
                db.delete(TABLE_FIELD_DATA, null, null)
                db.delete(TABLE_RULE, null, null)
                db.delete(TABLE_FILTER, null, null)
                db.delete(TABLE_ENUM_AREA, null, null)
                db.delete(TABLE_SAMPLE_AREA, null, null)
                db.delete(TABLE_TEAM, null, null)
                db.delete(TABLE_TEAM_MEMBER, null, null)
                db.delete(TABLE_LAT_LON, null, null)
                db.delete(TABLE_LOCATION, null, null)
                db.delete(TABLE_ENUMERATION_ITEM, null, null)
                db.delete(TABLE_ENUM_AREA_LAT_LON, null, null)
                db.delete(TABLE_SAMPLE_AREA_LAT_LON, null, null)
            }
        }

        fun createSharedInstance( context: Context ): DAO
        {
            if (instance == null)
            {
                instance = DAO( context, null, null, DATABASE_VERSION )

                userDAO = UserDAO( instance!! )
                configDAO = ConfigDAO( instance!! )
                studyDAO = StudyDAO( instance!! )
                fieldDAO = FieldDAO( instance!! )
                fieldOptionDAO = FieldOptionDAO( instance!! )
                ruleDAO = RuleDAO( instance!! )
                filterDAO = FilterDAO( instance!! )
              //  filterRuleDAO = FilterRuleDAO( instance!! )
                enumAreaDAO = EnumAreaDAO( instance!! )
                sampleAreaDAO = SampleAreaDAO( instance!! )
                teamDAO = TeamDAO( instance!! )
                teamMemberDAO = TeamMemberDAO( instance!! )
                fieldDataDAO = FieldDataDAO( instance!! )
                fieldDataOptionDAO = FieldDataOptionDAO( instance!! )
                latLonDAO = LatLonDAO( instance!!)
                locationDAO = LocationDAO( instance!!)
                enumerationItemDAO = EnumerationItemDAO( instance!!)
            }

            return instance!!
        }

        private const val DATABASE_VERSION = 234
    }
}