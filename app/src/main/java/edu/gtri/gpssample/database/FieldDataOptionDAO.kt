package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.database.models.FieldDataOption
import edu.gtri.gpssample.extensions.toBoolean

class FieldDataOptionDAO(private var dao: DAO)
{
    fun createOrUpdateFieldDataOption(fieldDataOption: FieldDataOption, fieldData: FieldData) : FieldDataOption?
    {
        if (exists( fieldDataOption ))
        {
            updateFieldDataOption( fieldDataOption )
        }
        else
        {
            val values = ContentValues()
            putFieldDataOption( fieldDataOption, values )
            fieldDataOption.id = dao.writableDatabase.insert(DAO.TABLE_FIELD_DATA_OPTION, null, values).toInt()
            fieldDataOption.id?.let { id ->
                Log.d( "xxx", "new fieldDataOption id = ${id}")
                createConnection( fieldDataOption, fieldData )
            } ?: return null
        }

        return fieldDataOption
    }

    fun createConnection(fieldDataOption: FieldDataOption, fieldData: FieldData)
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

    fun putFieldDataOption(fieldDataOption: FieldDataOption, values: ContentValues)
    {
        fieldDataOption.id?.let { id ->
            Log.d( "xxx", "existing fieldDataOption id = ${id}")
            values.put( DAO.COLUMN_ID, fieldDataOption.id )
        }

        values.put( DAO.COLUMN_FIELD_DATA_OPTION_NAME, fieldDataOption.name )
        values.put( DAO.COLUMN_FIELD_DATA_OPTION_VALUE, fieldDataOption.value )
    }

    fun updateFieldDataOption( fieldDataOption: FieldDataOption)
    {
        fieldDataOption.id?.let{ id ->
            Log.d( "xxx", "update fieldDataOption id ${id}")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putFieldDataOption( fieldDataOption, values )

            dao.writableDatabase.update(DAO.TABLE_FIELD_DATA_OPTION, values, whereClause, args )
        }
    }

    fun exists( fieldDataOption: FieldDataOption): Boolean
    {
        fieldDataOption.id?.let { id ->
            getFieldDataOption( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    @SuppressLint("Range")
    private fun  buildFieldDataOption(cursor: Cursor): FieldDataOption
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_OPTION_NAME))
        val value = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_OPTION_VALUE)).toBoolean()

        return FieldDataOption(id, name, value)
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

    fun deleteFieldDataOption( fieldDataOption: FieldDataOption)
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(fieldDataOption.id.toString())

        dao.writableDatabase.delete(DAO.TABLE_FIELD_DATA_OPTION, whereClause, args)
    }
}