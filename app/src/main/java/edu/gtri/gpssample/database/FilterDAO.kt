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

data class RawFilterOperator(var id : Int, var order : Int, var rule1 :Int , var rule2 : Int?, var connector : Int)

class FilterDAO(private var dao: DAO)
{
    private val kFilterOperatorOrderUndefined = 0

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
            filter.id?.let{id ->
                filter.rule?.let{rule ->
                    rule.filterOperator?.let{filterOperator ->
                        var filterOperatorOrder = kFilterOperatorOrderUndefined
                        traverseRuleChain(rule, filter, filterOperatorOrder)
                    }?: run {
                        addEmptyFilterOperator(rule, filter)
                    }

                }

//                filter.rule?.let{rule ->
//                    // add the rule if it doesn't exist
//
//
//                //DAO.ruleDAO.createOrUpdateRule(rule)
//                }
            }
//            filter.id?.let { id ->
//            // now traverse the rule - filter operator chain
//                filter.rule?.let{rule ->
//                    traverseRuleChain(rule, study)
//                }
//            } ?: return null
        }

        return filter
    }

    fun ruleCheck(rule : Rule)
    {
        if(rule.id == null)
        {
            // check if the rule is in the db.  it may not have an id, from the copy
            // but may still exist
            rule.id = DAO.ruleDAO.findRuleId(rule)

            if (rule.id == null)
            {
                // not there so we insert into the db
                DAO.ruleDAO.createOrUpdateRule(rule)
            }

        }
    }

    private fun addEmptyFilterOperator(rule: Rule, filter : Filter)
    {
        ruleCheck(rule)
        filter.id?.let { filterId ->
            rule.id?.let{ruleId ->
                val values = ContentValues()
                values.put(DAO.COLUMN_CONNECTOR, ConnectorConverter.toIndex(Connector.NONE))
                // values.put( DAO.COLUMN_STUDY_ID, study.id )
                values.put(DAO.COLUMN_FILTEROPERATOR_ORDER, 1)
                values.put(DAO.COLUMN_FILTER_ID, filterId)
                values.put( DAO.COLUMN_FIRST_RULE_ID, ruleId )
                dao.writableDatabase.insert(DAO.TABLE_FILTEROPERATOR, null, values)
            }

        }
    }
    private fun traverseRuleChain(rule: Rule, filter: Filter, filterOperatorOrder : Int)
    {
        // when adding a new rule chain, increase the order by 1
        val currentOrder = filterOperatorOrder + 1

        ruleCheck(rule)
        // now insert into connector table
        rule.filterOperator?.let{filterOperator ->
            filterOperator.rule?.let{secondRule ->
                ruleCheck(secondRule)
                addFilterOperator(rule, filter, currentOrder )
                traverseRuleChain(secondRule, filter, currentOrder)
                // the filter operator numst have a rule, otherwise this doesn't make sense

            }
        }

    }

    private fun putFilterOperator( filter: Filter, rule : Rule, order : Int, values: ContentValues )
    {
        val index = SampleTypeConverter.toIndex(filter.samplingType)
        filter.id?.let{filterId ->
            rule.filterOperator?.let{filterOperator ->
                filterOperator.id?.let { id ->
                    Log.d( "xxx", "existing filter id = ${id}")
                    values.put( DAO.COLUMN_ID, id )
                }


                rule.id?.let{firstRuleId ->
                    filterOperator.rule?.let{secondRule ->
                        secondRule.id?.let{secondRuleId ->
                            values.put(DAO.COLUMN_CONNECTOR, ConnectorConverter.toIndex(filterOperator.conenctor))
                            // values.put( DAO.COLUMN_STUDY_ID, study.id )
                            values.put(DAO.COLUMN_FILTEROPERATOR_ORDER, order)
                            values.put(DAO.COLUMN_FILTER_ID, filter.id!!)
                            values.put( DAO.COLUMN_FIRST_RULE_ID, firstRuleId )
                            values.put( DAO.COLUMN_SECOND_RULE_ID, secondRuleId )
                        }

                    }

                }
            }
        }

    }
    fun addFilterOperator(rule : Rule, filter : Filter, filterOperatorOrder: Int)
    {
        val values = ContentValues()

        putFilterOperator( filter, rule, filterOperatorOrder, values )
        rule.filterOperator?.id = dao.writableDatabase.insert(DAO.TABLE_FILTEROPERATOR, null, values).toInt()
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

//    private fun putFilterOperator(filterOperator: FilterOperator, values: ContentValues)
//    {
//        filterOperator.id?.let{id->
//            values.put(DAO.COLUMN_ID, id)
//        }
//        values.put(DAO.COLUMN_CONNECTOR, ConnectorConverter.toIndex(filterOperator.conenctor))
//        filterOperator.rule?.id?.let{rule_id ->
//            values.put(DAO.COLUMN_RULE_ID, rule_id)
//        }?: run{
//            filterOperator.rule?.let{rule ->
//                DAO.ruleDAO.createOrUpdateRule(rule)
//                values.put(DAO.COLUMN_RULE_ID, rule.id)
//            }
//        }
//    }
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
//        filter.rule?.let{rule ->
//            rule.id?.let{id ->
//                values.put(DAO.COLUMN_RULE_ID, id)
//            } ?: run {
//                DAO.ruleDAO.createOrUpdateRule(rule)
//                values.put(DAO.COLUMN_RULE_ID, rule.id!!)
//            }
//        }
    }

    //--------------------------------------------------------------------------
    fun updateFilter( filter: Filter, study : Study )
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(filter.id.toString())
        val values = ContentValues()

        putFilter( filter, study, values )

        dao.writableDatabase.update(DAO.TABLE_FILTER, values, whereClause, args )
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
        findFieldOperators(filter)

        return filter
    }

    fun ruleEquals(rule1 : Rule, rule2 : Rule) : Boolean
    {
        if(rule1.id == rule2.id)
        {
            return true
        }
        return false;
    }
    @SuppressLint("Range")
    fun findFieldOperators(filter : Filter)
    {
        // build the raw id list so we can sort it
        var rawFilterOperators = ArrayList<RawFilterOperator>()

        filter.id?.let { filterId ->
            val query =
                "SELECT * FROM ${DAO.TABLE_FILTEROPERATOR} WHERE ${DAO.COLUMN_FILTER_ID} = ${filterId} ORDER BY ${DAO.COLUMN_FILTEROPERATOR_ORDER} DESC"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext()) {
                val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
                val order = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTEROPERATOR_ORDER))
                val firstRuleId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIRST_RULE_ID))
                val secondRuleId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_SECOND_RULE_ID))
                val connectorId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONNECTOR))
                val rawFilterOperator =
                    RawFilterOperator(id, order, firstRuleId, secondRuleId, connectorId)
                rawFilterOperators.add(rawFilterOperator)
            }
            cursor.close()


            // alg:
            // load the filter operator as a list of RawFilterOperators ordered by operator_order
            // use those to load all rules.
            // loop through the filter operators again
            // create the filter operator object and chain pulling
            // the rule from the loaded rule list

            var rulesMap = HashMap<Int, Rule>()

            // now loop through to create the rule stack
            var firstRule: Rule? = null

            for (rfo in rawFilterOperators) {

                //build a list of rules
                // don't reload the rule from the database
                if (!rulesMap.keys.contains(rfo.rule1)) {
                    var rule1 = DAO.ruleDAO.getRule(rfo.rule1)
                    rule1?.let { rule1 ->
                        rulesMap.put(rule1.id!!, rule1)

                    }
                }
                rfo.rule2?.let { rule2Id ->
                    // don't reload the rule from the database
                    if (!rulesMap.keys.contains(rule2Id)) {
                        var rule2 = DAO.ruleDAO.getRule(rule2Id)
                        rule2?.let { rule2 ->
                            rulesMap.put(rule2.id!!, rule2)
                        }
                    }
                }

            }
            // loop through again
            for(rfo in rawFilterOperators)
            {
                // build up the ruleset
                var rule1 = rulesMap[rfo.rule1]
                var connector = ConnectorConverter.fromIndex(rfo.connector)
                if(connector != Connector.NONE)
                {
                    rfo.rule2?.let{rule2Id->
                        val rule2 = rulesMap[rule2Id]
                        rule1?.filterOperator = FilterOperator(rfo.id, rfo.order, connector, rule2 )
                    }
                }
                if(firstRule == null )
                {
                    firstRule = rule1
                }

            }
            // put the rule in the filter
            filter.rule = firstRule
        }

    }

    fun getFilter(id : Int, study : Study) : Filter?
    {
        var filter: Filter? = null
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_ID} = ${id}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filter = buildFilter(cursor, study)
            filter?.let{filter->
              //  filter.filterRules = DAO.filterRuleDAO.getFilterRules( filter )
            }
        }

        cursor.close()

        return filter
    }

    fun getFilters(study : Study) : ArrayList<Filter>
    {
        val filters = ArrayList<Filter>()
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_STUDY_ID} = '${study.id}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val filter = buildFilter(cursor, study)
            filter?.let{filter->
                filters.add( filter)
            }
        }

        cursor.close()

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
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(filter.id.toString())

        dao.writableDatabase.delete(DAO.TABLE_FILTER, whereClause, args)
    }
}