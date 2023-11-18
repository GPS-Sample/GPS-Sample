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
       // val operatorId = OperatorConverter.toIndex(rule.operator)

        rule.id?.let { id ->
            Log.d( "xxx", "existing filter id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        rule.field?.let{field->
            values.put( DAO.COLUMN_FIELD_ID, field.id )
            values.put( DAO.COLUMN_RULE_NAME, rule.name )
            rule.operator?.let{operator ->
                values.put( DAO.COLUMN_OPERATOR_ID, OperatorConverter.toIndex(operator) )
            }
            rule.filterOperator?.id?.let{filterOperatorId ->
                values.put(DAO.COLUMN_FILTEROPERATOR_ID, filterOperatorId)
            }
            values.put( DAO.COLUMN_RULE_VALUE, rule.value )
        }

    }

    //--------------------------------------------------------------------------
    fun updateRule( rule: Rule )
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(rule.id.toString())
        val values = ContentValues()

        putRule( rule, values )

        dao.writableDatabase.update(DAO.TABLE_RULE, values, whereClause, args )
    }

    @SuppressLint("Range")
    private fun  buildRule(cursor: Cursor): Rule?
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val fieldId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_ID))
        val field = DAO.fieldDAO.getField(fieldId)
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_NAME))

        // TODO:  this should be a lookup table
        val operatorId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_OPERATOR_ID))
        val value = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_VALUE))

        val operator = OperatorConverter.fromIndex(operatorId)
        field?.let{rule->
            return Rule( id, field, name, value, operator, null )
        }
        return null
    }

    fun getRule( id: Int ) : Rule?
    {
        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_ID} = ${id}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            return buildRule( cursor )
        }

        cursor.close()

        return null
    }

    fun getRuleByUUID(uuid : String) : Rule?
    {
        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_UUID} = ${uuid}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            return buildRule( cursor )
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
                rules.add( rule)
            }

        }

        cursor.close()

        return rules
    }

    fun getRulesForField( field : Field) : ArrayList<Rule>
    {
        val rules = ArrayList<Rule>()

        field.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_FIELD_ID} = '${id}'"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val rule = buildRule( cursor )
                rule?.let{rule->
                    rules.add( rule)
                }
            }

            cursor.close()
        }

        return rules
    }


//    fun getRules( study: Study ) : ArrayList<Rule>
//    {
//        val rules = ArrayList<Rule>()
//
//        study.id?.let { id ->
//            val db = dao.writableDatabase
//            val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_STUDY_ID} = '${id}'"
//            val cursor = db.rawQuery(query, null)
//
//            while (cursor.moveToNext())
//            {
//                val rule = buildRule( cursor )
//                rule?.let{rule->
//                    rules.add( rule)
//                }
//            }
//
//            cursor.close()
//            db.close()
//        }
//
//        return rules
//    }

    //--------------------------------------------------------------------------
    fun deleteRule( rule: Rule )
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(rule.id.toString())

        dao.writableDatabase.delete(DAO.TABLE_RULE, whereClause, args)
    }
}