package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.constants.OperatorConverter
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study

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
    private fun putRule( rule: Rule, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, rule.uuid )

        values.put( DAO.COLUMN_RULE_NAME, rule.name )

        values.put( DAO.COLUMN_RULE_VALUE, rule.value )

        val operatorId = OperatorConverter.toIndex(rule.operator)
        values.put( DAO.COLUMN_OPERATOR_ID, operatorId )

        rule.field?.let{field ->
            values.put(DAO.COLUMN_FIELD_ID, field.id)
        }
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
        return false//getRule( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }


    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun  buildRule(cursor: Cursor, field : Field): Rule
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        //val field_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_NAME))

        // TODO:  this should be a lookup table
        val operatorId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_OPERATOR_ID))
        val value = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_VALUE))

        val operator = OperatorConverter.fromIndex(operatorId)
        return Rule( id, uuid, field, name, operator, value )
    }

    fun getRulesForField(field : Field) : List<Rule>
    {
        val rules = ArrayList<Rule>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_FIELD_ID} = '${field.id}'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            rules.add( buildRule( cursor, field ))
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