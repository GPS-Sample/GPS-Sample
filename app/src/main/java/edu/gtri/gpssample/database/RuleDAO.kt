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
            if (dao.writableDatabase.insert(DAO.TABLE_RULE, null, values) < 0)
            {
                return null
            }
        }

        for (fieldDataOption in rule.fieldDataOptions)
        {
            DAO.fieldDataOptionDAO.createOrUpdateFieldDataOption( fieldDataOption, rule )
        }

        return rule
    }

    @SuppressLint("Range")
    fun findRuleId(rule : Rule) : Int?
    {
        rule.field?.let { field ->
//            // look for the field cause it may exist and the copy doesn't have the id
//            val field_id = DAO.fieldDAO.findFieldId(field)
//            field.id = field_id

            rule.operator?.let { operator ->
                val query = "SELECT ${DAO.COLUMN_ID} FROM ${DAO.TABLE_RULE} WHERE " +
                        "${DAO.COLUMN_FIELD_UUID} = '${field.uuid}' AND ${DAO.COLUMN_RULE_NAME} = '${rule.name}' " +
                        "AND ${DAO.COLUMN_OPERATOR_ID} = '${OperatorConverter.toIndex(operator)}' AND " +
                        "${DAO.COLUMN_RULE_VALUE} = '${rule.value}'"

                val cursor = dao.writableDatabase.rawQuery(query, null)

                while (cursor.moveToNext()) {
                    return cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
                }
            }
        }
        return null
    }
    fun exists( rule: Rule ): Boolean
    {
        getRule( rule.uuid )?.let {
            return true
        } ?:return false
    }

    private fun putRule( rule: Rule, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, rule.uuid )
        values.put( DAO.COLUMN_RULE_NAME, rule.name )
        values.put( DAO.COLUMN_RULE_VALUE, rule.value )

        rule.field?.let { field ->
            values.put(DAO.COLUMN_FIELD_UUID, field.uuid)
        }

        rule.operator?.let{operator ->
            values.put( DAO.COLUMN_OPERATOR_ID, OperatorConverter.toIndex(operator) )
        }

        rule.filterOperator?.let { filterOperator ->
            values.put(DAO.COLUMN_FILTEROPERATOR_UUID, filterOperator.uuid)
        }
    }

    fun updateRule( rule: Rule )
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(rule.uuid)
        val values = ContentValues()

        putRule( rule, values )

        dao.writableDatabase.update(DAO.TABLE_RULE, values, whereClause, args )
    }

    @SuppressLint("Range")
    private fun buildRule(cursor: Cursor): Rule?
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val fieldId = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_NAME))

        // TODO:  this should be a lookup table
        val operatorId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_OPERATOR_ID))
        val value = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_VALUE))

        val field = DAO.fieldDAO.getField(fieldId)
        val operator = OperatorConverter.fromIndex(operatorId)

        field?.let{rule->
            return Rule( id, field, name, value, operator, null )
        }

        return null
    }

    fun getRule( uuid: String ) : Rule?
    {
        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_UUID} = ${uuid}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            buildRule( cursor )?.let { rule ->
                rule.fieldDataOptions = DAO.fieldDataOptionDAO.getFieldDataOptions( rule )
                return rule
            }
        }

        cursor.close()

        return null
    }

    fun getRules() : ArrayList<Rule>
    {
        val rules = ArrayList<Rule>()

        val query = "SELECT * FROM ${DAO.TABLE_RULE}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val rule = buildRule( cursor )
            rule?.let{rule->
                rule.fieldDataOptions = DAO.fieldDataOptionDAO.getFieldDataOptions( rule )
                rules.add( rule)
            }
        }

        cursor.close()

        return rules
    }

    fun getRulesForField( field : Field) : ArrayList<Rule>
    {
        val rules = ArrayList<Rule>()

        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_FIELD_UUID} = '${field.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val rule = buildRule( cursor )
            rule?.let{rule->
                rule.fieldDataOptions = DAO.fieldDataOptionDAO.getFieldDataOptions( rule )
                rules.add( rule)
            }
        }

        cursor.close()

        return rules
    }

    fun deleteRule( rule: Rule )
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(rule.uuid)

        dao.writableDatabase.delete(DAO.TABLE_RULE, whereClause, args)
    }
}