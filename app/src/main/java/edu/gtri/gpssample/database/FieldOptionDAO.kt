package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldOption

class FieldOptionDAO(private var dao: DAO)
{
    fun createOrUpdateFieldOption( fieldOption: FieldOption, field: Field ) : FieldOption?
    {
        if (exists( fieldOption ))
        {
            updateFieldOption( fieldOption )
            Log.d( "xxx", "Updated FieldOption with ID = ${fieldOption.uuid}")
        }
        else
        {
            val values = ContentValues()
            putFieldOption( fieldOption, values )
            if (dao.writableDatabase.insert(DAO.TABLE_FIELD_OPTION, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "Created FieldOption with ID = ${fieldOption.uuid}")
        }

        createConnection( fieldOption, field )

        return fieldOption
    }

    private fun createConnection( fieldOption: FieldOption, field: Field )
    {
        val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_FIELD__FIELD_OPTION} WHERE ${DAO.COLUMN_FIELD_UUID} = '${field.uuid}' AND ${DAO.COLUMN_FIELD_OPTION_UUID} = '${fieldOption.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)
        if (cursor.count == 0)
        {
            val values = ContentValues()
            values.put( DAO.COLUMN_FIELD_UUID, field.uuid )
            values.put( DAO.COLUMN_FIELD_OPTION_UUID, fieldOption.uuid )
            dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_FIELD__FIELD_OPTION, null, values).toInt()
        }
    }

    fun putFieldOption( fieldOption: FieldOption, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, fieldOption.uuid )
        values.put( DAO.COLUMN_FIELD_OPTION_NAME, fieldOption.name )
    }

    fun updateFieldOption( fieldOption: FieldOption )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(fieldOption.uuid)
        val values = ContentValues()

        putFieldOption( fieldOption, values )

        dao.writableDatabase.update(DAO.TABLE_FIELD_OPTION, values, whereClause, args )
    }

    fun exists( fieldOption: FieldOption): Boolean
    {
        getFieldOption( fieldOption.uuid )?.let {
            return true
        } ?: return false
    }

    @SuppressLint("Range")
    private fun  buildFieldOption(cursor: Cursor ): FieldOption
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_NAME))

        return FieldOption(uuid, name)
    }

    fun getFieldOption( uuid : String ): FieldOption?
    {
        var fieldOption: FieldOption? = null
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_OPTION} where ${DAO.COLUMN_UUID} = '${uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            fieldOption = buildFieldOption( cursor )
        }

        cursor.close()

        return fieldOption
    }

    @SuppressLint("Range")
    fun getFieldOptions( field: Field ) : ArrayList<FieldOption>
    {
        val fieldOptions = ArrayList<FieldOption>()

        val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_FIELD__FIELD_OPTION} where ${DAO.COLUMN_FIELD_UUID} = '${field.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val fieldOptionId = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_UUID))
            val fieldOption = getFieldOption( fieldOptionId )
            fieldOption?.let {
                fieldOptions.add( it )
            }
        }

        cursor.close()

        return fieldOptions
    }

    fun deleteFieldOption( fieldOption: FieldOption )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(fieldOption.uuid)

        dao.writableDatabase.delete(DAO.TABLE_FIELD_OPTION, whereClause, args)
    }
}