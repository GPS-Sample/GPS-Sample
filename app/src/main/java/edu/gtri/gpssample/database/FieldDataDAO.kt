package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getLongOrNull
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumData
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.extensions.toBoolean

class FieldDataDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createFieldData( fieldData: FieldData ) : Int
    {
        val values = ContentValues()

        putFieldData( fieldData, values )

        return dao.writableDatabase.insert(DAO.TABLE_FIELD_DATA, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putFieldData(fieldData: FieldData, values: ContentValues)
    {
        fieldData.id?.let {
            values.put( DAO.COLUMN_ID, it )
        }

        values.put( DAO.COLUMN_FIELD_ID, fieldData.fieldId )
        values.put( DAO.COLUMN_ENUM_DATA_ID, fieldData.enumDataId )
        values.put( DAO.COLUMN_FIELD_DATA_TEXT_VALUE, fieldData.textValue )
        values.put( DAO.COLUMN_FIELD_DATA_NUMBER_VALUE, fieldData.numberValue )
        values.put( DAO.COLUMN_FIELD_DATA_DATE_VALUE, fieldData.dateValue )
        values.put( DAO.COLUMN_FIELD_DATA_DROPDOWN_INDEX, fieldData.dropdownIndex )
        values.put( DAO.COLUMN_FIELD_DATA_CHECKBOX1, fieldData.checkbox1 )
        values.put( DAO.COLUMN_FIELD_DATA_CHECKBOX2, fieldData.checkbox2 )
        values.put( DAO.COLUMN_FIELD_DATA_CHECKBOX3, fieldData.checkbox3 )
        values.put( DAO.COLUMN_FIELD_DATA_CHECKBOX4, fieldData.checkbox4 )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createFieldData(cursor: Cursor): FieldData
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val field_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_ID))
        val enum_data_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_ID))
        val textValue = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_TEXT_VALUE))
        val numberValue = cursor.getDoubleOrNull(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_NUMBER_VALUE))
        val dateValue = cursor.getLongOrNull(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_DATE_VALUE))
        val dropdownIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_DROPDOWN_INDEX))
        val checkbox1 = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_CHECKBOX1)).toBoolean()
        val checkbox2 = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_CHECKBOX2)).toBoolean()
        val checkbox3 = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_CHECKBOX3)).toBoolean()
        val checkbox4 = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_CHECKBOX4)).toBoolean()

        return FieldData( id, field_id, enum_data_id, textValue, numberValue, dateValue, dropdownIndex, checkbox1, checkbox2, checkbox3, checkbox4 )
    }

    //--------------------------------------------------------------------------
    fun updateFieldData( fieldData: FieldData )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        fieldData.id?.let { id ->
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putFieldData( fieldData, values )

            db.update(DAO.TABLE_FIELD_DATA, values, whereClause, args )
            db.close()
        }
    }

    fun printAllFieldData()
    {
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val fieldData = createFieldData( cursor )
        }

        cursor.close()
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getOrCreateFieldData( field_id: Int, enum_data_id: Int ): FieldData
    {
        var fieldData: FieldData? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA} WHERE ${DAO.COLUMN_FIELD_ID} = $field_id AND ${DAO.COLUMN_ENUM_DATA_ID} = $enum_data_id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            fieldData = createFieldData( cursor )
        }
        else
        {
            fieldData = FieldData( field_id, enum_data_id )
            fieldData.id = createFieldData( fieldData )
        }

        cursor.close()
        db.close()

        return fieldData
    }

    //--------------------------------------------------------------------------
    fun getFieldDataList( enum_data_id: Int ): ArrayList<FieldData>
    {
        var fieldDataList = ArrayList<FieldData>()

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA} WHERE ${DAO.COLUMN_ENUM_DATA_ID} = $enum_data_id"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            fieldDataList.add( createFieldData( cursor ))
        }

        cursor.close()
        db.close()

        return fieldDataList
    }

    fun delete( fieldData: FieldData )
    {
        fieldData.id?.let {field_data_id ->
            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(field_data_id.toString())

            db.delete(DAO.TABLE_FIELD_DATA, whereClause, args)
            db.close()
        }
    }

    //--------------------------------------------------------------------------
    fun deleteAllFields( enum_data_id: Int )
    {
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA} WHERE ${DAO.COLUMN_ENUM_DATA_ID} = $enum_data_id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            val fieldData = createFieldData( cursor )
            delete( fieldData )
        }

        cursor.close()
        db.close()
    }
}
