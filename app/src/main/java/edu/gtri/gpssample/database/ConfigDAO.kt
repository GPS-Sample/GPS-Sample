package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Study
import kotlin.math.min

class ConfigDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createConfig( config: Config ) : Boolean
    {
        val values = ContentValues()

        putConfig( config, values )
        val id = dao.writableDatabase.insert(DAO.TABLE_CONFIG, null, values).toInt()
        if (id > -1)
        {
            config.id = id
            // add studies
            updateStudies(config)
            return true
        }
        return false
    }

    //--------------------------------------------------------------------------
    fun getConfig( id: Int ): Config?
    {
        var config: Config? = null

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_CONFIG} WHERE ${DAO.COLUMN_ID} = $id"

        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            config = buildConfig( cursor )
//            // find studies
//            val query = "SELECT * FROM ${DAO.TABLE_CONFIG} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
//
//            val cursor = db.rawQuery(query, null)
        }

        cursor.close()
        db.close()

        return config
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun buildConfig(cursor: Cursor ) : Config
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_NAME))
        val distanceFormatIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DISTANCE_FORMAT_INDEX))
        val dateFormatIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DATE_FORMAT_INDEX))
        val timeFormatIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_TIME_FORMAT_INDEX))
        val minGpsPrecision = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_MIN_GPS_PRECISION))

        // TODO: these should be lookup tables
        val distanceFormat = DistanceFormatConverter.fromIndex(distanceFormatIndex)
        val dateFormat = DateFormatConverter.fromIndex(dateFormatIndex)
        val timeFormat = TimeFormatConverter.fromIndex(timeFormatIndex)

        return Config( id, name, dateFormat, timeFormat, distanceFormat, minGpsPrecision )
    }

    //--------------------------------------------------------------------------
    fun getConfigs(): List<Config>
    {
        val configs = ArrayList<Config>()
        var db = dao.writableDatabase
        var query = "SELECT * FROM ${DAO.TABLE_CONFIG}"

        var cursor = db.rawQuery(query, null)
        Log.d("xxx","Cursor count ${cursor.count}")
        while (cursor.moveToNext())
        {
            val config = buildConfig( cursor )

            // find studies
            config.id?.let{id ->
                config.studies = DAO.studyDAO.getStudies(config)
                Log.d("DAO", "")
            }

            configs.add( config)
        }


        db = dao.writableDatabase
        for(config in configs)
        {
            query = "SELECT * FROM ${DAO.TABLE_CONFIG_STUDY} WHERE ${DAO.COLUMN_CONFIG_ID} = '${config.id}'"

            cursor = db.rawQuery(query, null)

            while(cursor.moveToNext())
            {
                val coid = cursor.getColumnIndex(DAO.COLUMN_CONFIG_ID)
                val stid = cursor.getColumnIndex(DAO.COLUMN_STUDY_ID)
                val conid = cursor.getInt(coid)
                val stuid = cursor.getInt(stid)
                Log.d("xxxx CON", "the config id ${conid} and study id ${stuid}")
            }
        }

        cursor.close()
        db.close()

        return configs
    }

    //--------------------------------------------------------------------------
    fun putConfig( config: Config, values: ContentValues )
    {
        values.put( DAO.COLUMN_CONFIG_NAME, config.name )
        values.put( DAO.COLUMN_CONFIG_MIN_GPS_PRECISION, config.minGpsPrecision )

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

        values.put(DAO.COLUMN_CONFIG_ID, config.id)
        values.put(DAO.COLUMN_STUDY_ID, study.id)
    }

    //--------------------------------------------------------------------------
    fun updateConfig( config: Config )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        config.id?.let { id ->
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putConfig( config, values )

            db.update(DAO.TABLE_CONFIG, values, whereClause, args )
            // update studies
            updateStudies(config)
            db.close()
        }
    }

    //--------------------------------------------------------------------------
    fun deleteConfig( config: Config )
    {
//        val studies = DAO.studyDAO.getStudies( config.uuid )
//
//        for (study in studies)
//        {
//            DAO.studyDAO.deleteStudy( study )
//        }

        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(config.id.toString())

        db.delete(DAO.TABLE_CONFIG, whereClause, args)
        db.close()
    }

    private fun updateStudies(config : Config)
    {
        // check the id.  if we get here and the id is null, there's a problem.
        // TODO: add some error checking
        config.id?.let{id ->

            // remove all studies from connector table
            var db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_CONFIG_ID} = ?"
            val args = arrayOf(id.toString())

            db.delete(DAO.TABLE_CONFIG_STUDY, whereClause, args)

            // add studies
            for(study in config.studies)
            {
                var study_id = -1
                // study will either be created or updated
                study_id = DAO.studyDAO.createStudy(study)
                if(study_id > -1)
                {
                    study.id = study_id
                    val configStudyValues = ContentValues()
                    putConfigStudy(config, study, configStudyValues)
                    dao.writableDatabase.insert(DAO.TABLE_CONFIG_STUDY, null,
                        configStudyValues).toInt()
                }

            }

        }
    }
}