/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.Connector
import edu.gtri.gpssample.constants.ConnectorConverter
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CREATION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FILTER_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FILTER_SAMPLE_SIZE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FILTER_SAMPLE_TYPE_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_VERSION
import edu.gtri.gpssample.database.models.CollectionTeam
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterOperator
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.extensions.toBoolean
import java.util.UUID

data class RawFilterOperator(var uuid : String, var order : Int, var rule1 : String , var rule2 : String?, var connector : Int)

class FilterDAO(private var dao: DAO)
{
    private val kFilterOperatorOrderUndefined = 0

    fun createOrUpdateFilter( filter: Filter, version: String ) : Filter?
    {
        filter.version = version

        val values = ContentValues()
        putFilter( filter,values )

        deleteAllFilterOperators(filter)

        dao.upsert( DAO.TABLE_RULE, values )

        filter.rule?.let{ rule ->
            rule.filterOperator?.let{ filterOperator ->
                val filterOperatorOrder = kFilterOperatorOrderUndefined
                traverseRuleChain(rule, filter, filterOperatorOrder)
            }?: run {
                addEmptyFilterOperator(rule, filter)
            }
        }

        return filter
    }

    fun ruleCheck(rule : Rule)
    {
        DAO.ruleDAO.createOrUpdateRule(rule,rule.version)
    }

    private fun addEmptyFilterOperator(rule: Rule, filter : Filter)
    {
        ruleCheck(rule)
        val values = ContentValues()
        values.put(DAO.COLUMN_UUID, UUID.randomUUID().toString())
        values.put(DAO.COLUMN_CONNECTOR, ConnectorConverter.toIndex(Connector.NONE))
        values.put(DAO.COLUMN_FILTEROPERATOR_ORDER, 1)
        values.put(DAO.COLUMN_FILTER_UUID, filter.uuid)
        values.put( DAO.COLUMN_FIRST_RULE_UUID, rule.uuid )
        dao.writableDatabase.insert(DAO.TABLE_FILTEROPERATOR, null, values)
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
            }
        }
    }

    private fun putFilterOperator( filter: Filter, rule : Rule, order : Int, values: ContentValues )
    {
        val index = SampleTypeConverter.toIndex(filter.samplingType)

        rule.filterOperator?.let{ filterOperator ->
            values.put( DAO.COLUMN_UUID, UUID.randomUUID().toString())
            values.put( DAO.COLUMN_CONNECTOR, ConnectorConverter.toIndex(filterOperator.connector))
            values.put( DAO.COLUMN_FILTEROPERATOR_ORDER, order)
            values.put( DAO.COLUMN_FILTER_UUID, filter.uuid)
            values.put( DAO.COLUMN_FIRST_RULE_UUID, rule.uuid )

            filterOperator.rule?.let{ secondRule ->
                values.put( DAO.COLUMN_SECOND_RULE_UUID, secondRule.uuid )
            }
        }
    }

    fun addFilterOperator(rule : Rule, filter : Filter, filterOperatorOrder: Int)
    {
        val values = ContentValues()

        putFilterOperator( filter, rule, filterOperatorOrder, values )
        dao.writableDatabase.insert(DAO.TABLE_FILTEROPERATOR, null, values).toInt()
    }

    private fun putFilter( filter: Filter, values: ContentValues )
    {
        val index = SampleTypeConverter.toIndex(filter.samplingType)

        values.put( DAO.COLUMN_UUID, filter.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, filter.creationDate )
        values.put( DAO.COLUMN_VERSION, filter.version )
        values.put( DAO.COLUMN_STUDY_UUID, filter.studyUuid )
        values.put( DAO.COLUMN_FILTER_NAME, filter.name )
        values.put( DAO.COLUMN_FILTER_SAMPLE_SIZE, filter.sampleSize )
        values.put( DAO.COLUMN_FILTER_SAMPLE_TYPE_INDEX, index )
    }

    @SuppressLint("Range")
    private fun  buildFilter(cursor: Cursor, study : Study ): Filter?
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTER_NAME))
        val sampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_SIZE))
        val studyUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_UUID))

        val type = SampleTypeConverter.fromIndex(cursor.getColumnIndex(DAO.COLUMN_FILTER_SAMPLE_TYPE_INDEX))

        val filter = Filter( uuid, creationDate, name, type, sampleSize, studyUuid, version )

        findFieldOperators(filter)

        return filter
    }

    @SuppressLint("Range")
    fun findFieldOperators(filter : Filter)
    {
        // build the raw id list so we can sort it
        val rawFilterOperators = ArrayList<RawFilterOperator>()

        val query = "SELECT * FROM ${DAO.TABLE_FILTEROPERATOR} WHERE ${DAO.COLUMN_FILTER_UUID} = '${filter.uuid}' ORDER BY ${DAO.COLUMN_FILTEROPERATOR_ORDER} ASC"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext()) {
            val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
            val order = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTEROPERATOR_ORDER))
            val firstRuleId = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIRST_RULE_UUID))
            val secondRuleId = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_SECOND_RULE_UUID))
            val connectorId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONNECTOR))
            val rawFilterOperator = RawFilterOperator(uuid, order, firstRuleId, secondRuleId, connectorId)
            rawFilterOperators.add(rawFilterOperator)
        }

        cursor.close()

        // alg:
        // load the filter operator as a list of RawFilterOperators ordered by operator_order
        // use those to load all rules.
        // loop through the filter operators again
        // create the filter operator object and chain pulling
        // the rule from the loaded rule list

        val rulesMap = HashMap<String, Rule>()

        // now loop through to create the rule stack
        var firstRule: Rule? = null

        for (rfo in rawFilterOperators)
        {
            //build a list of rules, don't reload the rule from the database
            if (!rulesMap.keys.contains(rfo.rule1)) {
                val rule1 = DAO.ruleDAO.getRule(rfo.rule1)
                rule1?.let { rule1 ->
                    rulesMap.put(rule1.uuid, rule1)
                }
            }
            rfo.rule2?.let { rule2Id ->
                // don't reload the rule from the database
                if (!rulesMap.keys.contains(rule2Id)) {
                    val rule2 = DAO.ruleDAO.getRule(rule2Id)
                    rule2?.let { rule2 ->
                        rulesMap.put(rule2.uuid, rule2)
                    }
                }
            }
        }

        // loop through again
        for(rfo in rawFilterOperators)
        {
            // build up the ruleset
            val rule1 = rulesMap[rfo.rule1]
            val connector = ConnectorConverter.fromIndex(rfo.connector)
            if(connector != Connector.NONE)
            {
                rfo.rule2?.let{ rule2Id->
                    val rule2 = rulesMap[rule2Id]
                    rule1?.filterOperator = FilterOperator(rfo.uuid, rfo.order, connector, rule2 )
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

    fun getPrimaryFilters( study : Study ) : ArrayList<Filter>
    {
        val filters = ArrayList<Filter>()
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_STUDY_UUID} = '${study.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val filter = buildFilter(cursor, study)
            filter?.let { filter ->
                filter.rule?.let { rule ->
                    if (!rule.isSubsetRule)
                    {
                        filters.add( filter)
                    }
                }
            }
        }

        cursor.close()

        return filters
    }

    fun getSubsetFilters( study : Study ) : ArrayList<Filter>
    {
        val filters = ArrayList<Filter>()
        val query = "SELECT * FROM ${DAO.TABLE_FILTER} WHERE ${DAO.COLUMN_STUDY_UUID} = '${study.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val filter = buildFilter(cursor, study)
            filter?.let { filter ->
                filter.rule?.let { rule ->
                    if (rule.isSubsetRule)
                    {
                        filters.add( filter)
                    }
                }
            }
        }

        cursor.close()

        return filters
    }

    fun deleteFilter( filter: Filter )
    {
        deleteAllFilterOperators(filter)
        // delete the filterOperators
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(filter.uuid)

        dao.writableDatabase.delete(DAO.TABLE_FILTER, whereClause, args)
    }

    private fun deleteAllFilterOperators(filter : Filter)
    {
        val whereClause = "${DAO.COLUMN_FILTER_UUID} = ?"
        val args = arrayOf(filter.uuid)
        dao.writableDatabase.delete(DAO.TABLE_FILTEROPERATOR, whereClause, args)
    }

    companion object
    {
        val columnBindings = listOf(
            ColumnBinding<Filter>(COLUMN_CREATION_DATE,"INTEGER",Filter::creationDate ),
            ColumnBinding<Filter>(COLUMN_VERSION,"TEXT",Filter::version ),
            ColumnBinding<Filter>(COLUMN_STUDY_UUID,"TEXT",Filter::studyUuid ),
            ColumnBinding<Filter>(COLUMN_FILTER_NAME,"TEXT",Filter::name ),
            ColumnBinding<Filter>(COLUMN_FILTER_SAMPLE_SIZE,"INTEGER",Filter::sampleSize ),
            ColumnBinding<Filter>(COLUMN_FILTER_SAMPLE_TYPE_INDEX,"INTEGER",{SampleTypeConverter.toIndex(it.samplingType)} ),
        )
    }
}