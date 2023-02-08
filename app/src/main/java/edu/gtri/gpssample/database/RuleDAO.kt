package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Rule

class RuleDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createRule( rule: Rule) : Int
    {
        val values = ContentValues()

        putRule( rule, values )

        return dao.writableDatabase.insert(DAO.TABLE_RULE, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putRule( rule: Rule, values: ContentValues )
    {
        values.put( DAO.COLUMN_RULE_STUDY_ID, rule.studyId )
        values.put( DAO.COLUMN_RULE_FIELD_ID, rule.fieldId )
        values.put( DAO.COLUMN_RULE_NAME, rule.name )
        values.put( DAO.COLUMN_RULE_OPERATOR, rule.operator )
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

    //--------------------------------------------------------------------------
    fun getRule( ruleId: Int ) : Rule?
    {
        var rule: Rule? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_ID} = $ruleId"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            rule = createRuleModel( cursor )
        }

        cursor.close()
        db.close()

        return rule
    }

    //--------------------------------------------------------------------------
    private fun  createRuleModel( cursor: Cursor): Rule
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val studyId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_RULE_STUDY_ID))
        val fieldId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_RULE_FIELD_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_NAME))
        val operator = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_OPERATOR))
        val value = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_VALUE))

        return Rule( id, studyId, fieldId, name, operator, value )
    }

    //--------------------------------------------------------------------------
    fun getRules( studyId: Int ): List<Rule>
    {
        val rules = ArrayList<Rule>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_RULE_STUDY_ID} = $studyId"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            rules.add( createRuleModel( cursor ))
        }

        cursor.close()
        db.close()

        return rules
    }

    //--------------------------------------------------------------------------
    fun getRules(): List<Rule>
    {
        val rules = ArrayList<Rule>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_RULE}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            rules.add( createRuleModel( cursor ))
        }

        cursor.close()
        db.close()

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