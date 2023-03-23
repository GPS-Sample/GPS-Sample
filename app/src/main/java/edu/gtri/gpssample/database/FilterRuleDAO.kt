package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.Filter
import edu.gtri.gpssample.database.models.FilterRule

class FilterRuleDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createFilterRule( filterRule: FilterRule ) : Int
    {
        val values = ContentValues()

        putFilterRule( filterRule, values )

        return dao.writableDatabase.insert(DAO.TABLE_FILTERRULE, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putFilterRule( filterRule: FilterRule, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, filterRule.uuid )
        values.put( DAO.COLUMN_STUDY_ID, filterRule.study_id )
        values.put( DAO.COLUMN_FILTERRULE_FILTER_UUID, filterRule.filter_uuid )
        values.put( DAO.COLUMN_FILTERRULE_RULE_UUID, filterRule.rule_uuid )
        values.put( DAO.COLUMN_FILTERRULE_CONNECTOR, filterRule.connector )
    }

    //--------------------------------------------------------------------------
    fun updateFilterRule( filterRule: FilterRule )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(filterRule.uuid.toString())
        val values = ContentValues()

        putFilterRule( filterRule, values )

        db.update(DAO.TABLE_FILTERRULE, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getFilterRule( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun getFilterRule( uuid: String ) : FilterRule?
    {
        var filterRule: FilterRule? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTERRULE} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            filterRule = createFilterRule( cursor )
        }

        cursor.close()
        db.close()

        return filterRule
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun  createFilterRule(cursor: Cursor ): FilterRule
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val study_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val filter_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_FILTER_UUID))
        val rule_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_RULE_UUID))
        val connector = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FILTERRULE_CONNECTOR))

        return FilterRule( uuid, study_id, filter_uuid, rule_uuid, connector )
    }

    //--------------------------------------------------------------------------
    fun getFilterRules( study_uuid: String ): List<FilterRule>
    {
        val filterRules = ArrayList<FilterRule>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTERRULE} WHERE ${DAO.COLUMN_FILTERRULE_STUDY_UUID} = '$study_uuid'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filterRules.add( createFilterRule( cursor ))
        }

        cursor.close()
        db.close()

        return filterRules
    }

    //--------------------------------------------------------------------------
    fun getFilterRules( study_id: Int, filter_uuid: String ): List<FilterRule>
    {
        val filterRules = ArrayList<FilterRule>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FILTERRULE} WHERE ${DAO.COLUMN_STUDY_ID} = '$study_id' AND ${DAO.COLUMN_FILTERRULE_FILTER_UUID} = '$filter_uuid'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            filterRules.add( createFilterRule( cursor ))
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
            filterRules.add( createFilterRule( cursor ))
        }

        cursor.close()
        db.close()

        return filterRules
    }

    //--------------------------------------------------------------------------
    fun deleteFilterRule( filterRule: FilterRule )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(filterRule.uuid.toString())

        Log.d( "xxx", "Deleting FilterRule with id: ${filterRule.uuid}")

        db.delete(DAO.TABLE_FILTERRULE, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteOrphans()
    {
//        val filterRules = getFilterRules()
//
//        for (filterRule in filterRules)
//        {
//            if (DAO.studyDAO.doesNotExist( filterRule.study_uuid ) or DAO.filterDAO.doesNotExist( filterRule.filter_uuid ))
//            {
//                deleteFilterRule( filterRule )
//            }
//        }
    }
}