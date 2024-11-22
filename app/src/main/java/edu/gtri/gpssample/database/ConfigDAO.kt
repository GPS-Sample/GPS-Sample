package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.extensions.toBoolean
import kotlin.math.min

class ConfigDAO(private var dao: DAO)
{
    fun createOrUpdateConfig( config: Config ) : Config?
    {
        val existingConfig = getConfig( config.uuid )

        if (existingConfig != null)
        {
            if (config.doesNotEqual( existingConfig ))
            {
                updateConfig( config )
                Log.d( "xxx", "Updated Config with ID ${config.uuid}" )
            }
        }
        else
        {
            val values = ContentValues()
            putConfig( config, values )
            if (dao.writableDatabase.insert(DAO.TABLE_CONFIG, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "Created Config with ID = ${config.uuid}")
        }

        createOrUpdateEnumAreas(config)
        createOrUpdateStudies(config)

        return config
    }

    fun updateConfig( config: Config )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(config.uuid)
        val values = ContentValues()
        putConfig( config, values )
        dao.writableDatabase.update(DAO.TABLE_CONFIG, values, whereClause, args )
    }

    fun putConfig( config: Config, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, config.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, config.creationDate )
        values.put( DAO.COLUMN_TIME_ZONE, config.timeZone )
        values.put( DAO.COLUMN_ENUM_AREA_UUID, config.selectedEnumAreaUuid )
        values.put( DAO.COLUMN_STUDY_UUID, config.selectedStudyUuid )
        values.put( DAO.COLUMN_CONFIG_NAME, config.name )
        values.put( DAO.COLUMN_CONFIG_DB_VERSION, config.dbVersion )
        values.put( DAO.COLUMN_CONFIG_MIN_GPS_PRECISION, config.minGpsPrecision )
        values.put( DAO.COLUMN_CONFIG_ENCRYPTION_PASSWORD, config.encryptionPassword )
        values.put( DAO.COLUMN_CONFIG_ALLOW_MANUAL_LOCATION_ENTRY, config.allowManualLocationEntry )
        values.put( DAO.COLUMN_CONFIG_SUBADDRESS_IS_REQUIRED, config.subaddressIsrequired )
        values.put( DAO.COLUMN_CONFIG_AUTO_INCREMENT_SUBADDRESS, config.autoIncrementSubaddress )
        values.put( DAO.COLUMN_CONFIG_PROXIMITY_WARNING_IS_ENABLED, config.proximityWarningIsEnabled )
        values.put( DAO.COLUMN_CONFIG_PROXIMITY_WARNING_VALUE, config.proximityWarningValue )

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

    fun exists( config: Config ): Boolean
    {
        getConfig( config.uuid )?.let {
            return true
        } ?: return false
    }

    @SuppressLint("Range")
    private fun buildConfig(cursor: Cursor ) : Config
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val selectedEnumAreaUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_UUID))
        val selectedStudyUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val timeZone = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_TIME_ZONE))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_NAME))
        val dbVersion = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DB_VERSION))
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

        // TODO: these should be lookup tables
        val distanceFormat = DistanceFormatConverter.fromIndex(distanceFormatIndex)
        val dateFormat = DateFormatConverter.fromIndex(dateFormatIndex)
        val timeFormat = TimeFormatConverter.fromIndex(timeFormatIndex)

        return Config( uuid, creationDate, timeZone, name, dbVersion, dateFormat, timeFormat, distanceFormat, minGpsPrecision, encryptionPassword, allowManualLocationEntry, subaddressIsRequired, autoIncrementSubaddress, proximityWarningIsEnabled, proximityWarningValue, selectedStudyUuid, selectedEnumAreaUuid )
    }

    fun getConfig( uuid: String ): Config?
    {
        var config: Config? = null

        val query = "SELECT * FROM ${DAO.TABLE_CONFIG} WHERE ${DAO.COLUMN_UUID} = '$uuid'"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            config = buildConfig( cursor )
            config.studies = DAO.studyDAO.getStudies( config )
            config.enumAreas = DAO.enumAreaDAO.getEnumAreas( config )
        }

        cursor.close()

        return config
    }

    fun getConfigs(): ArrayList<Config>
    {
        val configs = ArrayList<Config>()
        val query = "SELECT * FROM ${DAO.TABLE_CONFIG}"

        val cursor = dao.writableDatabase.rawQuery(query, null)
        while (cursor.moveToNext())
        {
            val config = buildConfig( cursor )
            config.studies = DAO.studyDAO.getStudies( config )
            config.enumAreas = DAO.enumAreaDAO.getEnumAreas( config )

            configs.add( config)
        }

        cursor.close()

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
            DAO.studyDAO.createOrUpdateStudy(study)?.let { study
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
            DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea )
        }
    }

    fun deleteConfig( config: Config )
    {
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
    }
}