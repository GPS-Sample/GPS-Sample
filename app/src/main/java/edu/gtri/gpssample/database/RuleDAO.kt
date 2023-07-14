package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.OperatorConverter
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study

class RuleDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateRule( rule: Rule ) : Rule?
    {
        if (exists( rule ))
        {
            updateRule( rule )
        }
        else
        {
            val values = ContentValues()

            putRule( rule, values )
            rule.id = dao.writableDatabase.insert(DAO.TABLE_RULE, null, values).toInt()
            rule.id?.let { id ->
                Log.d( "xxx", "new rule id = ${id}")
            } ?: return null
        }

        return rule
    }

    //--------------------------------------------------------------------------
    fun exists( rule: Rule ): Boolean
    {
        rule.id?.let { id ->
            getRule( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    private fun putRule( rule: Rule, values: ContentValues )
    {
        val operatorId = OperatorConverter.toIndex(rule.operator)

        rule.id?.let { id ->
            Log.d( "xxx", "existing filter id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_STUDY_ID, rule.studyId )
        values.put( DAO.COLUMN_FIELD_ID, rule.fieldId )
        values.put( DAO.COLUMN_RULE_NAME, rule.name )
        values.put( DAO.COLUMN_OPERATOR_ID, operatorId )
        values.put( DAO.COLUMN_RULE_VALUE, rule.value )
    }

    //--------------------------------------------------------------------------
    fun updateRule( rule: Rule )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(rule.id.toString())
        val values = ContentValues()

        putRule( rule, values )

        db.update(DAO.TABLE_RULE, values, whereClause, args )
        db.close()
    }

    @SuppressLint("Range")
    private fun  buildRule(cursor: Cursor): Rule
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val studyId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val fieldId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_NAME))

        // TODO:  this should be a lookup table
        val operatorId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_OPERATOR_ID))
        val value = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_VALUE))

        val operator = OperatorConverter.fromIndex(operatorId)

        return Rule( id, studyId, fieldId, name, operator, value )
    }

    fun getRule( id: Int ) : Rule?
    {
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_ID} = ${id}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            return buildRule( cursor )
        }

        cursor.close()
        db.close()

        return null
    }

    fun getRules() : ArrayList<Rule>
    {
        val rules = ArrayList<Rule>()

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_RULE}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            rules.add( buildRule( cursor ))
        }

        cursor.close()
        db.close()

        return rules
    }

    fun getRules( study: Study ) : ArrayList<Rule>
    {
        val rules = ArrayList<Rule>()

        study.id?.let { id ->
            val db = dao.writableDatabase
            val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_STUDY_ID} = '${id}'"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                rules.add( buildRule( cursor ))
            }

            cursor.close()
            db.close()
        }

        return rules
    }

    //--------------------------------------------------------------------------
    fun deleteRule( rule: Rule )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(rule.id.toString())

        db.delete(DAO.TABLE_RULE, whereClause, args)
        db.close()
    }

}