package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.Filter

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
        values.put( DAO.COLUMN_FILTER_RULE_ID, filter.ruleId )
        values.put( DAO.COLUMN_FILTER_NAME, filter.name )
        values.put( DAO.COLUMN_FILTER_CONNECTOR, filter.connector )
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
        val ruleId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_RULE_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_NAME))
        val connector = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_CONNECTOR))

        return Filter( id, studyId, ruleId, name, connector )
    }

    //--------------------------------------------------------------------------
    fun getFilters( studyId: Int ): List<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_RULE_STUDY_ID} = $studyId"
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

        db.delete(DAO.TABLE_FILTER, whereClause, args)
        db.close()
    }}