package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.extensions.toBoolean

class FilterDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createFilter( filter: Filter) : Int
    {
        val values = ContentValues()

        putFilter( filter, values )

        return dao.writableDatabase.insert(DAO.TABLE_FILTER, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putFilter(filter: Filter, values: ContentValues)
    {
        values.put( DAO.COLUMN_FILTER_STUDY_ID, filter.studyId )
        values.put( DAO.COLUMN_FILTER_NAME, filter.name )
        values.put( DAO.COLUMN_FILTER_IS_VALID, filter.isValid )
    }

    //--------------------------------------------------------------------------
    fun updateFilter( filter: Filter)
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(filter.id.toString())
        val values = ContentValues()

        putFilter( filter, values )

        db.update(DAO.TABLE_FILTER, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getFilter( filterId: Int ) : Filter?
    {
        var filter: Filter? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_ID} = $filterId"
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
    private fun  createFilterModel( cursor: Cursor): Filter
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val studyId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_STUDY_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_NAME))
        val isValid = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_IS_VALID)).toBoolean()

        return Filter( id, studyId, name, isValid )
    }

    //--------------------------------------------------------------------------
    fun getValidFilters( studyId: Int ): List<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_FILTER_STUDY_ID} = $studyId AND ${DAO.COLUMN_FILTER_IS_VALID} = 1"
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
    fun getInvalidFilters( studyId: Int ): List<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_FILTER_STUDY_ID} = $studyId AND ${DAO.COLUMN_FILTER_IS_VALID} = 0"
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
    fun deleteFilter( filter: Filter)
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(filter.id.toString())

        Log.d( "xxx", "Delete Filter with id: ${filter.id}")

        db.delete(DAO.TABLE_FILTER, whereClause, args)
        db.close()
    }
}