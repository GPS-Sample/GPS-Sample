package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.models.Configuration

class ConfigDAO(private var dao: GPSSampleDAO)
{
    //--------------------------------------------------------------------------
    fun createConfig( configuration: Configuration) : Int
    {
        val values = ContentValues()

        values.put(GPSSampleDAO.COLUMN_CONFIG_NAME, configuration.name )
        values.put(GPSSampleDAO.COLUMN_CONFIG_DISTANCE_FORMAT, configuration.distanceFormat.toString() )
        values.put(GPSSampleDAO.COLUMN_CONFIG_DATE_FORMAT, configuration.dateFormat.toString() )
        values.put(GPSSampleDAO.COLUMN_CONFIG_TIME_FORMAT, configuration.timeFormat.toString() )
        values.put(GPSSampleDAO.COLUMN_CONFIG_MIN_GPS_PRECISION, configuration.minGpsPrecision )

        return dao.writableDatabase.insert(GPSSampleDAO.TABLE_CONFIG, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun getConfig( id: Int ): Configuration?
    {
        var config: Configuration? = null

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${GPSSampleDAO.TABLE_CONFIG} WHERE ${GPSSampleDAO.COLUMN_ID} = $id"

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
    fun createConfig( cursor: Cursor) : Configuration
    {
        val config = Configuration()

        config.id = Integer.parseInt(cursor.getString(0))
        config.name = cursor.getString(cursor.getColumnIndex(GPSSampleDAO.COLUMN_CONFIG_NAME))
        config.distanceFormat = DistanceFormat.valueOf(cursor.getString(cursor.getColumnIndex(
            GPSSampleDAO.COLUMN_CONFIG_DISTANCE_FORMAT
        )))
        config.dateFormat = DateFormat.valueOf(cursor.getString(cursor.getColumnIndex(GPSSampleDAO.COLUMN_CONFIG_DATE_FORMAT)))
        config.timeFormat = TimeFormat.valueOf(cursor.getString(cursor.getColumnIndex(GPSSampleDAO.COLUMN_CONFIG_TIME_FORMAT)))
        config.minGpsPrecision = cursor.getInt(cursor.getColumnIndex(GPSSampleDAO.COLUMN_CONFIG_MIN_GPS_PRECISION))

        return config
    }

    //--------------------------------------------------------------------------
    fun getConfigurations(): List<Configuration>
    {
        val configurations = ArrayList<Configuration>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${GPSSampleDAO.TABLE_CONFIG}"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            configurations.add( createConfig( cursor ))
        }

        cursor.close()
        db.close()

        return configurations
    }

    //--------------------------------------------------------------------------
    fun updateConfig( configuration: Configuration)
    {
        val db = dao.writableDatabase
        val whereClause = "${GPSSampleDAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(configuration.id.toString())

        val values = ContentValues()

        values.put(GPSSampleDAO.COLUMN_CONFIG_NAME, configuration.name )
        values.put(GPSSampleDAO.COLUMN_CONFIG_DISTANCE_FORMAT, configuration.distanceFormat.toString())
        values.put(GPSSampleDAO.COLUMN_CONFIG_DATE_FORMAT, configuration.dateFormat.toString())
        values.put(GPSSampleDAO.COLUMN_CONFIG_TIME_FORMAT, configuration.timeFormat.toString())
        values.put(GPSSampleDAO.COLUMN_CONFIG_MIN_GPS_PRECISION, configuration.minGpsPrecision )

        db.update(GPSSampleDAO.TABLE_CONFIG, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteConfig( configuration: Configuration)
    {
        val studies = GPSSampleDAO.studyDAO.getStudies( configuration.id )

        for (study in studies)
        {
            GPSSampleDAO.studyDAO.deleteStudy( study )
        }

        val db = dao.writableDatabase
        val whereClause = "${GPSSampleDAO.COLUMN_ID} = ?"
        val args = arrayOf(configuration.id.toString())

        db.delete(GPSSampleDAO.TABLE_CONFIG, whereClause, args)
        db.close()
    }
}