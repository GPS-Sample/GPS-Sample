package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.database.models.Config
import kotlin.math.min

class ConfigDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createConfig( config: Config ) : Int
    {
        val values = ContentValues()

        putConfig( config, values )

        return dao.writableDatabase.insert(DAO.TABLE_CONFIG, null, values).toInt()
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
            config = createConfigModel( cursor )
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
    private fun createConfigModel( cursor: Cursor ) : Config
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_NAME))
        val distanceFormat = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DISTANCE_FORMAT))
        val dateFormat = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DATE_FORMAT))
        val timeFormat = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_TIME_FORMAT))
        val minGpsPrecision = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_MIN_GPS_PRECISION))

        return Config( uuid, name, dateFormat, timeFormat, distanceFormat, minGpsPrecision )
    }

    //--------------------------------------------------------------------------
    fun getConfigs(): List<Config>
    {
        val configs = ArrayList<Config>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_CONFIG}"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            configs.add( createConfigModel( cursor ))
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

    //--------------------------------------------------------------------------
    fun updateConfig( config: Config )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(config.uuid)
        val values = ContentValues()

        putConfig( config, values )

        db.update(DAO.TABLE_CONFIG, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteConfig( config: Config )
    {
        val studies = DAO.studyDAO.getStudies( config.uuid )

        for (study in studies)
        {
            DAO.studyDAO.deleteStudy( study )
        }

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
}