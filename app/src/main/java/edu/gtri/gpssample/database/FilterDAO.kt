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

data class RawFilterOperator(var rule1 :Int , var rule2 : Int?, var connector : Int)

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
            filter.id?.let{id ->
                filter.rule?.let{rule ->
                    rule.filterOperator?.let{filterOperator ->
                        traverseRuleChain(rule, filter)
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

    fun addEmptyFilterOperator(rule: Rule, filter : Filter)
    {
        ruleCheck(rule)
        filter.id?.let { filterId ->
            rule.id?.let{ruleId ->
                val values = ContentValues()
                values.put(DAO.COLUMN_CONNECTOR, ConnectorConverter.toIndex(Connector.NONE))
                // values.put( DAO.COLUMN_STUDY_ID, study.id )

                values.put(DAO.COLUMN_FILTER_ID, filterId)
                values.put( DAO.COLUMN_FIRST_RULE_ID, ruleId )
                dao.writableDatabase.insert(DAO.TABLE_FILTEROPERATOR, null, values)
            }

        }
    }
    fun traverseRuleChain(rule: Rule, filter: Filter)
    {
        ruleCheck(rule)
        // now insert into connector table
        rule.filterOperator?.let{filterOperator ->
            filterOperator.rule?.let{secondRule ->
                ruleCheck(secondRule)
                addFilterOperator(rule, filter )
                traverseRuleChain(secondRule, filter)
                // the filter operator numst have a rule, otherwise this doesn't make sense

            }
        }

    }

    private fun putFilterOperator( filter: Filter, rule : Rule, values: ContentValues )
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

                            values.put(DAO.COLUMN_FILTER_ID, filter.id!!)
                            values.put( DAO.COLUMN_FIRST_RULE_ID, firstRuleId )
                            values.put( DAO.COLUMN_SECOND_RULE_ID, secondRuleId )
                        }

                    }

                }
            }
        }

    }
    fun addFilterOperator(rule : Rule, filter : Filter)
    {
        val values = ContentValues()

        putFilterOperator( filter, rule, values )
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
        //buildRuleChain(filter, cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_RULE_ID)), study)
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

        filter.id?.let{ filterId ->
            val query = "SELECT * FROM ${DAO.TABLE_FILTEROPERATOR} WHERE ${DAO.COLUMN_FILTER_ID} = ${filterId}"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {

                val firstRuleId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIRST_RULE_ID))
                val secondRuleId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_SECOND_RULE_ID))
                val connectorId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONNECTOR))
                val rawFilterOperator = RawFilterOperator(firstRuleId, secondRuleId, connectorId)
                rawFilterOperators.add(rawFilterOperator)
            }
            cursor.close()


            // alg:

            // find rules, see if they are already in the list.  if so, use the ones that are
            // there.  if not add to structure.  we want a chain that starts with a single rule
            // since this can come in out of order we need to do a weird sorting
            // if a rule is in the list and it is the second rule for another rule, remove it from the list


            var rules = ArrayList<Rule>()
            // now loop through to create the rule stack
            var startRule : Rule? = null
            for(rfo in rawFilterOperators)
            {
                // get the first rule
                var rule1 = DAO.ruleDAO.getRule(rfo.rule1)
                var rule2 : Rule? = null
                rfo.rule2?.let{rule2Id->
                    rule2 = DAO.ruleDAO.getRule(rule2Id)

                }
                
                // now we have two rules
                // loop through the raw rules again
                // rule1 can be rule2 or rule2 can be rule 1

//                var containsRule1 = false
//                var containsRule2 = false
//                for(rule in rules)
//                {
//                    if(rule.id == rule1.id)
//                    {
//                        rule1 = rule
//                    }
//                    rule2?.let{r2->
//                        if(r2.id == rule.id)
//                        {
//                            rule2 = rule
//                        }
//                    }
//                }
//
//
//
//                rule1?.let{rule1->
//                    // see if it's already there.  if so, this is a copy, ditch it and grab the  one
//                    // in the list
//                    for(rule in rules)
//                    {
//
//                    }


                }
            }
        }


    }

//    private fun buildRuleChain(filter : Filter, rule_id : Int, study : Study)
//    {
//        // find rule in list of rules in study
//        for(rule in study.rules)
//        {
//            if(rule.id == rule_id)
//            {
//                filter.rule = rule
//                // traverse the chain
//                 getFilterOperator(rule)
//            }
//        }
//    }
//    @SuppressLint("Range")
//    private fun getFilterOperator(rule : Rule?)
//    {
//        rule?.id?.let{rule_id ->
//            val query = "SELECT * FROM ${DAO.TABLE_FILTEROPERATOR}  " +
//                    "WHERE ${DAO.COLUMN_RULE_ID} = ${rule?.id} "
//
//            val cursor = dao.writableDatabase.rawQuery(query, null)
//
//            while (cursor.moveToNext())
//            {
//                val id = cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_ID}"))
//                val connector = ConnectorConverter.fromIndex(cursor.getInt(cursor.
//                                getColumnIndex("${DAO.COLUMN_CONNECTOR}")))
//                val rule = DAO.ruleDAO.getRule(cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_RULE_ID}")))
//                val filterOperator =  FilterOperator(id, connector, rule)
//                filterOperator?.let{filterOperator ->
//                    getFilterOperator(filterOperator.rule)
//                }
//            }
//        }
//    }

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

                // build filter rules from filter operator
                // find all filter operators
                // need to sort and build the chain

                //filter.filterRules = DAO.filterRuleDAO.getFilterRules( filter )
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