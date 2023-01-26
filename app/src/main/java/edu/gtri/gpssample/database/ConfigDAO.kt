package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.models.Config

class ConfigDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createConfig( config: Config ) : Int
    {
        val values = ContentValues()

        values.put(DAO.COLUMN_CONFIG_NAME, config.name )
        values.put(DAO.COLUMN_CONFIG_DISTANCE_FORMAT, config.distanceFormat.toString() )
        values.put(DAO.COLUMN_CONFIG_DATE_FORMAT, config.dateFormat.toString() )
        values.put(DAO.COLUMN_CONFIG_TIME_FORMAT, config.timeFormat.toString() )
        values.put(DAO.COLUMN_CONFIG_MIN_GPS_PRECISION, config.minGpsPrecision )

        return dao.writableDatabase.insert(DAO.TABLE_CONFIG, null, values).toInt()
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

            config = createConfig( cursor )
        }

        cursor.close()
        db.close()

        return config
    }

    //--------------------------------------------------------------------------
    fun createConfig( cursor: Cursor) : Config
    {
        val config = Config()

        config.id = Integer.parseInt(cursor.getString(0))
        config.name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_NAME))
        config.distanceFormat = DistanceFormat.valueOf(cursor.getString(cursor.getColumnIndex(
            DAO.COLUMN_CONFIG_DISTANCE_FORMAT
        )))
        config.dateFormat = DateFormat.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_DATE_FORMAT)))
        config.timeFormat = TimeFormat.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_CONFIG_TIME_FORMAT)))
        config.minGpsPrecision = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_MIN_GPS_PRECISION))

        return config
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
            configs.add( createConfig( cursor ))
        }

        cursor.close()
        db.close()

        return configs
    }

    //--------------------------------------------------------------------------
    fun updateConfig( config: Config)
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(config.id.toString())

        val values = ContentValues()

        values.put(DAO.COLUMN_CONFIG_NAME, config.name )
        values.put(DAO.COLUMN_CONFIG_DISTANCE_FORMAT, config.distanceFormat.toString())
        values.put(DAO.COLUMN_CONFIG_DATE_FORMAT, config.dateFormat.toString())
        values.put(DAO.COLUMN_CONFIG_TIME_FORMAT, config.timeFormat.toString())
        values.put(DAO.COLUMN_CONFIG_MIN_GPS_PRECISION, config.minGpsPrecision )

        db.update(DAO.TABLE_CONFIG, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteConfig( config: Config)
    {
        val studies = DAO.studyDAO.getStudies( config.id )

        for (study in studies)
        {
            DAO.studyDAO.deleteStudy( study )
        }

        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(config.id.toString())

        db.delete(DAO.TABLE_CONFIG, whereClause, args)
        db.close()
    }
}