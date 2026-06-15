/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.OperatorConverter
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.extensions.toBoolean
import java.util.*
import kotlin.collections.ArrayList

class RuleDAO(private var dao: DAO)
{
    fun createOrUpdateRule( rule: Rule, version: String ) : Rule?
    {
        rule.version = version

        val values = ContentValues()
        putRule( rule, values )

        dao.upsert( DAO.TABLE_RULE, values )

        for (fieldDataOption in rule.fieldDataOptions)
        {
            DAO.fieldDataOptionDAO.createOrUpdateFieldDataOption( fieldDataOption, rule, fieldDataOption.version )
        }

        return rule
    }

    private fun putRule( rule: Rule, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, rule.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, rule.creationDate )
        values.put( DAO.COLUMN_VERSION, rule.version )
        values.put( DAO.COLUMN_FIELD_UUID, rule.fieldUuid )
        values.put( DAO.COLUMN_RULE_NAME, rule.name )
        values.put( DAO.COLUMN_RULE_VALUE, rule.value )
        values.put( DAO.COLUMN_RULE_IS_SUBSET_RULE, rule.isSubsetRule )

        rule.operator?.let { operator ->
            values.put( DAO.COLUMN_OPERATOR_ID, OperatorConverter.toIndex(operator))
        }

        rule.filterOperator?.let { filterOperator ->
            values.put(DAO.COLUMN_FILTEROPERATOR_UUID, filterOperator.uuid)
        }
    }

    @SuppressLint("Range")
    private fun buildRule(cursor: Cursor): Rule?
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val fieldUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_NAME))
        val operatorId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_OPERATOR_ID))
        val value = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_RULE_VALUE))
        val isSubsetRule = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_RULE_IS_SUBSET_RULE)).toBoolean()

        val field = DAO.fieldDAO.getField(fieldUuid)
        val operator = OperatorConverter.fromIndex(operatorId)

        field?.let { rule->
            return Rule( uuid, creationDate, fieldUuid, name, value, isSubsetRule, operator, null, version )
        }

        return null
    }

    fun getRule( uuid: String ) : Rule?
    {
        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_UUID} = '${uuid}'"
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

    fun getPrimaryRules( field : Field ) : ArrayList<Rule>
    {
        val rules = ArrayList<Rule>()

        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_FIELD_UUID} = '${field.uuid}' AND ${DAO.COLUMN_RULE_IS_SUBSET_RULE} = 0"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val rule = buildRule( cursor )
            rule?.let { rule->
                rule.fieldDataOptions = DAO.fieldDataOptionDAO.getFieldDataOptions( rule )
                rules.add( rule)
            }
        }

        cursor.close()

        return rules
    }

    fun getSubsetRules( field : Field ) : ArrayList<Rule>
    {
        val rules = ArrayList<Rule>()

        val query = "SELECT * FROM ${DAO.TABLE_RULE} WHERE ${DAO.COLUMN_FIELD_UUID} = '${field.uuid}' AND ${DAO.COLUMN_RULE_IS_SUBSET_RULE} = 1"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val rule = buildRule( cursor )
            rule?.let { rule->
                rule.fieldDataOptions = DAO.fieldDataOptionDAO.getFieldDataOptions( rule )
                rules.add( rule)
            }
        }

        cursor.close()

        return rules
    }

    fun deleteRule( rule: Rule )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(rule.uuid)

        dao.writableDatabase.delete(DAO.TABLE_RULE, whereClause, args)
    }
}