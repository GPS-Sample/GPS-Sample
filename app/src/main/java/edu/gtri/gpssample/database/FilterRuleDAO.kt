package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterRule

class FilterRuleDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createFilterRule( filterRule: FilterRule) : Int
    {
        val values = ContentValues()

        putFilterRule( filterRule, values )

        return dao.writableDatabase.insert(DAO.TABLE_FILTERRULE, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putFilterRule(filterRule: FilterRule, values: ContentValues)
    {
        values.put( DAO.COLUMN_FILTERRULE_STUDY_ID, filterRule.studyId )
        values.put( DAO.COLUMN_FILTERRULE_FILTER_ID, filterRule.filterId )
        values.put( DAO.COLUMN_FILTERRULE_RULE_ID, filterRule.ruleId )
        values.put( DAO.COLUMN_FILTERRULE_CONNECTOR, filterRule.connector )
    }

    //--------------------------------------------------------------------------
    fun updateFilterRule( filterRule: FilterRule)
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
    fun getFilterRule( filterRuleId: Int ) : FilterRule?
    {
        var filterRule: FilterRule? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTERRULE} WHERE ${DAO.COLUMN_ID} = $filterRuleId"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            filterRule = createFilterRuleModel( cursor )
        }

        cursor.close()
        db.close()

        return filterRule
    }

    //--------------------------------------------------------------------------
    private fun  createFilterRuleModel( cursor: Cursor): FilterRule
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val studyId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_STUDY_ID))
        val filterId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_FILTER_ID))
        val ruleId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_RULE_ID))
        val connector = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_CONNECTOR))

        return FilterRule( id, studyId, filterId, ruleId,connector )
    }

    //--------------------------------------------------------------------------
    fun getFilterRules( studyId: Int, filterId: Int ): List<FilterRule>
    {
        val filterRules = ArrayList<FilterRule>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTERRULE} WHERE ${DAO.COLUMN_FILTERRULE_STUDY_ID} = $studyId AND ${DAO.COLUMN_FILTERRULE_FILTER_ID} = $filterId"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filterRules.add( createFilterRuleModel( cursor ))
        }

        cursor.close()
        db.close()

        return filterRules
    }

    //--------------------------------------------------------------------------
    fun getFilterRules(): List<FilterRule>
    {
        val filterRules = ArrayList<FilterRule>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTERRULE}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filterRules.add( createFilterRuleModel( cursor ))
        }

        cursor.close()
        db.close()

        return filterRules
    }

    //--------------------------------------------------------------------------
    fun deleteFilterRule( filterRule: FilterRule)
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(filterRule.id.toString())

        Log.d( "xxx", "Deleting FilterRule with id: ${filterRule.id}")

        db.delete(DAO.TABLE_FILTERRULE, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteFilterRules( studyId: Int, filterId: Int )
    {
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTERRULE} WHERE ${DAO.COLUMN_FILTERRULE_STUDY_ID} = $studyId AND ${DAO.COLUMN_FILTERRULE_FILTER_ID} = ${filterId}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            deleteFilterRule( createFilterRuleModel( cursor ))
        }

        cursor.close()
        db.close()
    }
}