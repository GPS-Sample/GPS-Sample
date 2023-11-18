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
    //--------------------------------------------------------------------------
    fun createConfig( config: Config ) : Config?
    {
        if (exists( config ))
        {
            // if we need to handle the update case here,
            // let's change the name to createOrUpdateConfig and do the update here
            Log.d( "xxx", "Oops! config with id ${config.id!!} already exists")
            return null
        }
        else
        {
            val values = ContentValues()
            putConfig( config, values )
            config.id = dao.writableDatabase.insert(DAO.TABLE_CONFIG, null, values).toInt()
            config.id?.let { id ->
                Log.d( "xxx", "new config id = ${id}")
                createOrUpdateEnumAreas(config)
                createOrUpdateStudies(config)
                return config
            } ?: return null
        }
    }

    //--------------------------------------------------------------------------
    fun putConfig( config: Config, values: ContentValues )
    {
        config.id?.let { id ->
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_ENUM_AREA_ID, config.selectedEnumAreaId )
        values.put( DAO.COLUMN_STUDY_ID, config.selectedStudyId )

        values.put( DAO.COLUMN_CREATION_DATE, config.creationDate )
        values.put( DAO.COLUMN_CONFIG_NAME, config.name )
        values.put( DAO.COLUMN_CONFIG_MIN_GPS_PRECISION, config.minGpsPrecision )
        values.put( DAO.COLUMN_CONFIG_MIN_GPS_PRECISION, config.minGpsPrecision )
        values.put( DAO.COLUMN_CONFIG_ALLOW_MANUAL_LOCATION_ENTRY, config.allowManualLocationEntry )

        // TODO: these should be from lookup tables
        val dateFormatIndex = DateFormatConverter.toIndex(config.dateFormat)
        val timeFormatIndex = TimeFormatConverter.toIndex(config.timeFormat)
        val distanceFormatIndex = DistanceFormatConverter.toIndex(config.distanceFormat)

        values.put( DAO.COLUMN_CONFIG_DATE_FORMAT_INDEX, dateFormatIndex)
        values.put( DAO.COLUMN_CONFIG_TIME_FORMAT_INDEX, timeFormatIndex)
        values.put( DAO.COLUMN_CONFIG_DISTANCE_FORMAT_INDEX, distanceFormatIndex)
    }

    //--------------------------------------------------------------------------
    fun putConfigStudy(config: Config, study: Study, values: ContentValues )
    {
        values.put(DAO.COLUMN_CONFIG_ID, config.id)
        values.put(DAO.COLUMN_STUDY_ID, study.id)
    }

    //--------------------------------------------------------------------------
    fun exists( config: Config ): Boolean
    {
        config.id?.let { id ->
            getConfig( id )?.let { config
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    fun getConfig( id: Int ): Config?
    {
        var config: Config? = null

        val query = "SELECT * FROM ${DAO.TABLE_CONFIG} WHERE ${DAO.COLUMN_ID} = $id"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            config = buildConfig( cursor )

            config.id?.let{id ->
                config.studies = DAO.studyDAO.getStudies(config)
                config.enumAreas = DAO.enumAreaDAO.getEnumAreas(config)
            }
        }

        cursor.close()

        return config
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun buildConfig(cursor: Cursor ) : Config
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val selectedEnumAreaId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_ID))
        val selectedStudyId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_NAME))
        val distanceFormatIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DISTANCE_FORMAT_INDEX))
        val dateFormatIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DATE_FORMAT_INDEX))
        val timeFormatIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_TIME_FORMAT_INDEX))
        val minGpsPrecision = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_MIN_GPS_PRECISION))
        val allowManualLocationEntry = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_ALLOW_MANUAL_LOCATION_ENTRY)).toBoolean()

        // TODO: these should be lookup tables
        val distanceFormat = DistanceFormatConverter.fromIndex(distanceFormatIndex)
        val dateFormat = DateFormatConverter.fromIndex(dateFormatIndex)
        val timeFormat = TimeFormatConverter.fromIndex(timeFormatIndex)

        return Config( id, creationDate, name, dateFormat, timeFormat, distanceFormat, minGpsPrecision, allowManualLocationEntry, selectedStudyId, selectedEnumAreaId )
    }

    //--------------------------------------------------------------------------
    fun getConfigs(): List<Config>
    {
        val configs = ArrayList<Config>()
        val query = "SELECT * FROM ${DAO.TABLE_CONFIG}"

        val cursor = dao.writableDatabase.rawQuery(query, null)
        while (cursor.moveToNext())
        {
            val config = buildConfig( cursor )

            config.id?.let{id ->
                config.studies = DAO.studyDAO.getStudies(config)
                config.enumAreas = DAO.enumAreaDAO.getEnumAreas(config)
            }

            configs.add( config)
        }

        cursor.close()

        return configs
    }

    //--------------------------------------------------------------------------
    fun updateConfig( config: Config )
    {
        if (!exists( config ))
        {
            createConfig(config)
        }
        else
        {
            val whereClause = "${DAO.COLUMN_ID} = ?"
            config.id?.let { id ->
                val args: Array<String> = arrayOf(id.toString())
                val values = ContentValues()
                putConfig( config, values )
                dao.writableDatabase.update(DAO.TABLE_CONFIG, values, whereClause, args )
                createOrUpdateEnumAreas(config)
                createOrUpdateStudies(config)
            }
        }
    }

    private fun createOrUpdateStudies(config : Config)
    {
        // check the id.  if we get here and the id is null, there's a problem.
        // TODO: add some error checking
        config.id?.let{id ->

            // remove all studies from connector table
            val whereClause = "${DAO.COLUMN_CONFIG_ID} = ?"
            val args = arrayOf(id.toString())

            dao.writableDatabase.delete(DAO.TABLE_CONFIG_STUDY, whereClause, args)

            // add studies
            for(study in config.studies)
            {
                // study will either be created or updated
                DAO.studyDAO.createOrUpdateStudy(study)?.let { study
                    val configStudyValues = ContentValues()
                    putConfigStudy(config, study, configStudyValues)
                    dao.writableDatabase.insert(DAO.TABLE_CONFIG_STUDY, null, configStudyValues).toInt()
                }
            }
        }
    }

    private fun createOrUpdateEnumAreas(config: Config)
    {
        config.id?.let { id ->

            for (enumArea in config.enumAreas)
            {
                DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea, config )
            }
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

        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(config.id.toString())

        dao.writableDatabase.delete(DAO.TABLE_CONFIG, whereClause, args)
    }
}