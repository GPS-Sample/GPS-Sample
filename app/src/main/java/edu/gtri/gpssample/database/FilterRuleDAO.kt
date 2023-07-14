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
    fun createFilterRule( filterRule: FilterRule ) : Int
    {
        filterRule.rule?.let { rule ->
            val values = ContentValues()
            putFilterRule(filterRule, values)
            return dao.writableDatabase.insert(DAO.TABLE_FILTERRULE, null, values).toInt()
        }
        return -1
    }

    //--------------------------------------------------------------------------
    private fun putFilterRule( filterRule: FilterRule, values: ContentValues )
    {
        filterRule.id?.let { id ->
            Log.d( "xxx", "existing filterRule id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_FILTER_ID, filterRule.filterId )
        values.put( DAO.COLUMN_RULE_ID, filterRule.rule!!.id!! )
        val cntrIndex = ConnectorConverter.toIndex(filterRule.connector)
        values.put( DAO.COLUMN_FILTERRULE_CONNECTOR_INDEX, cntrIndex )
    }

    //--------------------------------------------------------------------------
    fun updateFilterRule( filterRule: FilterRule )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(filterRule.id.toString())
        val values = ContentValues()

        putFilterRule( filterRule, values )

        db.update(DAO.TABLE_FILTERRULE, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun  buildFilterRule(cursor: Cursor): FilterRule
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val filterId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTER_ID))
        val order = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_ORDER))
        val connector = ConnectorConverter.fromIndex(cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_CONNECTOR_INDEX)))

        val ruleId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_RULE_ID))
        val rule = DAO.ruleDAO.getRule( ruleId )

        return FilterRule(id, filterId, order, rule, connector)
    }

    //--------------------------------------------------------------------------
    fun getFilterRules( filter : Filter ): ArrayList<FilterRule>
    {
        val filterRules = ArrayList<FilterRule>()

        filter.id?.let{id ->
            val db = dao.writableDatabase
            val query = "SELECT * FROM ${DAO.TABLE_FILTERRULE} WHERE ${DAO.COLUMN_FILTER_ID} = '$id'"
            val cursor = db.rawQuery(query, null)

            // pass in the list of rules for a given study
            while (cursor.moveToNext())
            {
                filterRules.add( buildFilterRule( cursor ))
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