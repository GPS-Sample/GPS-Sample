/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.database.models.Image
import java.util.*

class DAO(private var context: Context, name: String?, factory: SQLiteDatabase.CursorFactory?, version: Int )
    : SQLiteOpenHelper( context, DATABASE_NAME, factory, DATABASE_VERSION )
{
    override fun onCreate( db: SQLiteDatabase )
    {
        try {
            val createTableUser = ("CREATE TABLE " +
                    TABLE_USER + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_USER_ROLE + " TEXT" + "," +
                    COLUMN_USER_NAME + " TEXT" + "," +
                    COLUMN_USER_RECOVERY_QUESTION + " TEXT" + "," +
                    COLUMN_USER_RECOVERY_ANSWER + " TEXT" + "," +
                    COLUMN_USER_IS_ONLINE + " BOOLEAN" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableUser)

            val createTableConfig = ("CREATE TABLE " +
                    TABLE_CONFIG + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_TIME_ZONE + " INTEGER" + "," +
                    COLUMN_CONFIG_NAME + " TEXT UNIQUE NOT NULL" + "," +
                    COLUMN_CONFIG_DB_VERSION + " INTEGER" + "," +
                    COLUMN_CONFIG_MAP_ENGINE_INDEX + " INTEGER" + "," +
                    COLUMN_CONFIG_DATE_FORMAT_INDEX + " INTEGER" + "," +
                    COLUMN_CONFIG_TIME_FORMAT_INDEX + " INTEGER" + "," +
                    COLUMN_CONFIG_DISTANCE_FORMAT_INDEX + " INTEGER" + "," +
                    COLUMN_CONFIG_MIN_GPS_PRECISION + " INTEGER" + "," +
                    COLUMN_CONFIG_ENCRYPTION_PASSWORD + " TEXT" + "," +
                    COLUMN_CONFIG_ALLOW_MANUAL_LOCATION_ENTRY + " INTEGER" + "," +
                    COLUMN_CONFIG_SUBADDRESS_IS_REQUIRED + " INTEGER" + "," +
                    COLUMN_CONFIG_AUTO_INCREMENT_SUBADDRESS + " INTEGER" + "," +
                    COLUMN_CONFIG_PROXIMITY_WARNING_IS_ENABLED + " INTEGER" + "," +
                    COLUMN_CONFIG_PROXIMITY_WARNING_VALUE + " INTEGER" + "," +
                    COLUMN_ENUM_AREA_UUID + " TEXT" + "," +
                    COLUMN_STUDY_UUID + " TEXT" + "," +
                    COLUMN_CONFIG_VALID_USERS + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_UUID) REFERENCES $TABLE_ENUM_AREA($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_STUDY_UUID) REFERENCES $TABLE_STUDY($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableConfig)

            val createTableEnumArea = ("CREATE TABLE " +
                    TABLE_ENUM_AREA + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CONFIG_UUID + " TEXT" + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_ENUM_AREA_NAME + " TEXT" + "," +
                    COLUMN_ENUM_AREA_MBTILESPATH + " TEXT" + "," +
                    COLUMN_ENUM_AREA_MBTILESSIZE + " INTEGER" + "," +
                    COLUMN_ENUMERATION_TEAM_UUID + " TEXT" + "," +
                    COLUMN_COLLECTION_TEAM_UUID + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_CONFIG_UUID) REFERENCES $TABLE_CONFIG($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_ENUMERATION_TEAM_UUID) REFERENCES $TABLE_ENUMERATION_TEAM($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_COLLECTION_TEAM_UUID) REFERENCES $TABLE_COLLECTION_TEAM($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableEnumArea)

            val createTableStudy = ("CREATE TABLE " +
                    TABLE_STUDY + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_STUDY_NAME + " TEXT" + "," +
                    COLUMN_STUDY_SAMPLING_METHOD_INDEX + " INTEGER" + "," +
                    COLUMN_STUDY_SAMPLE_SIZE + " INTEGER" + "," +
                    COLUMN_STUDY_SAMPLE_SIZE_INDEX + " INTEGER" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableStudy)

            val createConnectorTableConfigStudy = ("CREATE TABLE " +
                    CONNECTOR_TABLE_CONFIG__STUDY + "(" +
                    COLUMN_CID + COLUMN_CID_TYPE + "," +
                    COLUMN_CONFIG_UUID + " TEXT NOT NULL" + "," +
                    COLUMN_STUDY_UUID + " TEXT NOT NULL" + "," +
                    "FOREIGN KEY($COLUMN_CONFIG_UUID) REFERENCES $TABLE_CONFIG($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_STUDY_UUID) REFERENCES $TABLE_STUDY($COLUMN_UUID)" +
                    ")")
            db.execSQL(createConnectorTableConfigStudy)

            val createTableField = ("CREATE TABLE " +
                    TABLE_FIELD + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_STUDY_UUID + " TEXT" + "," +
                    COLUMN_FIELD_PARENT_UUID + " TEXT" + "," +
                    COLUMN_FIELD_INDEX + " INTEGER" + "," +
                    COLUMN_FIELD_NAME + " TEXT" + "," +

                    // should be look up table
                    COLUMN_FIELD_TYPE_INDEX + " INTEGER" + "," +
                    COLUMN_FIELD_PII + " BOOLEAN" + "," +
                    COLUMN_FIELD_REQUIRED + " BOOLEAN" + "," +
                    COLUMN_FIELD_INTEGER_ONLY + " BOOLEAN" + "," +
                    COLUMN_FIELD_NUMBER_OF_RESIDENTS + " BOOLEAN" + "," +
                    COLUMN_FIELD_DATE + " BOOLEAN" + "," +
                    COLUMN_FIELD_TIME + " BOOLEAN" + "," +
                    COLUMN_FIELD_MINIMUM + " REAL" + "," +
                    COLUMN_FIELD_MAXIMUM + " REAL" + "," +
                    COLUMN_FIELD_OPTION_1 + " TEXT" + "," +
                    COLUMN_FIELD_OPTION_2 + " TEXT" + "," +
                    COLUMN_FIELD_OPTION_3 + " TEXT" + "," +
                    COLUMN_FIELD_OPTION_4 + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_STUDY_UUID) REFERENCES $TABLE_STUDY($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableField)

            val createTableFieldOption = ("CREATE TABLE " +
                    TABLE_FIELD_OPTION + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_FIELD_OPTION_NAME + " TEXT" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableFieldOption)

            val createConnectorTableField__FieldOption = ("CREATE TABLE " +
                    CONNECTOR_TABLE_FIELD__FIELD_OPTION + "(" +
                    COLUMN_CID + COLUMN_CID_TYPE + "," +
                    COLUMN_FIELD_UUID + " TEXT" + "," +
                    COLUMN_FIELD_OPTION_UUID + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_UUID) REFERENCES $TABLE_FIELD($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_OPTION_UUID) REFERENCES $TABLE_FIELD_OPTION($COLUMN_UUID)" +
                    ")")
            db.execSQL(createConnectorTableField__FieldOption)

            val createTableRule = ("CREATE TABLE " +
                    TABLE_RULE + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_FIELD_UUID + " TEXT" + "," +
                    COLUMN_RULE_NAME + " TEXT" + "," +
                    COLUMN_OPERATOR_ID + " INTEGER" + "," +
                    COLUMN_RULE_VALUE + " TEXT" + "," +
                    COLUMN_FILTEROPERATOR_UUID + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_FILTEROPERATOR_UUID) REFERENCES $TABLE_FILTEROPERATOR($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_UUID) REFERENCES $TABLE_FIELD($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableRule)

            val createTableFilter = ("CREATE TABLE " +
                    TABLE_FILTER + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_STUDY_UUID + " TEXT" + "," +
                    COLUMN_FILTER_NAME + " TEXT" + "," +
                    COLUMN_FILTER_SAMPLE_SIZE + " INTEGER" + "," +
                    COLUMN_FILTER_SAMPLE_TYPE_INDEX + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_STUDY_UUID) REFERENCES $TABLE_STUDY($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableFilter)

            val createTableFilterOperator = ("CREATE TABLE " +
                    TABLE_FILTEROPERATOR + " (" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_FILTEROPERATOR_ORDER + " INTEGER NOT NULL " + "," +
                    COLUMN_CONNECTOR +  " INTEGER " + "," +
                    COLUMN_FILTER_UUID + " TEXT " + "," +
                    COLUMN_FIRST_RULE_UUID +   " TEXT " + "," +
                    COLUMN_SECOND_RULE_UUID +  " TEXT " + "," +
                    "FOREIGN KEY($COLUMN_FILTER_UUID) REFERENCES $TABLE_FILTER($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_FIRST_RULE_UUID) REFERENCES $TABLE_RULE($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_SECOND_RULE_UUID) REFERENCES $TABLE_RULE($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableFilterOperator)

            val createTableEnumerationTeam = ("CREATE TABLE " +
                    TABLE_ENUMERATION_TEAM + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_ENUM_AREA_UUID + " TEXT" + "," +
                    COLUMN_ENUMERATION_TEAM_NAME + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_UUID) REFERENCES $TABLE_ENUM_AREA($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableEnumerationTeam)

            val createTableCollectionTeam = ("CREATE TABLE " +
                    TABLE_COLLECTION_TEAM + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_ENUM_AREA_UUID + " TEXT" + "," +
                    COLUMN_COLLECTION_TEAM_NAME + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_UUID) REFERENCES $TABLE_ENUM_AREA($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableCollectionTeam)

            val createTableLocation = ("CREATE TABLE " +
                    TABLE_LOCATION + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_TIME_ZONE + " INTEGER" + "," +
                    COLUMN_LOCATION_TYPE_ID + " INTEGER" + "," +
                    COLUMN_LOCATION_GPS_ACCURACY + " INTEGER" + "," +
                    COLUMN_LOCATION_LATITUDE + " REAL" + "," +
                    COLUMN_LOCATION_LONGITUDE + " REAL" + "," +
                    COLUMN_LOCATION_ALTITUDE + " REAL" + "," +
                    COLUMN_LOCATION_IS_LANDMARK + " INTEGER" + "," +
                    COLUMN_LOCATION_DESCRIPTION + " TEXT" + "," +
                    COLUMN_LOCATION_IMAGE_UUID + " TEXT" + "," +
                    COLUMN_LOCATION_IS_MULTI_FAMILY + " INTEGER" + "," +
                    COLUMN_LOCATION_PROPERTIES + " STRING" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableLocation)

            val createConnectorTableLocationEnumArea = ("CREATE TABLE " +
                    CONNECTOR_TABLE_LOCATION__ENUM_AREA + "(" +
                    COLUMN_CID + COLUMN_CID_TYPE + "," +
                    COLUMN_LOCATION_UUID + " TEXT" + "," +
                    COLUMN_ENUM_AREA_UUID + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_LOCATION_UUID) REFERENCES $TABLE_LOCATION($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_UUID) REFERENCES $TABLE_ENUM_AREA($COLUMN_UUID)" + "," +
                    "UNIQUE ($COLUMN_LOCATION_UUID, $COLUMN_ENUM_AREA_UUID)" +
                    ")")
            db.execSQL(createConnectorTableLocationEnumArea)

            val createConnectorTableLocationEnumerationTeam = ("CREATE TABLE " +
                    CONNECTOR_TABLE_LOCATION__ENUMERATION_TEAM + "(" +
                    COLUMN_CID + COLUMN_CID_TYPE + "," +
                    COLUMN_LOCATION_UUID + " TEXT" + "," +
                    COLUMN_ENUMERATION_TEAM_UUID + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_LOCATION_UUID) REFERENCES $TABLE_LOCATION($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_ENUMERATION_TEAM_UUID) REFERENCES $TABLE_ENUMERATION_TEAM($COLUMN_UUID)" + "," +
                    "UNIQUE ($COLUMN_LOCATION_UUID, $COLUMN_ENUMERATION_TEAM_UUID)" +
                    ")")
            db.execSQL(createConnectorTableLocationEnumerationTeam)

            val createConnectorTableLocationCollectionTeam = ("CREATE TABLE " +
                    CONNECTOR_TABLE_LOCATION__COLLECTION_TEAM + "(" +
                    COLUMN_CID + COLUMN_CID_TYPE + "," +
                    COLUMN_LOCATION_UUID + " TEXT" + "," +
                    COLUMN_COLLECTION_TEAM_UUID + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_LOCATION_UUID) REFERENCES $TABLE_LOCATION($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_COLLECTION_TEAM_UUID) REFERENCES $TABLE_COLLECTION_TEAM($COLUMN_UUID)" + "," +
                    "UNIQUE ($COLUMN_LOCATION_UUID, $COLUMN_COLLECTION_TEAM_UUID)" +
                    ")")
            db.execSQL(createConnectorTableLocationCollectionTeam)

            val createTableEnumerationItem = ("CREATE TABLE " +
                    TABLE_ENUMERATION_ITEM + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_SYNC_CODE + " INTEGER" + "," +
                    COLUMN_LOCATION_UUID + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_SUB_ADDRESS + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_ENUMERATOR_NAME + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_ENUMERATION_DATE + " INTEGER" + "," +
                    COLUMN_ENUMERATION_ITEM_ENUMERATION_INCOMPLETE_REASON + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_ENUMERATION_NOTES + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SAMPLING + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_SAMPLING_STATE + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_COLLECTOR_NAME + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_COLLECTION_STATE + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_COLLECTION_DATE + " INTEGER" + "," +
                    COLUMN_ENUMERATION_ITEM_COLLECTION_INCOMPLETE_REASON + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_COLLECTION_NOTES + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_ODK_RECORD_URI + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_LOCATION_UUID) REFERENCES $TABLE_LOCATION($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableEnumerationItem)

            val createTableFieldData = ("CREATE TABLE " +
                    TABLE_FIELD_DATA + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_STUDY_UUID + " TEXT" + "," +
                    COLUMN_FIELD_UUID + " TEXT" + "," +
                    COLUMN_ENUMERATION_ITEM_UUID + " TEXT" + "," +
                    COLUMN_FIELD_NAME + " TEXT" + "," +
                    COLUMN_FIELD_TYPE_INDEX + " INTEGER" + "," +
                    COLUMN_FIELD_DATA_TEXT_VALUE + " TEXT" + "," +
                    COLUMN_FIELD_DATA_NUMBER_VALUE + " REAL" + "," +
                    COLUMN_FIELD_DATA_DATE_VALUE + " INTEGER" + "," +
                    COLUMN_FIELD_DATA_DROPDOWN_INDEX + " INTEGER" + "," +
                    COLUMN_FIELD_DATA_BLOCK_NUMBER + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_STUDY_UUID) REFERENCES $TABLE_STUDY($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_UUID) REFERENCES $TABLE_FIELD($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableFieldData)

            val createTableFieldDataOption = ("CREATE TABLE " +
                    TABLE_FIELD_DATA_OPTION + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_FIELD_DATA_OPTION_NAME + " TEXT" + "," +
                    COLUMN_FIELD_DATA_OPTION_VALUE + " INTEGER" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableFieldDataOption)

            val createConnectorTableFieldData__FieldDataOption = ("CREATE TABLE " +
                    CONNECTOR_TABLE_FIELD_DATA__FIELD_DATA_OPTION + "(" +
                    COLUMN_CID + COLUMN_CID_TYPE + "," +
                    COLUMN_FIELD_DATA_UUID + " TEXT" + "," +
                    COLUMN_FIELD_DATA_OPTION_UUID + " TEXT" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_DATA_UUID) REFERENCES $TABLE_FIELD_DATA($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_DATA_OPTION_UUID) REFERENCES $TABLE_FIELD_DATA_OPTION($COLUMN_UUID)" +
                    ")")
            db.execSQL(createConnectorTableFieldData__FieldDataOption)

            val createConnectorTableRule__FieldDataOption = ("CREATE TABLE " +
                    CONNECTOR_TABLE_RULE__FIELD_DATA_OPTION + "(" +
                    COLUMN_CID + COLUMN_CID_TYPE + "," +
                    COLUMN_RULE_UUID + " TEXT" + "," +
                    COLUMN_FIELD_DATA_OPTION_UUID + " INTEGER" + "," +
                    "FOREIGN KEY($COLUMN_RULE_UUID) REFERENCES $TABLE_RULE($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_FIELD_DATA_OPTION_UUID) REFERENCES $TABLE_FIELD_DATA_OPTION($COLUMN_UUID)" +
                    ")")
            db.execSQL(createConnectorTableRule__FieldDataOption)

            val createTableLatLon = ("CREATE TABLE " +
                    TABLE_LAT_LON + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_LAT + " REAL" + "," +
                    COLUMN_LON + " REAL" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableLatLon)

            val createConnectorTableEnumAreaLatLon = ("CREATE TABLE " +
                    CONNECTOR_TABLE_ENUM_AREA__LAT_LON + "(" +
                    COLUMN_CID + COLUMN_CID_TYPE + "," +
                    COLUMN_ENUM_AREA_UUID + " TEXT " + "," +
                    COLUMN_LAT_LON_UUID + " TEXT " + "," +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_UUID) REFERENCES $TABLE_ENUM_AREA($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_LAT_LON_UUID) REFERENCES $TABLE_LAT_LON($COLUMN_UUID)" + "," +
                    "UNIQUE ($COLUMN_ENUM_AREA_UUID, $COLUMN_LAT_LON_UUID)" +
                    ")")
            db.execSQL(createConnectorTableEnumAreaLatLon)

            val createConnectorTableEnumerationTeamLatLon = ("CREATE TABLE " +
                    CONNECTOR_TABLE_ENUMERATION_TEAM__LAT_LON + "(" +
                    COLUMN_CID + COLUMN_CID_TYPE + "," +
                    COLUMN_ENUMERATION_TEAM_UUID + " TEXT " + "," +
                    COLUMN_LAT_LON_UUID + " TEXT " + "," +
                    "FOREIGN KEY($COLUMN_ENUMERATION_TEAM_UUID) REFERENCES $TABLE_ENUMERATION_TEAM($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_LAT_LON_UUID) REFERENCES $TABLE_LAT_LON($COLUMN_UUID)" + "," +
                    "UNIQUE ($COLUMN_ENUMERATION_TEAM_UUID, $COLUMN_LAT_LON_UUID)" +
                    ")")
            db.execSQL(createConnectorTableEnumerationTeamLatLon)

            val createConnectorTableCollectionTeamLatLon = ("CREATE TABLE " +
                    CONNECTOR_TABLE_COLLECTION_TEAM__LAT_LON + "(" +
                    COLUMN_CID + COLUMN_CID_TYPE + "," +
                    COLUMN_COLLECTION_TEAM_UUID + " TEXT " + "," +
                    COLUMN_LAT_LON_UUID + " TEXT " + "," +
                    "FOREIGN KEY($COLUMN_COLLECTION_TEAM_UUID) REFERENCES $TABLE_COLLECTION_TEAM($COLUMN_UUID)" + "," +
                    "FOREIGN KEY($COLUMN_LAT_LON_UUID) REFERENCES $TABLE_LAT_LON($COLUMN_UUID)" + "," +
                    "UNIQUE ($COLUMN_COLLECTION_TEAM_UUID, $COLUMN_LAT_LON_UUID)" +
                    ")")
            db.execSQL(createConnectorTableCollectionTeamLatLon)

            val createTableMapTileRegion = ("CREATE TABLE " +
                    TABLE_MAP_TILE_REGION + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_ENUM_AREA_UUID + " TEXT" + "," +
                    COLUMN_NORTH_EAST_LAT + " REAL" + "," +
                    COLUMN_NORTH_EAST_LON + " REAL" + "," +
                    COLUMN_SOUTH_WEST_LAT + " REAL" + "," +
                    COLUMN_SOUTH_WEST_LON + " REAL" + "," +
                    "FOREIGN KEY($COLUMN_ENUM_AREA_UUID) REFERENCES $TABLE_ENUM_AREA($COLUMN_UUID)" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableMapTileRegion)

            val createTableBreadcrumb = ("CREATE TABLE " +
                    TABLE_BREADCRUMB + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_ENUM_AREA_UUID + " TEXT" + "," +
                    COLUMN_LATITUDE + " REAL" + "," +
                    COLUMN_LONGITUDE + " REAL" + "," +
                    COLUMN_GROUP_ID + " TEXT" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableBreadcrumb)
        }
        catch(ex: Exception)
        {
            Log.d("xxx", ex.stackTraceToString())
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
    {
        if (oldVersion == 314 && newVersion == 321)
        {
            migrateFrom314To321( db )
        }
        else
        {
            dropAllTables( db )
        }
    }

    fun migrateFrom314To321XXX( db: SQLiteDatabase )
    {
        try
        {
            // Add new columns to Config table
            db.execSQL("ALTER TABLE $TABLE_CONFIG ADD COLUMN $COLUMN_CONFIG_MAP_ENGINE_INDEX INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_CONFIG ADD COLUMN $COLUMN_CONFIG_VALID_USERS TEXT")

            // Assign a default value for VALID_USERS to existing rows
            db.execSQL("UPDATE $TABLE_CONFIG SET $COLUMN_CONFIG_VALID_USERS = '' WHERE $COLUMN_CONFIG_VALID_USERS IS NULL")

            // Rename old Location table
            db.execSQL("ALTER TABLE $TABLE_LOCATION RENAME TO ${TABLE_LOCATION}_old")

            // Create new Location table with updated schema
            val createTableLocation = ("CREATE TABLE " +
                    TABLE_LOCATION + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_TIME_ZONE + " INTEGER" + "," +
                    COLUMN_LOCATION_TYPE_ID + " INTEGER" + "," +
                    COLUMN_LOCATION_GPS_ACCURACY + " INTEGER" + "," +
                    COLUMN_LOCATION_LATITUDE + " REAL" + "," +
                    COLUMN_LOCATION_LONGITUDE + " REAL" + "," +
                    COLUMN_LOCATION_ALTITUDE + " REAL" + "," +
                    COLUMN_LOCATION_IS_LANDMARK + " INTEGER" + "," +
                    COLUMN_LOCATION_DESCRIPTION + " TEXT" + "," +
                    COLUMN_LOCATION_IMAGE_UUID + " TEXT" + "," +
                    COLUMN_LOCATION_IS_MULTI_FAMILY + " INTEGER" + "," +
                    COLUMN_LOCATION_PROPERTIES + " STRING" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableLocation)

            // Migrate data from old Location table to new one
            db.execSQL("""
                INSERT INTO $TABLE_LOCATION (
                    $COLUMN_UUID, $COLUMN_CREATION_DATE, $COLUMN_TIME_ZONE,
                    $COLUMN_LOCATION_TYPE_ID, $COLUMN_LOCATION_GPS_ACCURACY,
                    $COLUMN_LOCATION_LATITUDE, $COLUMN_LOCATION_LONGITUDE,
                    $COLUMN_LOCATION_ALTITUDE, $COLUMN_LOCATION_IS_LANDMARK,
                    $COLUMN_LOCATION_DESCRIPTION, $COLUMN_LOCATION_IMAGE_UUID,
                    $COLUMN_LOCATION_IS_MULTI_FAMILY, $COLUMN_LOCATION_PROPERTIES
                )
                SELECT
                    $COLUMN_UUID, $COLUMN_CREATION_DATE, $COLUMN_TIME_ZONE,
                    $COLUMN_LOCATION_TYPE_ID, $COLUMN_LOCATION_GPS_ACCURACY,
                    $COLUMN_LOCATION_LATITUDE, $COLUMN_LOCATION_LONGITUDE,
                    $COLUMN_LOCATION_ALTITUDE, $COLUMN_LOCATION_IS_LANDMARK,
                    $COLUMN_LOCATION_DESCRIPTION, '' AS $COLUMN_LOCATION_IMAGE_UUID,
                    $COLUMN_LOCATION_IS_MULTI_FAMILY, $COLUMN_LOCATION_PROPERTIES
                FROM ${TABLE_LOCATION}_old
            """)

            // Extract imageData from the old Location table, create new Image in the Image db,
            // Update the new Location table with the new Image Id

            val cursor = db.rawQuery("SELECT $COLUMN_UUID, $COLUMN_LOCATION_IMAGE_DATA FROM ${TABLE_LOCATION}_old", null)

            while (cursor.moveToNext())
            {
                val locationUuid = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UUID))
                val imageData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_IMAGE_DATA))

                if (imageData != null && imageData.isNotEmpty())
                {
                    ImageDAO.instance().createImage(Image(locationUuid, imageData))?.let { image ->
                        db.execSQL(
                            "UPDATE $TABLE_LOCATION SET $COLUMN_LOCATION_IMAGE_UUID = ? WHERE $COLUMN_UUID = ?",
                            arrayOf(image.uuid, locationUuid)
                        )
                    }
                }
            }

            cursor.close()

            // Drop old Location table
            db.execSQL("DROP TABLE ${TABLE_LOCATION}_old")

            // Create Breadcrumb table
            val createTableBreadcrumb = ("CREATE TABLE " +
                    TABLE_BREADCRUMB + "(" +
                    COLUMN_UUID + COLUMN_UUID_TYPE + "," +
                    COLUMN_CREATION_DATE + " INTEGER" + "," +
                    COLUMN_ENUM_AREA_UUID + " TEXT" + "," +
                    COLUMN_LATITUDE + " REAL" + "," +
                    COLUMN_LONGITUDE + " REAL" + "," +
                    COLUMN_GROUP_ID + " TEXT" +
                    ") WITHOUT ROWID")
            db.execSQL(createTableBreadcrumb)
        }
        catch (ex: Exception)
        {
            Log.d( "xxx", "Migration from DB 314 to 321 FAILED")
        }
    }

    fun migrateFrom314To321(db: SQLiteDatabase) {
        db.beginTransaction()
        try {
            // ----------------------------
            // 1. Update Config table
            // ----------------------------
            db.execSQL("ALTER TABLE $TABLE_CONFIG ADD COLUMN $COLUMN_CONFIG_MAP_ENGINE_INDEX INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_CONFIG ADD COLUMN $COLUMN_CONFIG_VALID_USERS TEXT")
            db.execSQL("UPDATE $TABLE_CONFIG SET $COLUMN_CONFIG_VALID_USERS = '' WHERE $COLUMN_CONFIG_VALID_USERS IS NULL")

            // ----------------------------
            // 2. Migrate Location table
            // ----------------------------
            db.execSQL("ALTER TABLE $TABLE_LOCATION RENAME TO ${TABLE_LOCATION}_old")

            val createTableLocation = """
            CREATE TABLE $TABLE_LOCATION (
                $COLUMN_UUID $COLUMN_UUID_TYPE,
                $COLUMN_CREATION_DATE INTEGER,
                $COLUMN_TIME_ZONE INTEGER,
                $COLUMN_LOCATION_TYPE_ID INTEGER,
                $COLUMN_LOCATION_GPS_ACCURACY INTEGER,
                $COLUMN_LOCATION_LATITUDE REAL,
                $COLUMN_LOCATION_LONGITUDE REAL,
                $COLUMN_LOCATION_ALTITUDE REAL,
                $COLUMN_LOCATION_IS_LANDMARK INTEGER,
                $COLUMN_LOCATION_DESCRIPTION TEXT,
                $COLUMN_LOCATION_IMAGE_UUID TEXT,
                $COLUMN_LOCATION_IS_MULTI_FAMILY INTEGER,
                $COLUMN_LOCATION_PROPERTIES STRING
            ) WITHOUT ROWID
            """.trimIndent()
            db.execSQL(createTableLocation)

            // Copy data from old table
            db.execSQL("""
            INSERT INTO $TABLE_LOCATION (
                $COLUMN_UUID, $COLUMN_CREATION_DATE, $COLUMN_TIME_ZONE,
                $COLUMN_LOCATION_TYPE_ID, $COLUMN_LOCATION_GPS_ACCURACY,
                $COLUMN_LOCATION_LATITUDE, $COLUMN_LOCATION_LONGITUDE,
                $COLUMN_LOCATION_ALTITUDE, $COLUMN_LOCATION_IS_LANDMARK,
                $COLUMN_LOCATION_DESCRIPTION, $COLUMN_LOCATION_IMAGE_UUID,
                $COLUMN_LOCATION_IS_MULTI_FAMILY, $COLUMN_LOCATION_PROPERTIES
            )
            SELECT
                $COLUMN_UUID, $COLUMN_CREATION_DATE, $COLUMN_TIME_ZONE,
                $COLUMN_LOCATION_TYPE_ID, $COLUMN_LOCATION_GPS_ACCURACY,
                $COLUMN_LOCATION_LATITUDE, $COLUMN_LOCATION_LONGITUDE,
                $COLUMN_LOCATION_ALTITUDE, $COLUMN_LOCATION_IS_LANDMARK,
                $COLUMN_LOCATION_DESCRIPTION, '' AS $COLUMN_LOCATION_IMAGE_UUID,
                $COLUMN_LOCATION_IS_MULTI_FAMILY, $COLUMN_LOCATION_PROPERTIES
            FROM ${TABLE_LOCATION}_old
            """)

            // Migrate images
            val cursor = db.rawQuery("SELECT $COLUMN_UUID, $COLUMN_LOCATION_IMAGE_DATA FROM ${TABLE_LOCATION}_old", null)
            while (cursor.moveToNext()) {
                val locationUuid = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_UUID))
                val imageData = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION_IMAGE_DATA))

                if (!imageData.isNullOrEmpty()) {
                    ImageDAO.instance().createImage(Image(locationUuid, imageData))?.let { image ->
                        db.execSQL(
                            "UPDATE $TABLE_LOCATION SET $COLUMN_LOCATION_IMAGE_UUID = ? WHERE $COLUMN_UUID = ?",
                            arrayOf(image.uuid, locationUuid)
                        )
                    }
                }
            }
            cursor.close()

            db.execSQL("DROP TABLE ${TABLE_LOCATION}_old")

            // ----------------------------
            // 3. Create Breadcrumb table
            // ----------------------------
            val createTableBreadcrumb = """
            CREATE TABLE $TABLE_BREADCRUMB (
                $COLUMN_UUID $COLUMN_UUID_TYPE,
                $COLUMN_CREATION_DATE INTEGER,
                $COLUMN_ENUM_AREA_UUID TEXT,
                $COLUMN_LATITUDE REAL,
                $COLUMN_LONGITUDE REAL,
                $COLUMN_GROUP_ID TEXT
            ) WITHOUT ROWID
            """.trimIndent()
            db.execSQL(createTableBreadcrumb)

            // ----------------------------
            // 4. Update dbVersion in Config
            // ----------------------------
            val newDbVersion = 321
            val contentValues = ContentValues().apply {
                put(COLUMN_CONFIG_DB_VERSION, newDbVersion)
            }
            db.update(TABLE_CONFIG, contentValues, null, null)

            // ----------------------------
            // 5. Commit transaction
            // ----------------------------
            db.setTransactionSuccessful()
        } catch (ex: Exception) {
            Log.d("xxx", "Migration from DB 314 to 321 FAILED: ${ex.message}")
            throw ex // optional: rethrow so Android knows migration failed
        } finally {
            db.endTransaction()
        }
    }

    fun dropAllTables( db: SQLiteDatabase )
    {
        try
        {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_CONFIG")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_STUDY")
            db.execSQL("DROP TABLE IF EXISTS $CONNECTOR_TABLE_CONFIG__STUDY")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FIELD")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_RULE")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FILTER")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FILTEROPERATOR")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_ENUM_AREA")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_ENUMERATION_TEAM")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_COLLECTION_TEAM")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FIELD_DATA")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FIELD_DATA_OPTION")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_LAT_LON")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_LOCATION")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_ENUMERATION_ITEM")
            db.execSQL("DROP TABLE IF EXISTS $CONNECTOR_TABLE_ENUM_AREA__LAT_LON")
            db.execSQL("DROP TABLE IF EXISTS $CONNECTOR_TABLE_ENUMERATION_TEAM__LAT_LON")
            db.execSQL("DROP TABLE IF EXISTS $CONNECTOR_TABLE_COLLECTION_TEAM__LAT_LON")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FIELD_OPTION")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_MAP_TILE_REGION")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_BREADCRUMB")

            db.execSQL("DROP TABLE IF EXISTS $CONNECTOR_TABLE_FIELD__FIELD_OPTION")
            db.execSQL("DROP TABLE IF EXISTS $CONNECTOR_TABLE_FIELD_DATA__FIELD_DATA_OPTION")
            db.execSQL("DROP TABLE IF EXISTS $CONNECTOR_TABLE_RULE__FIELD_DATA_OPTION")
            db.execSQL("DROP TABLE IF EXISTS $CONNECTOR_TABLE_LOCATION__ENUM_AREA")
            db.execSQL("DROP TABLE IF EXISTS $CONNECTOR_TABLE_LOCATION__ENUMERATION_TEAM")
            db.execSQL("DROP TABLE IF EXISTS $CONNECTOR_TABLE_LOCATION__COLLECTION_TEAM")

            onCreate(db)
        }
        catch (ex:Exception)
        {
            Log.d( "xxx", "Drop All tables FAILED" )
        }
    }

    companion object
    {
        private const val DATABASE_NAME = "GPSSampleDB.db"

        const val COLUMN_CID = "cid"
        const val COLUMN_CID_TYPE = " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL"

        const val COLUMN_UUID = "uuid"
        const val COLUMN_UUID_TYPE = " TEXT PRIMARY KEY NOT NULL"

        const val COLUMN_TIME_ZONE = "time_zone"
        const val COLUMN_SYNC_CODE = "sync_code"
        const val COLUMN_CREATION_DATE = "creation_date"

        // foreign key columns
        const val COLUMN_CONFIG_UUID = "config_uuid"
        const val COLUMN_STUDY_UUID = "study_uuid"
        const val COLUMN_FIELD_UUID = "field_uuid"
        const val COLUMN_FIELD_OPTION_UUID = "field_option_uuid"
        const val COLUMN_FIELD_DATA_UUID = "field_data_uuid"
        const val COLUMN_FIELD_DATA_OPTION_UUID = "field_data_option_uuid"
        const val COLUMN_RULE_UUID = "rule_uuid"
        const val COLUMN_FILTER_UUID = "filter_uuid"
        const val COLUMN_ENUM_AREA_UUID = "enum_area_uuid"
        const val COLUMN_ENUMERATION_TEAM_UUID = "enumeration_team_uuid"
        const val COLUMN_COLLECTION_TEAM_UUID = "collection_team_uuid"
        const val COLUMN_OPERATOR_ID = "operator_id"
        const val COLUMN_LOCATION_UUID = "location_uuid"
        const val COLUMN_ENUMERATION_ITEM_UUID = "enumeration_item_uuid"
        const val COLUMN_LAT_LON_UUID = "lat_lon_uuid"

        // Connector Tables
        const val CONNECTOR_TABLE_CONFIG__STUDY = "config__study"
        const val CONNECTOR_TABLE_FIELD__FIELD_OPTION = "field__field_option"
        const val CONNECTOR_TABLE_LOCATION__ENUM_AREA = "location__enum_area"
        const val CONNECTOR_TABLE_LOCATION__ENUMERATION_TEAM = "location__enumeration_team"
        const val CONNECTOR_TABLE_LOCATION__COLLECTION_TEAM = "location__collection_team"
        const val CONNECTOR_TABLE_FIELD_DATA__FIELD_DATA_OPTION = "field_data__field_data_option"
        const val CONNECTOR_TABLE_ENUM_AREA__LAT_LON = "enum_area__lat_lon"
        const val CONNECTOR_TABLE_ENUMERATION_TEAM__LAT_LON = "enumeration_team__lat_lon"
        const val CONNECTOR_TABLE_COLLECTION_TEAM__LAT_LON = "collection_team__lat_lon"
        const val CONNECTOR_TABLE_RULE__FIELD_DATA_OPTION = "rule__field_data_option"

        // User Table
        const val TABLE_USER = "user"
        const val COLUMN_USER_ROLE = "user_role"
        const val COLUMN_USER_NAME = "user_name"
        const val COLUMN_USER_RECOVERY_QUESTION = "user_recover_question"
        const val COLUMN_USER_RECOVERY_ANSWER = "user_recovery_answer"
        const val COLUMN_USER_IS_ONLINE = "user_is_online"

        // Config Table
        const val TABLE_CONFIG = "config"
        const val COLUMN_CONFIG_NAME = "config_name"
        const val COLUMN_CONFIG_DB_VERSION = "db_version"
        const val COLUMN_CONFIG_MAP_ENGINE_INDEX = "config_map_engine_index"
        const val COLUMN_CONFIG_DISTANCE_FORMAT_INDEX = "config_distance_format_index"
        const val COLUMN_CONFIG_DATE_FORMAT_INDEX = "config_date_format_index"
        const val COLUMN_CONFIG_TIME_FORMAT_INDEX = "config_time_format_index"
        const val COLUMN_CONFIG_MIN_GPS_PRECISION = "config_min_gps_precision"
        const val COLUMN_CONFIG_ENCRYPTION_PASSWORD = "config_encryption_password"
        const val COLUMN_CONFIG_ALLOW_MANUAL_LOCATION_ENTRY = "config_allow_manual_location_entry"
        const val COLUMN_CONFIG_SUBADDRESS_IS_REQUIRED = "config_subaddress_is_required"
        const val COLUMN_CONFIG_AUTO_INCREMENT_SUBADDRESS = "config_auto_increment_subaddress"
        const val COLUMN_CONFIG_PROXIMITY_WARNING_IS_ENABLED = "config_proximity_warning_is_enabled"
        const val COLUMN_CONFIG_PROXIMITY_WARNING_VALUE = "config_proximity_warning_value"
        const val COLUMN_CONFIG_VALID_USERS = "config_valid_users"

        // Study Table
        const val TABLE_STUDY = "study"
        const val COLUMN_STUDY_NAME = "study_name"
        const val COLUMN_STUDY_SAMPLING_METHOD_INDEX = "study_sampling_method_index"
        const val COLUMN_STUDY_SAMPLE_SIZE = "study_sample_size"
        const val COLUMN_STUDY_SAMPLE_SIZE_INDEX = "study_sample_size_index"

        // Field Table
        const val TABLE_FIELD = "field"
        const val COLUMN_FIELD_NAME = "field_name"
        const val COLUMN_FIELD_INDEX = "field_index"
        const val COLUMN_FIELD_PARENT_UUID = "field_parent_uuid"
        const val COLUMN_FIELD_TYPE_INDEX = "field_type_index"
        const val COLUMN_FIELD_PII = "field_pii"
        const val COLUMN_FIELD_REQUIRED = "field_required"
        const val COLUMN_FIELD_INTEGER_ONLY = "field_integer_only"
        const val COLUMN_FIELD_NUMBER_OF_RESIDENTS = "field_number_of_residents"
        const val COLUMN_FIELD_DATE = "field_date"
        const val COLUMN_FIELD_TIME = "field_time"
        const val COLUMN_FIELD_MINIMUM = "field_minimum"
        const val COLUMN_FIELD_MAXIMUM = "field_maximum"
        const val COLUMN_FIELD_OPTION_1 = "field_option_1"
        const val COLUMN_FIELD_OPTION_2 = "field_option_2"
        const val COLUMN_FIELD_OPTION_3 = "field_option_3"
        const val COLUMN_FIELD_OPTION_4 = "field_option_4"

        const val TABLE_FIELD_OPTION = "field_option"
        const val COLUMN_FIELD_OPTION_NAME = "field_option_name"

        // Rule Table
        const val TABLE_RULE = "rule"
        const val COLUMN_RULE_NAME = "rule_name"
        const val COLUMN_RULE_VALUE = "rule_value"
        const val COLUMN_FILTEROPERATOR_UUID = "filter_operator_uuid"
        // Filter Table
        const val TABLE_FILTER = "filter"
        const val COLUMN_FILTER_NAME = "filter_name"
        const val COLUMN_FILTER_SAMPLE_SIZE = "filter_sample_size"
        const val COLUMN_FILTER_SAMPLE_TYPE_INDEX = "filter_sample_type_index"

        // FilterOperator
        const val TABLE_FILTEROPERATOR = "filteroperator"
        const val COLUMN_FILTEROPERATOR_ORDER = "operator_order"
        const val COLUMN_CONNECTOR = "connector"
        const val COLUMN_FIRST_RULE_UUID = "first_rule_uuid"
        const val COLUMN_SECOND_RULE_UUID = "second_rule_uuid"

        // EnumArea Table
        const val TABLE_ENUM_AREA = "enum_area"
        const val COLUMN_ENUM_AREA_NAME = "enum_area_name"
        const val COLUMN_ENUM_AREA_MBTILESPATH = "enum_area_mbtilespath"
        const val COLUMN_ENUM_AREA_MBTILESSIZE = "enum_area_mbtilessize"

        // EnumerationTeam Table
        const val TABLE_ENUMERATION_TEAM = "enumeration_team"
        const val COLUMN_ENUMERATION_TEAM_NAME = "enumeration_team_name"

        // CollectionTeam Table
        const val TABLE_COLLECTION_TEAM = "collection_team"
        const val COLUMN_COLLECTION_TEAM_NAME = "collection_team_name"

        // Location Table
        const val TABLE_LOCATION = "location"
        const val COLUMN_LOCATION_TYPE_ID = "location_type_id"
        const val COLUMN_LOCATION_GPS_ACCURACY = "location_gps_accuracy"
        const val COLUMN_LOCATION_LATITUDE = "location_latitude"
        const val COLUMN_LOCATION_LONGITUDE = "location_longitude"
        const val COLUMN_LOCATION_ALTITUDE = "location_altitude"
        const val COLUMN_LOCATION_IS_LANDMARK = "location_is_landmark"
        const val COLUMN_LOCATION_DESCRIPTION = "location_description"
        const val COLUMN_LOCATION_IMAGE_DATA = "location_image_data"
        const val COLUMN_LOCATION_IMAGE_UUID = "location_image_uuid"
        const val COLUMN_LOCATION_IS_MULTI_FAMILY = "location_is_multi_family"
        const val  COLUMN_LOCATION_PROPERTIES = "location_properties"

        // EnumerationItem Table
        const val TABLE_ENUMERATION_ITEM = "enumeration_item"
        const val COLUMN_ENUMERATION_ITEM_SUB_ADDRESS = "enumeration_item_sub_address"
        const val COLUMN_ENUMERATION_ITEM_ENUMERATOR_NAME = "enumeration_item_enumerator_name"
        const val COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE = "enumeration_item_enumeration_state"
        const val COLUMN_ENUMERATION_ITEM_ENUMERATION_DATE = "enumeration_item_enumeration_date"
        const val COLUMN_ENUMERATION_ITEM_ENUMERATION_INCOMPLETE_REASON = "enumeration_item_enumeration_incomplete_reason"
        const val COLUMN_ENUMERATION_ITEM_ENUMERATION_NOTES = "enumeration_item_enumeration_notes"
        const val COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SAMPLING = "enumeration_item_enumeration_eligible_for_sampling"
        const val COLUMN_ENUMERATION_ITEM_SAMPLING_STATE = "enumeration_item_sampling_state"
        const val COLUMN_ENUMERATION_ITEM_COLLECTOR_NAME = "enumeration_item_collector_name"
        const val COLUMN_ENUMERATION_ITEM_COLLECTION_STATE = "enumeration_item_collection_state"
        const val COLUMN_ENUMERATION_ITEM_COLLECTION_DATE = "enumeration_item_collection_date"
        const val COLUMN_ENUMERATION_ITEM_COLLECTION_INCOMPLETE_REASON = "enumeration_item_collection_incomplete_reason"
        const val COLUMN_ENUMERATION_ITEM_COLLECTION_NOTES = "enumeration_item_collection_notes"
        const val COLUMN_ENUMERATION_ITEM_ODK_RECORD_URI = "enumeration_item_odk_record_uri"

        const val TABLE_FIELD_DATA = "field_data"
        const val COLUMN_FIELD_DATA_TEXT_VALUE = "field_data_text_value"
        const val COLUMN_FIELD_DATA_NUMBER_VALUE = "field_data_number_value"
        const val COLUMN_FIELD_DATA_DATE_VALUE = "field_data_date_value"
        const val COLUMN_FIELD_DATA_DROPDOWN_INDEX = "field_data_dropdown_index"
        const val COLUMN_FIELD_DATA_BLOCK_NUMBER = "field_data_block_number"

        const val TABLE_FIELD_DATA_OPTION = "field_data_option"
        const val COLUMN_FIELD_DATA_OPTION_NAME = "field_data_option_name"
        const val COLUMN_FIELD_DATA_OPTION_VALUE = "field_data_option_value"

        const val TABLE_MAP_TILE_REGION = "map_tile_region"
        const val COLUMN_NORTH_EAST_LAT = "north_east_lat"
        const val COLUMN_NORTH_EAST_LON = "north_east_lon"
        const val COLUMN_SOUTH_WEST_LAT = "south_west_lat"
        const val COLUMN_SOUTH_WEST_LON = "south_west_lon"

        const val TABLE_LAT_LON = "lat_lon"
        const val COLUMN_LAT = "lat"
        const val COLUMN_LON = "lon"

        const val TABLE_BREADCRUMB = "breadcrumb"
        const val COLUMN_LATITUDE = "latitude"
        const val COLUMN_LONGITUDE = "longitude"
        const val COLUMN_GROUP_ID = "group_id"

        // DAO's
        private lateinit var _userDAO: UserDAO
        private lateinit var _configDAO: ConfigDAO
        private lateinit var _studyDAO: StudyDAO
        private lateinit var _fieldDAO: FieldDAO
        private lateinit var _fieldOptionDAO: FieldOptionDAO
        private lateinit var _ruleDAO: RuleDAO
        private lateinit var _filterDAO: FilterDAO
        private lateinit var _enumAreaDAO: EnumAreaDAO
        private lateinit var _enumerationTeamDAO: EnumerationTeamDAO
        private lateinit var _collectionTeamDAO: CollectionTeamDAO
        private lateinit var _fieldDataDAO: FieldDataDAO
        private lateinit var _fieldDataOptionDAO: FieldDataOptionDAO
        private lateinit var _latLonDAO: LatLonDAO
        private lateinit var _locationDAO: LocationDAO
        private lateinit var _enumerationItemDAO: EnumerationItemDAO
        private lateinit var _mapTileRegionDAO: MapTileRegionDAO
        private lateinit var _breadcrumbDAO: BreadcrumbDAO

        val userDAO get() = _userDAO
        val configDAO get() = _configDAO
        val studyDAO get() = _studyDAO
        val fieldDAO get() = _fieldDAO
        val fieldOptionDAO get() = _fieldOptionDAO
        val ruleDAO get() = _ruleDAO
        val filterDAO get() = _filterDAO
        val enumAreaDAO get() = _enumAreaDAO
        val enumerationTeamDAO get() = _enumerationTeamDAO
        val collectionTeamDAO get() = _collectionTeamDAO
        val fieldDataDAO get() = _fieldDataDAO
        val fieldDataOptionDAO get() = _fieldDataOptionDAO
        val latLonDAO get() = _latLonDAO
        val locationDAO get() = _locationDAO
        val enumerationItemDAO get() = _enumerationItemDAO
        val mapTileRegionDAO get() = _mapTileRegionDAO
        val breadcrumbDAO get() = _breadcrumbDAO

        // creation/access methods

        private var _instance: DAO? = null

        public fun instance() : DAO
        {
            return _instance!!
        }

        fun deleteAll( includingUserTable: Boolean = false )
        {
            _instance?.let {
                val db = it.writableDatabase

                if (includingUserTable)
                {
                    db.execSQL("DELETE FROM $TABLE_USER")
                }

                db.execSQL("DELETE FROM $TABLE_CONFIG")
                db.execSQL("DELETE FROM $TABLE_ENUM_AREA")
                db.execSQL("DELETE FROM $TABLE_STUDY")
                db.execSQL("DELETE FROM $CONNECTOR_TABLE_CONFIG__STUDY")
                db.execSQL("DELETE FROM $TABLE_FIELD")
                db.execSQL("DELETE FROM $TABLE_FIELD_OPTION")
                db.execSQL("DELETE FROM $CONNECTOR_TABLE_FIELD__FIELD_OPTION")
                db.execSQL("DELETE FROM $TABLE_RULE")
                db.execSQL("DELETE FROM $TABLE_FILTER")
                db.execSQL("DELETE FROM $TABLE_FILTEROPERATOR")
                db.execSQL("DELETE FROM $TABLE_ENUMERATION_TEAM")
                db.execSQL("DELETE FROM $TABLE_COLLECTION_TEAM")
                db.execSQL("DELETE FROM $TABLE_LOCATION")
                db.execSQL("DELETE FROM $CONNECTOR_TABLE_LOCATION__ENUM_AREA")
                db.execSQL("DELETE FROM $CONNECTOR_TABLE_LOCATION__ENUMERATION_TEAM")
                db.execSQL("DELETE FROM $CONNECTOR_TABLE_LOCATION__COLLECTION_TEAM")
                db.execSQL("DELETE FROM $TABLE_ENUMERATION_ITEM")
                db.execSQL("DELETE FROM $TABLE_FIELD_DATA")
                db.execSQL("DELETE FROM $TABLE_FIELD_DATA_OPTION")
                db.execSQL("DELETE FROM $CONNECTOR_TABLE_FIELD_DATA__FIELD_DATA_OPTION")
                db.execSQL("DELETE FROM $CONNECTOR_TABLE_RULE__FIELD_DATA_OPTION")
                db.execSQL("DELETE FROM $TABLE_LAT_LON")
                db.execSQL("DELETE FROM $CONNECTOR_TABLE_ENUM_AREA__LAT_LON")
                db.execSQL("DELETE FROM $CONNECTOR_TABLE_ENUMERATION_TEAM__LAT_LON")
                db.execSQL("DELETE FROM $CONNECTOR_TABLE_COLLECTION_TEAM__LAT_LON")
                db.execSQL("DELETE FROM $TABLE_MAP_TILE_REGION")
                db.execSQL("DELETE FROM $TABLE_BREADCRUMB")

                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_CONFIG'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_ENUM_AREA'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_STUDY'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$CONNECTOR_TABLE_CONFIG__STUDY'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_FIELD'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_FIELD_OPTION'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$CONNECTOR_TABLE_FIELD__FIELD_OPTION'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_RULE'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_FILTER'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_FILTEROPERATOR'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_ENUMERATION_TEAM'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_COLLECTION_TEAM'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_LOCATION'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$CONNECTOR_TABLE_LOCATION__ENUM_AREA'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$CONNECTOR_TABLE_LOCATION__ENUMERATION_TEAM'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$CONNECTOR_TABLE_LOCATION__COLLECTION_TEAM'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_ENUMERATION_ITEM'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_FIELD_DATA'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_FIELD_DATA_OPTION'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$CONNECTOR_TABLE_FIELD_DATA__FIELD_DATA_OPTION'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$CONNECTOR_TABLE_RULE__FIELD_DATA_OPTION'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_LAT_LON'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$CONNECTOR_TABLE_ENUM_AREA__LAT_LON'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$CONNECTOR_TABLE_ENUMERATION_TEAM__LAT_LON'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$CONNECTOR_TABLE_COLLECTION_TEAM__LAT_LON'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_MAP_TILE_REGION'")
                db.execSQL("DELETE FROM SQLITE_SEQUENCE where name='$TABLE_BREADCRUMB'")

                db.close()
            }
        }

        fun createSharedInstance( context: Context ): DAO
        {
            if (_instance == null)
            {
                _instance = DAO( context, null, null, DATABASE_VERSION )

                _userDAO = UserDAO( _instance!! )
                _configDAO = ConfigDAO( _instance!! )
                _studyDAO = StudyDAO( _instance!! )
                _fieldDAO = FieldDAO( _instance!! )
                _fieldOptionDAO = FieldOptionDAO( _instance!! )
                _ruleDAO = RuleDAO( _instance!! )
                _filterDAO = FilterDAO( _instance!! )
                _enumAreaDAO = EnumAreaDAO( _instance!! )
                _enumerationTeamDAO = EnumerationTeamDAO( _instance!! )
                _collectionTeamDAO = CollectionTeamDAO( _instance!! )
                _fieldDataDAO = FieldDataDAO( _instance!! )
                _fieldDataOptionDAO = FieldDataOptionDAO( _instance!! )
                _latLonDAO = LatLonDAO( _instance!! )
                _locationDAO = LocationDAO( _instance!! )
                _enumerationItemDAO = EnumerationItemDAO( _instance!! )
                _mapTileRegionDAO = MapTileRegionDAO( _instance!! )
                _breadcrumbDAO = BreadcrumbDAO( _instance!! )
            }

            return _instance!!
        }

        const val DATABASE_VERSION = 321
    }
}