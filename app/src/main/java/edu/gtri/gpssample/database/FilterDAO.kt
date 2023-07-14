package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.Connector
import edu.gtri.gpssample.constants.ConnectorConverter
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterRule
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.extensions.toBoolean

class FilterDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateFilter( filter: Filter ) : Filter?
    {
        if (exists( filter ))
        {
            updateFilter( filter )
        }
        else
        {
            val values = ContentValues()

            putFilter( filter, values )
            filter.id = dao.writableDatabase.insert(DAO.TABLE_FILTER, null, values).toInt()
            filter.id?.let { id ->
                for (filterRule in filter.filterRules)
                {
                    filterRule.filterId = id
                    DAO.filterRuleDAO.createOrUpdateFilterRule(filterRule)
                }
            } ?: return null
        }

        return filter
    }

    //--------------------------------------------------------------------------
    fun exists( filter: Filter ): Boolean
    {
        filter.id?.let { id ->
            getFilter( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    private fun putFilter( filter: Filter, values: ContentValues )
    {
        val index = SampleTypeConverter.toIndex(filter.samplingType)

        filter.id?.let { id ->
            Log.d( "xxx", "existing filter id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_STUDY_ID, filter.studyId )
        values.put( DAO.COLUMN_FILTER_NAME, filter.name )
        values.put( DAO.COLUMN_FILTER_SAMPLE_SIZE, filter.sampleSize )
        values.put( DAO.COLUMN_FILTER_SAMPLE_TYPE_INDEX, index )
    }

    //--------------------------------------------------------------------------
    fun updateFilter( filter: Filter )
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
    @SuppressLint("Range")
    private fun  buildFilter(cursor: Cursor ): Filter
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val studyId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_NAME))
        val sampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_SIZE))

        val type = SampleTypeConverter.fromIndex(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_TYPE_INDEX))

        return Filter( id, studyId, name, type, sampleSize )
    }

    fun getFilter(id : Int) : Filter?
    {
        var filter: Filter? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_ID} = ${id}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filter = buildFilter(cursor)
            filter.filterRules = DAO.filterRuleDAO.getFilterRules( filter )
        }

        cursor.close()
        db.close()

        return filter
    }

    fun getFilters(study : Study) : ArrayList<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_STUDY_ID} = '${study.id}'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val filter = buildFilter(cursor)
            filter.filterRules = DAO.filterRuleDAO.getFilterRules( filter )
            filters.add( filter)
        }

        cursor.close()
        db.close()

        return filters
    }

    fun getFilters() : ArrayList<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val filter = buildFilter(cursor)
            filter.filterRules = DAO.filterRuleDAO.getFilterRules( filter )
            filters.add( filter)
        }

        cursor.close()
        db.close()

        return filters
    }

    //--------------------------------------------------------------------------
    fun deleteFilter( filter: Filter )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(filter.id.toString())

        db.delete(DAO.TABLE_FILTER, whereClause, args)
        db.close()
    }
}