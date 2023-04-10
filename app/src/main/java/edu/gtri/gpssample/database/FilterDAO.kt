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
        return false
        //return getFilter( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
//    fun getFilter( uuid: String ) : Filter?
//    {
//        var filter: Filter? = null
//        val db = dao.writableDatabase
//        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
//        val cursor = db.rawQuery(query, null)
//
//        if (cursor.count > 0)
//        {
//            cursor.moveToNext()
//
//            filter = buildFilter( cursor )
//        }
//
//        cursor.close()
//        db.close()
//
//        return filter
//    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun  buildFilter(cursor: Cursor ): Filter
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_NAME))
        val sampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_SIZE))

        val type = SampleTypeConverter.fromIndex(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_TYPE_INDEX))

        return Filter( id, uuid, name, type, sampleSize )
    }

    // Pass in the list of rules since they were already pulled from the database
    // TODO:  maybe build a better search through the list to find the matching rule for
    //        the rule_id that is in the database.
    @SuppressLint("Range")
    private fun  buildFilterRule(cursor: Cursor, rules : ArrayList<Rule> ): FilterRule
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val order = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_ORDER))
        val rule_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_RULE_ID))
        val connector_index = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_CONNECTOR_INDEX))
        val connector = ConnectorConverter.fromIndex(connector_index)

        var found_rule : Rule? = null

        // this could be more clever
        for(rule in rules)
        {
            if(rule.id == rule_id)
            {
                found_rule = rule
                break
            }
        }

        return FilterRule(id, uuid, order, found_rule, connector) //Filter( id, uuid, name, type, sampleSize )
    }

    fun getFiltersForStudy(study : Study) : ArrayList<Filter>
    {
        val filters = ArrayList<Filter>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_STUDY_ID} = '${study.id}'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val filter = buildFilter(cursor)
            getFilterRulesForFilter(filter, study.rules)

            filters.add( filter)
        }

        cursor.close()
        db.close()

        return filters
    }

    private fun getFilterRulesForFilter(filter : Filter, rules : ArrayList<Rule>) : ArrayList<FilterRule>
    {
        val filterRules = ArrayList<FilterRule>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTERRULE} WHERE ${DAO.COLUMN_FILTER_ID} = '${filter.id}'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val filterRule = buildFilterRule(cursor, rules)
            filter.filterRules.add(filterRule)

            //filters.add( filter)
        }

        cursor.close()
        db.close()

        return filterRules
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