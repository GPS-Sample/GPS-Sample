package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.Connector
import edu.gtri.gpssample.constants.ConnectorConverter
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.FilterRule
import edu.gtri.gpssample.database.models.Study

class FilterRuleDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createFilterRule( filterRule: FilterRule, filter : Filter ) : Int
    {
        filterRule.rule?.let { rule ->
            val values = ContentValues()
            putFilterRule(filterRule, filter, values)
            return dao.writableDatabase.insert(DAO.TABLE_FILTERRULE, null, values).toInt()
        }
        return -1
    }

    //--------------------------------------------------------------------------
    private fun putFilterRule( filterRule: FilterRule, filter: Filter, values: ContentValues )
    {
        // this only works if the rule is set on the filterrule and it's in the database
        values.put( DAO.COLUMN_FILTER_ID, filter.id!! )
        values.put( DAO.COLUMN_RULE_ID, filterRule.rule!!.id!! )
        val cntrIndex = ConnectorConverter.toIndex(filterRule.connector)
        values.put( DAO.COLUMN_FILTERRULE_CONNECTOR_INDEX, cntrIndex )
    }

    //--------------------------------------------------------------------------
    fun updateFilterRule( filterRule: FilterRule, filter : Filter )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(filterRule.id.toString())
        val values = ContentValues()

        putFilterRule( filterRule, filter, values )

        db.update(DAO.TABLE_FILTERRULE, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun  buildFilterRule(cursor: Cursor, study : Study ): FilterRule
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val study_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val rule_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_RULE_ID))

        // find rule in list based on id.  we don't want to rebuild a rule, nor do we want to
        // continue building the filterrule if the rule isnt' in the list.
        // bug out if rule isn't there.  the dependency is study has filters, filters have
        // filterrules, filterrule has rule.
        var rule : Rule? = null

        // probably could do this better with a .equals override and use contains?
        // there's only the id so maybe no.
        for(test in study.rules)
        {
            test.id?.let { id ->
                if (id == rule_id) {
                    rule = test
                }
            }
            if(rule != null)
            {
                break
            }
        }
        //TODO: connector table
        val connector = ConnectorConverter.fromIndex(
            cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_CONNECTOR_INDEX)))

        return FilterRule(id, -1, rule, connector)
    }

    //--------------------------------------------------------------------------
    fun getFilterRules( filter : Filter, study : Study): List<FilterRule>
    {
        val filterRules = ArrayList<FilterRule>()

        filter.id?.let{id ->
            val db = dao.writableDatabase
            val query = "SELECT * FROM ${DAO.TABLE_FILTERRULE} WHERE ${DAO.COLUMN_FILTER_ID} = '$id'"
            val cursor = db.rawQuery(query, null)

            // pass in the list of rules for a given study
            while (cursor.moveToNext())
            {
                filterRules.add( buildFilterRule( cursor, study))
            }

            cursor.close()
            db.close()

        }

        return filterRules
    }

    //--------------------------------------------------------------------------
    fun deleteFilterRule( filterRule: FilterRule )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(filterRule.id.toString())

        Log.d( "xxx", "Deleting FilterRule with id: ${filterRule.id}")

        db.delete(DAO.TABLE_FILTERRULE, whereClause, args)
        db.close()
    }
}