package edu.gtri.gpssample.database

import android.annotation.SuppressLint
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
        values.put( DAO.COLUMN_UUID, rule.uuid )
        values.put( DAO.COLUMN_RULE_STUDY_UUID, rule.study_uuid )
        values.put( DAO.COLUMN_RULE_FIELD_UUID, rule.field_uuid )
        values.put( DAO.COLUMN_RULE_NAME, rule.name )
        values.put( DAO.COLUMN_RULE_OPERATOR, rule.operator )
        values.put( DAO.COLUMN_RULE_VALUE, rule.value )
    }

    //--------------------------------------------------------------------------
    fun updateRule( rule: Rule )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(rule.uuid.toString())
        val values = ContentValues()

        putRule( rule, values )

        db.update(DAO.TABLE_RULE, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getRule( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun getRule( uuid: String ) : Rule?
    {
        var rule: Rule? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            rule = createRule( cursor )
        }

        cursor.close()
        db.close()

        return rule
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun  createRule(cursor: Cursor): Rule
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val study_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_STUDY_UUID))
        val field_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_FIELD_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_NAME))
        val operator = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_OPERATOR))
        val value = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_VALUE))

        return Rule( id, uuid, study_uuid, field_uuid, name, operator, value )
    }

    //--------------------------------------------------------------------------
    fun getRules( study_id: Int ): List<Rule>
    {
        val rules = ArrayList<Rule>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_STUDY_ID} = '$study_id'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            rules.add( createRule( cursor ))
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
            rules.add( createRule( cursor ))
        }

        cursor.close()
        db.close()

        return rules
    }

    //--------------------------------------------------------------------------
    fun deleteRule( rule: Rule )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(rule.uuid.toString())

        db.delete(DAO.TABLE_RULE, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteOrphans()
    {
        val rules = getRules()

        for (rule in rules)
        {
            if (DAO.studyDAO.doesNotExist( rule.study_uuid ) or DAO.fieldDAO.doesNotExist( rule.field_uuid ))
            {
                deleteRule( rule )
            }
        }
    }
}