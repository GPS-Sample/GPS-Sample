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
        }
        else
        {
            val values = ContentValues()
            putFieldOption( fieldOption, values )
            fieldOption.id = dao.writableDatabase.insert(DAO.TABLE_FIELD_OPTION, null, values).toInt()
            fieldOption.id?.let { id ->
                Log.d( "xxx", "new fieldOption id = ${id}")
                createConnection( fieldOption, field )
            } ?: return null
        }

        return fieldOption
    }

    fun createConnection( fieldOption: FieldOption, field: Field )
    {
        fieldOption.id?.let { fieldOptionId ->
            field.id?.let { fieldId ->
                val values = ContentValues()
                values.put( DAO.COLUMN_FIELD_ID, fieldId )
                values.put( DAO.COLUMN_FIELD_OPTION_ID, fieldOptionId )
                dao.writableDatabase.insert(DAO.TABLE_FIELD__FIELD_OPTION, null, values).toInt()
            }
        }
    }

    fun putFieldOption( fieldOption: FieldOption, values: ContentValues )
    {
        fieldOption.id?.let { id ->
            Log.d( "xxx", "existing fieldOption id = ${id}")
            values.put( DAO.COLUMN_ID, fieldOption.id )
        }

        values.put( DAO.COLUMN_FIELD_OPTION_NAME, fieldOption.name )
    }

    fun updateFieldOption( fieldOption: FieldOption )
    {
        val db = dao.writableDatabase

        fieldOption.id?.let{ id ->
            Log.d( "xxx", "update fieldOption id ${id}")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putFieldOption( fieldOption, values )

            db.update(DAO.TABLE_FIELD_OPTION, values, whereClause, args )
        }

        db.close()
    }

    fun exists( fieldOption: FieldOption): Boolean
    {
        fieldOption.id?.let { id ->
            getFieldOption( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    @SuppressLint("Range")
    private fun  buildFieldOption(cursor: Cursor ): FieldOption
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_NAME))

        return FieldOption(id, name)
    }

    fun getFieldOption( id : Int ): FieldOption?
    {
        var fieldOption: FieldOption? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_OPTION} where id=${id}"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            fieldOption = buildFieldOption( cursor )
        }

        cursor.close()
        db.close()

        return fieldOption
    }

    @SuppressLint("Range")
    fun getFieldOptions( field: Field ) : ArrayList<FieldOption>
    {
        val fieldOptions = ArrayList<FieldOption>()
        val db = dao.writableDatabase

        field.id?.let { fieldId ->
            val query = "SELECT * FROM ${DAO.TABLE_FIELD__FIELD_OPTION} where ${DAO.COLUMN_FIELD_ID}=${fieldId}"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val fieldOptionId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_ID))
                val fieldOption = getFieldOption( fieldOptionId )
                fieldOption?.let {
                    fieldOptions.add( it )
                }
            }

            cursor.close()
        }

        db.close()

        return fieldOptions
    }

    fun deleteFieldOption( fieldOption: FieldOption )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(fieldOption.id.toString())

        db.delete(DAO.TABLE_FIELD_OPTION, whereClause, args)
        db.close()
    }
}