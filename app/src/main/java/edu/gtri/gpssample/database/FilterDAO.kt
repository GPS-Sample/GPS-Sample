package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.extensions.toBoolean

class FilterDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createFilter( filter: Filter ) : Int
    {
        val values = ContentValues()

        putFilter( filter, values )

        return dao.writableDatabase.insert(DAO.TABLE_FILTER, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putFilter( filter: Filter, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, filter.uuid )
        values.put( DAO.COLUMN_FILTER_STUDY_UUID, filter.study_uuid )
        values.put( DAO.COLUMN_FILTER_NAME, filter.name )
    }

    //--------------------------------------------------------------------------
    fun updateFilter( filter: Filter )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(filter.uuid.toString())
        val values = ContentValues()

        putFilter( filter, values )

        db.update(DAO.TABLE_FILTER, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getFilter( filter_uuid: String ) : Filter?
    {
        var filter: Filter? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_UUID} = '$filter_uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            filter = createFilterModel( cursor )
        }

        cursor.close()
        db.close()

        return filter
    }

    //--------------------------------------------------------------------------
    private fun  createFilterModel( cursor: Cursor ): Filter
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val study_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_STUDY_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_NAME))

        return Filter( uuid, study_uuid, name )
    }

    //--------------------------------------------------------------------------
    fun getFilters( study_uuid: String ): List<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_FILTER_STUDY_UUID} = '$study_uuid'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filters.add( createFilterModel( cursor ))
        }

        cursor.close()
        db.close()

        return filters
    }

    //--------------------------------------------------------------------------
    fun getFilters(): List<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filters.add( createFilterModel( cursor ))
        }

        cursor.close()
        db.close()

        return filters
    }

    //--------------------------------------------------------------------------
    fun deleteFilter( filter: Filter )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(filter.uuid)

        db.delete(DAO.TABLE_FILTER, whereClause, args)
        db.close()
    }
}