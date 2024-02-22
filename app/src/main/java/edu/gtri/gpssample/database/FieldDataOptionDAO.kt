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
        if (exists( fieldDataOption ))
        {
            updateFieldDataOption( fieldDataOption )
        }
        else
        {
            fieldDataOption.id = null
            val values = ContentValues()
            putFieldDataOption( fieldDataOption, values )
            fieldDataOption.id = dao.writableDatabase.insert(DAO.TABLE_FIELD_DATA_OPTION, null, values).toInt()
            fieldDataOption.id?.let { id ->
                Log.d( "xxx", "created FieldDataOption with ID $id")
                val fieldData = obj as? FieldData
                fieldData?.let { fieldData ->
                    createFieldDataConnection( fieldDataOption, fieldData )
                }
                val rule = obj as? Rule
                rule?.let {
                    createRuleConnection( fieldDataOption, rule )
                }
            } ?: return null
        }

        return fieldDataOption
    }

    fun createFieldDataConnection(fieldDataOption: FieldDataOption, fieldData: FieldData)
    {
        fieldDataOption.id?.let { fieldDataOptionId ->
            fieldData.id?.let { fieldDataId ->
                val values = ContentValues()
                values.put( DAO.COLUMN_FIELD_DATA_ID, fieldDataId )
                values.put( DAO.COLUMN_FIELD_DATA_OPTION_ID, fieldDataOptionId )
                dao.writableDatabase.insert(DAO.TABLE_FIELD_DATA__FIELD_DATA_OPTION, null, values).toInt()
            }
        }
    }

    fun createRuleConnection(fieldDataOption: FieldDataOption, rule: Rule)
    {
        fieldDataOption.id?.let { fieldDataOptionId ->
            rule.id?.let { ruleId ->
                val values = ContentValues()
                values.put( DAO.COLUMN_RULE_ID, ruleId )
                values.put( DAO.COLUMN_FIELD_DATA_OPTION_ID, fieldDataOptionId )
                dao.writableDatabase.insert(DAO.TABLE_RULE__FIELD_DATA_OPTION, null, values).toInt()
            }
        }
    }

    fun putFieldDataOption(fieldDataOption: FieldDataOption, values: ContentValues)
    {
        fieldDataOption.id?.let { id ->
            values.put( DAO.COLUMN_ID, fieldDataOption.id )
        }

        values.put( DAO.COLUMN_UUID, fieldDataOption.uuid )
        values.put( DAO.COLUMN_FIELD_DATA_OPTION_NAME, fieldDataOption.name )
        values.put( DAO.COLUMN_FIELD_DATA_OPTION_VALUE, fieldDataOption.value )
    }

    fun updateFieldDataOption( fieldDataOption: FieldDataOption)
    {
        fieldDataOption.id?.let{ id ->
            Log.d( "xxx", "updated FieldDataOption with ID $id")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putFieldDataOption( fieldDataOption, values )

            dao.writableDatabase.update(DAO.TABLE_FIELD_DATA_OPTION, values, whereClause, args )
        }
    }

    fun exists( fieldDataOption: FieldDataOption): Boolean
    {
        getFieldDataOption( fieldDataOption.uuid )?.let {
            return true
        } ?: return false
    }

    @SuppressLint("Range")
    private fun  buildFieldDataOption(cursor: Cursor): FieldDataOption
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_OPTION_NAME))
        val value = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_OPTION_VALUE)).toBoolean()

        return FieldDataOption(id, uuid, name, value)
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
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA_OPTION} where ${DAO.COLUMN_UUID}='${uuid}'"
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

        fieldData.id?.let { fieldDataId ->
            val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA__FIELD_DATA_OPTION} where ${DAO.COLUMN_FIELD_DATA_ID}=${fieldDataId}"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val fieldDataOptionId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_OPTION_ID))
                val fieldDataOption = getFieldDataOption( fieldDataOptionId )
                fieldDataOption?.let {
                    fieldDataOptions.add( it )
                }
            }

            cursor.close()
        }

        return fieldDataOptions
    }

    @SuppressLint("Range")
    fun getFieldDataOptions( rule: Rule ) : ArrayList<FieldDataOption>
    {
        val fieldDataOptions = ArrayList<FieldDataOption>()

        rule.id?.let { ruleId ->
            val query = "SELECT * FROM ${DAO.TABLE_RULE__FIELD_DATA_OPTION} where ${DAO.COLUMN_RULE_ID}=${ruleId}"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val fieldDataOptionId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_OPTION_ID))
                val fieldDataOption = getFieldDataOption( fieldDataOptionId )
                fieldDataOption?.let {
                    fieldDataOptions.add( it )
                }
            }

            cursor.close()
        }

        return fieldDataOptions
    }

    fun delete( fieldDataOption: FieldDataOption)
    {
        fieldDataOption.id?.let { id ->
            Log.d( "xxx", "deleted FieldDataOption with ID $id")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            dao.writableDatabase.delete(DAO.TABLE_FIELD_DATA_OPTION, whereClause, args)
        }
    }
}