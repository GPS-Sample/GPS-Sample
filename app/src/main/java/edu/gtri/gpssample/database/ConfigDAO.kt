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
import android.database.DatabaseUtils
import android.util.Log
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_ALLOW_MANUAL_LOCATION_ENTRY
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_AUTO_INCREMENT_SUBADDRESS
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_DATE_FORMAT_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_DB_VERSION
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_DISTANCE_FORMAT_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_ENCRYPTION_PASSWORD
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_MAP_ENGINE_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_MIN_GPS_PRECISION
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_PROXIMITY_WARNING_IS_ENABLED
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_PROXIMITY_WARNING_VALUE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_SUBADDRESS_IS_REQUIRED
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_TIME_FORMAT_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CONFIG_VALID_USERS
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CREATION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUM_AREA_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_TIME_ZONE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_VERSION
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.managers.PreferencesManager
import java.util.Date

class ConfigDAO(private var dao: DAO)
{
    fun createOrUpdateConfig( config: Config, version: String )
    {
        config.version = version

        MainApplication.instance.user?.let { user ->
            if (!config.validUsers.contains(user.uuid))
            {
                config.validUsers += " ${user.uuid}"
            }
        }

        dao.writableDatabase.beginTransaction()

        val values = ContentValues()
        putConfig( config, values )
        dao.upsert( DAO.TABLE_CONFIG, values )

        createOrUpdateEnumAreas(config)
        createOrUpdateStudies(config)

        dao.writableDatabase.setTransactionSuccessful()
        dao.writableDatabase.endTransaction()
    }

    fun doRealUpsert()
    {
        // Possible future improvement, if needed
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
//        {
//            // This will do a real upsert
//            ColumnBinding.bindStatement( stmt = upsertStatement, uuid = config.uuid, columns = columnBindings, item = config )
//            upsertStatement.execute()
//        }
//        else
//        {
//            val values = ContentValues()
//            putConfig( config, values )
//            dao.upsert( DAO.TABLE_CONFIG, values )
//        }
    }

    fun putConfig( config: Config, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, config.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, config.creationDate )
        values.put( DAO.COLUMN_VERSION, config.version )
        values.put( DAO.COLUMN_TIME_ZONE, config.timeZone )
        values.put( DAO.COLUMN_ENUM_AREA_UUID, config.selectedEnumAreaUuid )
        values.put( DAO.COLUMN_STUDY_UUID, config.selectedStudyUuid )
        values.put( DAO.COLUMN_CONFIG_NAME, config.name )
        values.put( DAO.COLUMN_CONFIG_DB_VERSION, config.dbVersion )
        values.put( DAO.COLUMN_CONFIG_MAP_ENGINE_INDEX, config.mapEngineIndex )
        values.put( DAO.COLUMN_CONFIG_MIN_GPS_PRECISION, config.minGpsPrecision )
        values.put( DAO.COLUMN_CONFIG_ENCRYPTION_PASSWORD, config.encryptionPassword )
        values.put( DAO.COLUMN_CONFIG_ALLOW_MANUAL_LOCATION_ENTRY, config.allowManualLocationEntry )
        values.put( DAO.COLUMN_CONFIG_SUBADDRESS_IS_REQUIRED, config.subaddressIsrequired )
        values.put( DAO.COLUMN_CONFIG_AUTO_INCREMENT_SUBADDRESS, config.autoIncrementSubaddress )
        values.put( DAO.COLUMN_CONFIG_PROXIMITY_WARNING_IS_ENABLED, config.proximityWarningIsEnabled )
        values.put( DAO.COLUMN_CONFIG_PROXIMITY_WARNING_VALUE, config.proximityWarningValue )
        values.put( DAO.COLUMN_CONFIG_VALID_USERS, config.validUsers )

        // TODO: these should be from lookup tables
        val dateFormatIndex = DateFormatConverter.toIndex(config.dateFormat)
        val timeFormatIndex = TimeFormatConverter.toIndex(config.timeFormat)
        val distanceFormatIndex = DistanceFormatConverter.toIndex(config.distanceFormat)

        values.put( DAO.COLUMN_CONFIG_DATE_FORMAT_INDEX, dateFormatIndex)
        values.put( DAO.COLUMN_CONFIG_TIME_FORMAT_INDEX, timeFormatIndex)
        values.put( DAO.COLUMN_CONFIG_DISTANCE_FORMAT_INDEX, distanceFormatIndex)
    }

    fun putConfigStudy(config: Config, study: Study, values: ContentValues )
    {
        values.put(DAO.COLUMN_CONFIG_UUID, config.uuid)
        values.put(DAO.COLUMN_STUDY_UUID, study.uuid)
    }

    @SuppressLint("Range")
    private fun buildConfig(cursor: Cursor ) : Config
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val selectedEnumAreaUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_UUID))
        val selectedStudyUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val timeZone = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_TIME_ZONE))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_NAME))
        val dbVersion = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DB_VERSION))
        val mapEngineIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_MAP_ENGINE_INDEX))
        val distanceFormatIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DISTANCE_FORMAT_INDEX))
        val dateFormatIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DATE_FORMAT_INDEX))
        val timeFormatIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_TIME_FORMAT_INDEX))
        val minGpsPrecision = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_MIN_GPS_PRECISION))
        val encryptionPassword = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_ENCRYPTION_PASSWORD))
        val allowManualLocationEntry = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_ALLOW_MANUAL_LOCATION_ENTRY)).toBoolean()
        val subaddressIsRequired = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_SUBADDRESS_IS_REQUIRED)).toBoolean()
        val autoIncrementSubaddress = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_AUTO_INCREMENT_SUBADDRESS)).toBoolean()
        val proximityWarningIsEnabled = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_PROXIMITY_WARNING_IS_ENABLED)).toBoolean()
        val proximityWarningValue = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_PROXIMITY_WARNING_VALUE))
        var validUsers = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_VALID_USERS))

        // HACK!!! is this really necessary?
        if (validUsers.isEmpty())
        {
            MainApplication.instance.user?.let { user->
                validUsers = " ${user.uuid} "
            }
        }

        val distanceFormat = DistanceFormatConverter.fromIndex(distanceFormatIndex)
        val dateFormat = DateFormatConverter.fromIndex(dateFormatIndex)
        val timeFormat = TimeFormatConverter.fromIndex(timeFormatIndex)

        return Config( uuid, creationDate, timeZone, name, dbVersion, mapEngineIndex, dateFormat, timeFormat, distanceFormat, minGpsPrecision, encryptionPassword, allowManualLocationEntry, subaddressIsRequired, autoIncrementSubaddress, proximityWarningIsEnabled, proximityWarningValue, selectedStudyUuid, selectedEnumAreaUuid, validUsers, version )
    }

    fun getConfig( uuid: String ): Config?
    {
        var config: Config? = null

        val query = "SELECT * FROM ${DAO.TABLE_CONFIG} WHERE ${DAO.COLUMN_UUID} = '$uuid'"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            config = buildConfig( cursor )
            config.studies = DAO.studyDAO.getStudies( config )
        }

        cursor.close()

        return config
    }

    fun getMinimalConfigs(): ArrayList<Config>
    {
        val configs = ArrayList<Config>()

        MainApplication.instance.user?.let { user ->
            val query = "SELECT * FROM ${DAO.TABLE_CONFIG} ORDER BY ${DAO.COLUMN_CREATION_DATE}"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val config = buildConfig(cursor)

                if (config.validUsers.contains(user.uuid ))
                {
                    configs.add( config)
                }
            }

            cursor.close()
        }

        return configs
    }

    private fun createOrUpdateStudies(config : Config)
    {
        // remove all studies from connector table
        val whereClause = "${DAO.COLUMN_CONFIG_UUID} = ?"
        val args = arrayOf(config.uuid)

        dao.writableDatabase.delete(DAO.CONNECTOR_TABLE_CONFIG__STUDY, whereClause, args)

        // add studies
        for(study in config.studies)
        {
            // study will either be created or updated
            DAO.studyDAO.createOrUpdateStudy(study, study.version)?.let { study
                val configStudyValues = ContentValues()
                putConfigStudy(config, study, configStudyValues)
                dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_CONFIG__STUDY, null, configStudyValues).toInt()
            }
        }
    }

    private fun createOrUpdateEnumAreas(config: Config)
    {
        for (enumArea in config.enumAreas)
        {
            DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea, enumArea.version )
        }
    }

    data class ConfigSummary(
        val enumAreaCount: Int,
        val enumerationCount: Int,
        val eligibleCount: Int,
        val sampledCount: Int,
        val surveyedCount: Int
    )

    fun getConfigSummary(configUuid: String): ConfigSummary {

        val db = dao.readableDatabase

        val enumAreaCount = DatabaseUtils.longForQuery(db,"""
            SELECT COUNT(*)
                FROM enum_area
                WHERE config_uuid = ?
            """.trimIndent(), arrayOf(configUuid)).toInt()

        val query = """
            SELECT
                SUM(CASE
                    WHEN ei.enumeration_item_enumeration_state IN ('Enumerated', 'Incomplete')
                    THEN 1 ELSE 0 END) AS enumeration_count,

                SUM(CASE
                    WHEN ei.enumeration_item_enumeration_eligible_for_sampling = 1
                    OR ei.enumeration_item_enumeration_eligible_for_subset_sampling = 1
                    THEN 1 ELSE 0 END) AS eligible_count,

                SUM(CASE
                    WHEN ei.enumeration_item_sampling_state = 'Sampled'
                    OR ei.enumeration_item_subset_sampling_state = 'Sampled'
                THEN 1 ELSE 0 END) AS sampled_count,

                SUM(CASE
                    WHEN ei.enumeration_item_collection_state = 'Complete'
                THEN 1 ELSE 0 END) AS surveyed_count

            FROM enumeration_item ei
            JOIN location l
                ON ei.location_uuid = l.uuid
            JOIN location__enum_area lea
                ON lea.location_uuid = l.uuid
            JOIN enum_area ea
                ON ea.uuid = lea.enum_area_uuid
            WHERE ea.config_uuid = ?
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(configUuid))

        var result = ConfigSummary(
            enumAreaCount = 0,
            enumerationCount = 0,
            eligibleCount = 0,
            sampledCount = 0,
            surveyedCount = 0
        )

        cursor.use { c ->
            if (c.moveToFirst()) {

                val enumIndex = c.getColumnIndexOrThrow("enumeration_count")
                val eligibleIndex = c.getColumnIndexOrThrow("eligible_count")
                val sampledIndex = c.getColumnIndexOrThrow("sampled_count")
                val surveyedIndex = c.getColumnIndexOrThrow("surveyed_count")

                result = ConfigSummary(
                    enumAreaCount = enumAreaCount,
                    enumerationCount = c.getInt(enumIndex),
                    eligibleCount = c.getInt(eligibleIndex),
                    sampledCount = c.getInt(sampledIndex),
                    surveyedCount = c.getInt(surveyedIndex)
                )
            }
        }

        return result
    }

    fun deleteConfig( config: Config )
    {
        dao.writableDatabase.beginTransaction()

        val studies = DAO.studyDAO.getStudies( config )
        for (study in studies)
        {
            DAO.studyDAO.deleteStudy( study )
        }

        val enumAreas = DAO.enumAreaDAO.getEnumAreas( config )
        for (enumArea in enumAreas)
        {
            DAO.enumAreaDAO.delete( enumArea )
        }

        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(config.uuid)

        dao.writableDatabase.delete(DAO.TABLE_CONFIG, whereClause, args)

        PreferencesManager.removeAllHashes(config.uuid )

        dao.writableDatabase.setTransactionSuccessful()
        dao.writableDatabase.endTransaction()
    }

    companion object
    {
        val columnBindings = listOf(
            ColumnBinding<Config>(COLUMN_CREATION_DATE, "INTEGER",Config::creationDate),
            ColumnBinding<Config>(COLUMN_VERSION,"TEXT",Config::version ),
            ColumnBinding<Config>(COLUMN_TIME_ZONE,"INTEGER",Config::timeZone),
            ColumnBinding<Config>(COLUMN_CONFIG_NAME,"TEXT UNIQUE NOT NULL",Config::name),
            ColumnBinding<Config>(COLUMN_CONFIG_DB_VERSION,"INTEGER",Config::creationDate),
            ColumnBinding<Config>(COLUMN_CONFIG_MAP_ENGINE_INDEX,"INTEGER",Config::mapEngineIndex),
            ColumnBinding<Config>(COLUMN_CONFIG_DATE_FORMAT_INDEX,"INTEGER",{ DateFormatConverter.toIndex(it.dateFormat) }),
            ColumnBinding<Config>(COLUMN_CONFIG_TIME_FORMAT_INDEX,"INTEGER",{ TimeFormatConverter.toIndex(it.timeFormat) }),
            ColumnBinding<Config>(COLUMN_CONFIG_DISTANCE_FORMAT_INDEX,"INTEGER",{ DistanceFormatConverter.toIndex(it.distanceFormat) }),
            ColumnBinding<Config>(COLUMN_CONFIG_MIN_GPS_PRECISION,"INTEGER",Config::minGpsPrecision),
            ColumnBinding<Config>(COLUMN_CONFIG_ENCRYPTION_PASSWORD,"TEXT",Config::encryptionPassword),
            ColumnBinding<Config>(COLUMN_CONFIG_ALLOW_MANUAL_LOCATION_ENTRY,"INTEGER",Config::allowManualLocationEntry),
            ColumnBinding<Config>(COLUMN_CONFIG_SUBADDRESS_IS_REQUIRED,"INTEGER",Config::subaddressIsrequired),
            ColumnBinding<Config>(COLUMN_CONFIG_AUTO_INCREMENT_SUBADDRESS,"INTEGER",Config::autoIncrementSubaddress),
            ColumnBinding<Config>(COLUMN_CONFIG_PROXIMITY_WARNING_IS_ENABLED,"INTEGER",Config::proximityWarningIsEnabled),
            ColumnBinding<Config>(COLUMN_CONFIG_PROXIMITY_WARNING_VALUE,"INTEGER",Config::proximityWarningValue),
            ColumnBinding<Config>(COLUMN_ENUM_AREA_UUID,"TEXT",Config::selectedEnumAreaUuid),
            ColumnBinding<Config>(COLUMN_STUDY_UUID,"TEXT",Config::selectedStudyUuid),
            ColumnBinding<Config>(COLUMN_CONFIG_VALID_USERS,"TEXT",Config::validUsers),
        )

//        val sql = ColumnBinding.createUpsertSQL(DAO.TABLE_CONFIG, columnBindings )
//        val upsertStatement = DAO.instance().writableDatabase.compileStatement( sql )
    }
}