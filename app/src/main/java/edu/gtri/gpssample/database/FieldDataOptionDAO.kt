package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.database.models.FieldDataOption
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.extensions.toBoolean

class FieldDataOptionDAO(private var dao: DAO)
{
    fun createOrUpdateFieldDataOption(fieldDataOption: FieldDataOption, obj: Any) : FieldDataOption?
    {
        val existingFieldDataOption = getFieldDataOption( fieldDataOption.uuid )

        if (existingFieldDataOption != null)
        {
            if (fieldDataOption.doesNotEqual( existingFieldDataOption ))
            {
                updateFieldDataOption( fieldDataOption )
                Log.d( "xxx", "Updated FieldDataOption with ID ${fieldDataOption.uuid}")
            }
        }
        else
        {
            val values = ContentValues()
            putFieldDataOption( fieldDataOption, values )
            if (dao.writableDatabase.insert(DAO.TABLE_FIELD_DATA_OPTION, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "Created FieldDataOption with ID ${fieldDataOption.uuid}")
        }

        val fieldData = obj as? FieldData

        fieldData?.let {
            createFieldDataConnection( fieldDataOption, it )
        }

        val rule = obj as? Rule

        rule?.let {
            createRuleConnection( fieldDataOption, it )
        }

        return fieldDataOption
    }

    private fun createFieldDataConnection(fieldDataOption: FieldDataOption, fieldData: FieldData)
    {
        val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_FIELD_DATA__FIELD_DATA_OPTION} WHERE ${DAO.COLUMN_FIELD_DATA_UUID} = '${fieldData.uuid}' AND ${DAO.COLUMN_FIELD_DATA_OPTION_UUID} = '${fieldDataOption.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)
        if (cursor.count == 0)
        {
            val values = ContentValues()
            values.put( DAO.COLUMN_FIELD_DATA_UUID, fieldData.uuid )
            values.put( DAO.COLUMN_FIELD_DATA_OPTION_UUID, fieldDataOption.uuid )
            dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_FIELD_DATA__FIELD_DATA_OPTION, null, values).toInt()
        }
    }

    private fun createRuleConnection(fieldDataOption: FieldDataOption, rule: Rule)
    {
        val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_RULE__FIELD_DATA_OPTION} WHERE ${DAO.COLUMN_RULE_UUID} = '${rule.uuid}' AND ${DAO.COLUMN_FIELD_DATA_OPTION_UUID} = '${fieldDataOption.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)
        if (cursor.count == 0)
        {
            val values = ContentValues()
            values.put( DAO.COLUMN_RULE_UUID, rule.uuid )
            values.put( DAO.COLUMN_FIELD_DATA_OPTION_UUID, fieldDataOption.uuid )
            dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_RULE__FIELD_DATA_OPTION, null, values).toInt()
        }
    }

    fun putFieldDataOption(fieldDataOption: FieldDataOption, values: ContentValues)
    {
        values.put( DAO.COLUMN_UUID, fieldDataOption.uuid )
        values.put( DAO.COLUMN_FIELD_DATA_OPTION_NAME, fieldDataOption.name )
        values.put( DAO.COLUMN_FIELD_DATA_OPTION_VALUE, fieldDataOption.value )
    }

    fun updateFieldDataOption( fieldDataOption: FieldDataOption)
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(fieldDataOption.uuid)
        val values = ContentValues()

        putFieldDataOption( fieldDataOption, values )

        dao.writableDatabase.update(DAO.TABLE_FIELD_DATA_OPTION, values, whereClause, args )
    }

    fun exists( fieldDataOption: FieldDataOption): Boolean
    {
        getFieldDataOption( fieldDataOption.uuid )?.let {
            return true
        } ?: return false
    }

    fun modified( fieldDataOption : FieldDataOption ) : Boolean
    {
        getFieldDataOption( fieldDataOption.uuid )?.let {
            if (!fieldDataOption.equals(it))
            {
                return true
            }
        }

        return false
    }

    @SuppressLint("Range")
    private fun  buildFieldDataOption(cursor: Cursor): FieldDataOption
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_OPTION_NAME))
        val value = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_OPTION_VALUE)).toBoolean()

        return FieldDataOption(uuid, name, value)
    }

    fun getFieldDataOption( id : Int ): FieldDataOption?
    {
        var fieldDataOption: FieldDataOption? = null
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA_OPTION} where id=${id}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            fieldDataOption = buildFieldDataOption( cursor )
        }

        cursor.close()

        return fieldDataOption
    }

    fun getFieldDataOption( uuid: String ): FieldDataOption?
    {
        var fieldDataOption: FieldDataOption? = null
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA_OPTION} where ${DAO.COLUMN_UUID} = '${uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            fieldDataOption = buildFieldDataOption( cursor )
        }

        cursor.close()

        return fieldDataOption
    }

    @SuppressLint("Range")
    fun getFieldDataOptions( fieldData: FieldData) : ArrayList<FieldDataOption>
    {
        val fieldDataOptions = ArrayList<FieldDataOption>()

        val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_FIELD_DATA__FIELD_DATA_OPTION} where ${DAO.COLUMN_FIELD_DATA_UUID} = '${fieldData.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val fieldDataOptionId = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_OPTION_UUID))
            val fieldDataOption = getFieldDataOption( fieldDataOptionId )
            fieldDataOption?.let {
                fieldDataOptions.add( it )
            }
        }

        cursor.close()

        return fieldDataOptions
    }

    @SuppressLint("Range")
    fun getFieldDataOptions( rule: Rule ) : ArrayList<FieldDataOption>
    {
        val fieldDataOptions = ArrayList<FieldDataOption>()

        val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_RULE__FIELD_DATA_OPTION} where ${DAO.COLUMN_RULE_UUID}='${rule.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val fieldDataOptionId = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_OPTION_UUID))
            val fieldDataOption = getFieldDataOption( fieldDataOptionId )
            fieldDataOption?.let {
                fieldDataOptions.add( it )
            }
        }

        cursor.close()

        return fieldDataOptions
    }

    fun delete( fieldDataOption: FieldDataOption)
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(fieldDataOption.uuid)

        dao.writableDatabase.delete(DAO.TABLE_FIELD_DATA_OPTION, whereClause, args)
    }
}