package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.TimeFormat
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
    fun getConfig( uuid: String ): Config?
    {
        var config: Config? = null

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_CONFIG} WHERE ${DAO.COLUMN_UUID} = '$uuid'"

        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            config = createConfig( cursor )
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
    fun exists( uuid: String ) : Boolean
    {
        return getConfig( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createConfig(cursor: Cursor ) : Config
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_NAME))
        val distanceFormat = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DISTANCE_FORMAT))
        val dateFormat = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DATE_FORMAT))
        val timeFormat = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_TIME_FORMAT))
        val minGpsPrecision = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_MIN_GPS_PRECISION))

        return Config( id, uuid, name, dateFormat, timeFormat, distanceFormat, minGpsPrecision )
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
            val config = createConfig( cursor )

            // find studies
            config.id?.let{id ->
                config.studies = DAO.studyDAO.getStudies(id)
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
        values.put( DAO.COLUMN_UUID, config.uuid )
        values.put( DAO.COLUMN_CONFIG_NAME, config.name )
        values.put( DAO.COLUMN_CONFIG_DATE_FORMAT, config.dateFormat)
        values.put( DAO.COLUMN_CONFIG_TIME_FORMAT, config.timeFormat)
        values.put( DAO.COLUMN_CONFIG_DISTANCE_FORMAT, config.distanceFormat)
        values.put( DAO.COLUMN_CONFIG_MIN_GPS_PRECISION, config.minGpsPrecision )
    }

    fun putConfigStudy(config: Config, study: Study, values: ContentValues )
    {

        values.put(DAO.COLUMN_CONFIG_ID, config.id)
        values.put(DAO.COLUMN_STUDY_ID, study.id)
//        values.put( DAO.COLUMN_UUID, config.uuid )
//        values.put( DAO.COLUMN_CONFIG_NAME, config.name )
//        values.put( DAO.COLUMN_CONFIG_DATE_FORMAT, config.dateFormat)
//        values.put( DAO.COLUMN_CONFIG_TIME_FORMAT, config.timeFormat)
//        values.put( DAO.COLUMN_CONFIG_DISTANCE_FORMAT, config.distanceFormat)
//        values.put( DAO.COLUMN_CONFIG_MIN_GPS_PRECISION, config.minGpsPrecision )
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
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(config.uuid.toString())

        db.delete(DAO.TABLE_CONFIG, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteAllConfigs()
    {
        val studies = DAO.studyDAO.getStudies()

        for (study in studies)
        {
            DAO.studyDAO.deleteStudy( study )
        }

        val configs = DAO.configDAO.getConfigs()

        for (config in configs)
        {
            DAO.configDAO.deleteConfig( config )
        }
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
                study.id?.let {id ->
                    DAO.studyDAO.updateStudy(study)
                    study.id?.let{id ->
                        study_id = id
                    }

                }?: run{
                    study_id = DAO.studyDAO.createStudy(study)

                }
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