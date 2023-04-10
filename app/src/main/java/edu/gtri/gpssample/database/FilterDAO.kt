package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.ConnectorConverter
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterRule
import edu.gtri.gpssample.extensions.toBoolean

class FilterDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createFilter( filter: Filter, study_id: Int ) : Int
    {
        val values = ContentValues()

        putFilter( filter, study_id, values )
        val id = dao.writableDatabase.insert(DAO.TABLE_FILTER, null, values).toInt()
        filter.id = id

        // write out filter rules
        for(filterRule in filter.filterRules)
        {
            createFilterRule(filterRule, id)
        }
        return id
    }

    fun createFilterRule(filterRule : FilterRule, filterId : Int) : Int
    {
        val values = ContentValues()
        putFilterRule(filterRule, filterId, values)
        val id = dao.writableDatabase.insert(DAO.TABLE_FILTERRULE, null, values).toInt()
        filterRule.id = id
        return id
    }

    private fun putFilterRule(filterRule: FilterRule, filter_id : Int, values: ContentValues )
    {

        values.put( DAO.COLUMN_UUID, filterRule.uuid )
        filterRule.rule?.let{rule ->
            values.put(DAO.COLUMN_RULE_ID, rule.id)
        }

        values.put(DAO.COLUMN_FILTER_ID, filter_id)
        val index = ConnectorConverter.toIndex(filterRule.connector)
        values.put(DAO.COLUMN_FILTERRULE_CONNECTOR_INDEX, index)
    }
    //--------------------------------------------------------------------------
    private fun putFilter( filter: Filter, study_id: Int, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, filter.uuid )
        values.put( DAO.COLUMN_FILTER_NAME, filter.name )
        values.put( DAO.COLUMN_FILTER_SAMPLE_SIZE, filter.sampleSize )

        val index = SampleTypeConverter.toIndex(filter.samplingType)
        values.put( DAO.COLUMN_FILTER_SAMPLE_TYPE_INDEX, index )
        values.put(DAO.COLUMN_STUDY_ID, study_id)
    }

    //--------------------------------------------------------------------------
    fun updateFilter( filter: Filter, study_id: Int )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(filter.uuid.toString())
        val values = ContentValues()

        putFilter( filter, study_id, values )

        db.update(DAO.TABLE_FILTER, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getFilter( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun getFilter( uuid: String ) : Filter?
    {
        var filter: Filter? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            filter = buildFilter( cursor )
        }

        cursor.close()
        db.close()

        return filter
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun  buildFilter(cursor: Cursor ): Filter
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val study_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_NAME))
        val sampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_SIZE))

        val sampleSizeIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_TYPE_INDEX))
        val type = SampleTypeConverter.fromIndex(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_TYPE_INDEX))
        return Filter( id, uuid, study_id, name, type, sampleSize, sampleSizeIndex )
    }

    //--------------------------------------------------------------------------
    fun getFilters( study_id: Int ): List<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_FILTER_STUDY_UUID} = '$study_id'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filters.add( buildFilter( cursor ))
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
            filters.add( buildFilter( cursor ))
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

    //--------------------------------------------------------------------------
    fun deleteOrphans()
    {
//        val filters = getFilters()
//
//        for (filter in filters)
//        {
//            if (DAO.studyDAO.doesNotExist( filter.study_uuid ))
//            {
//                deleteFilter( filter )
//            }
//        }
    }
}