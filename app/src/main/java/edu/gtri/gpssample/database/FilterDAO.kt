package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.Connector
import edu.gtri.gpssample.constants.ConnectorConverter
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterOperator
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.extensions.toBoolean

class FilterDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateFilter( filter: Filter, study : Study ) : Filter?
    {
        if (exists( filter, study ))
        {
            updateFilter( filter, study )
        }
        else
        {
            val values = ContentValues()

            putFilter( filter,study, values )
            filter.id = dao.writableDatabase.insert(DAO.TABLE_FILTER, null, values).toInt()
            filter.id?.let { id ->
            // now traverse the rule - filter operator chain
                filter.rule?.let{rule ->
                    traverseRuleChain(rule, study)
                }
            } ?: return null
        }

        return filter
    }

    fun traverseRuleChain(rule : Rule?, study : Study)
    {
        rule?.let{rule ->
            // By the time we get here the rule should have been saved in the database
            rule.id?.let{rule_id ->
                rule.filterOperator?.let{filterOperator ->
                    // find the id of the connector copy
                    val values = ContentValues()
                    putFilterOperator(filterOperator, values)
                    filterOperator.id?.let{id ->
                        // update, else insert
                        val whereClause = "${DAO.COLUMN_ID} = ?"
                        val args: Array<String> = arrayOf(id.toString())
                        val values = ContentValues()

                        dao.writableDatabase.update(DAO.TABLE_FILTEROPERATOR, values, whereClause, args )
                    }?: run{
                        filterOperator.id = dao.writableDatabase.insert(DAO.TABLE_FILTEROPERATOR,
                            null, values).toInt()

                    }
                    traverseRuleChain(filterOperator.rule, study)
                }
            }?: run{
                Log.d("xxxxxx", "NO ID")
            }
        }
    }
    //--------------------------------------------------------------------------
    fun exists( filter: Filter, study : Study ): Boolean
    {
        filter.id?.let { id ->
            getFilter( id, study)?.let {
                return true
            } ?: return false
        } ?: return false
    }

    private fun putFilterOperator(filterOperator: FilterOperator, values: ContentValues)
    {
        filterOperator.id?.let{id->
            values.put(DAO.COLUMN_ID, id)
        }
        values.put(DAO.COLUMN_CONNECTOR, ConnectorConverter.toIndex(filterOperator.conenctor))
        filterOperator.rule?.id?.let{rule_id ->
            values.put(DAO.COLUMN_RULE_ID, rule_id)
        }?: run{
            filterOperator.rule?.let{rule ->
                DAO.ruleDAO.createOrUpdateRule(rule)
                values.put(DAO.COLUMN_RULE_ID, rule.id)
            }
        }
    }
    //--------------------------------------------------------------------------
    private fun putFilter( filter: Filter, study: Study, values: ContentValues )
    {
        val index = SampleTypeConverter.toIndex(filter.samplingType)

        filter.id?.let { id ->
            Log.d( "xxx", "existing filter id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_STUDY_ID, study.id )
        values.put( DAO.COLUMN_FILTER_NAME, filter.name )
        values.put( DAO.COLUMN_FILTER_SAMPLE_SIZE, filter.sampleSize )
        values.put( DAO.COLUMN_FILTER_SAMPLE_TYPE_INDEX, index )
        filter.rule?.let{rule ->
            rule.id?.let{id ->
                values.put(DAO.COLUMN_RULE_ID, id)
            } ?: run {
                DAO.ruleDAO.createOrUpdateRule(rule)
                values.put(DAO.COLUMN_RULE_ID, rule.id!!)
            }
        }
    }

    //--------------------------------------------------------------------------
    fun updateFilter( filter: Filter, study : Study )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(filter.id.toString())
        val values = ContentValues()

        putFilter( filter, study, values )

        db.update(DAO.TABLE_FILTER, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun  buildFilter(cursor: Cursor, study : Study ): Filter?
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_NAME))
        val sampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_SIZE))

        val type = SampleTypeConverter.fromIndex(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_TYPE_INDEX))
        val filter = Filter( id, name, type, sampleSize )
        buildRuleChain(filter, cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_RULE_ID)), study)
        return filter
    }

    private fun buildRuleChain(filter : Filter, rule_id : Int, study : Study)
    {
        // find rule in list of rules in study
        for(rule in study.rules)
        {
            if(rule.id == rule_id)
            {
                filter.rule = rule
                // traverse the chain
                 getFilterOperator(rule)
            }
        }
    }
    @SuppressLint("Range")
    private fun getFilterOperator(rule : Rule?)
    {
        var db = dao.writableDatabase
        rule?.id?.let{rule_id ->
            var query = "SELECT * FROM ${DAO.TABLE_FILTEROPERATOR}  " +
                    "WHERE ${DAO.COLUMN_RULE_ID} = ${rule?.id} "

            var cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val id = cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_ID}"))
                val connector = ConnectorConverter.fromIndex(cursor.getInt(cursor.
                                getColumnIndex("${DAO.COLUMN_CONNECTOR}")))
                val rule = DAO.ruleDAO.getRule(cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_RULE_ID}")))
                val filterOperator =  FilterOperator(id, connector, rule)
                filterOperator?.let{filterOperator ->
                    getFilterOperator(filterOperator.rule)
                }
            }
        }

    }
    private fun buildRuleChainFromRule(rule : Rule?, study : Study)
    {

    }
    fun getFilter(id : Int, study : Study) : Filter?
    {
        var filter: Filter? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_ID} = ${id}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filter = buildFilter(cursor, study)
            filter?.let{filter->
              //  filter.filterRules = DAO.filterRuleDAO.getFilterRules( filter )
            }
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
            val filter = buildFilter(cursor, study)
            filter?.let{filter->
                //filter.filterRules = DAO.filterRuleDAO.getFilterRules( filter )
                filters.add( filter)
            }
        }

        cursor.close()
        db.close()

        return filters
    }

    fun getFilters() : ArrayList<Filter>
    {
        val filters = ArrayList<Filter>()
//        val db = dao.writableDatabase
//        val query = "SELECT * FROM ${DAO.TABLE_FILTER}"
//        val cursor = db.rawQuery(query, null)
//
//        while (cursor.moveToNext())
//        {
//            val filter = buildFilter(cursor)
//            filter?.let{filter->
//                //filter.filterRules = DAO.filterRuleDAO.getFilterRules( filter )
//                filters.add( filter)
//            }
//        }
//
//        cursor.close()
//        db.close()

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