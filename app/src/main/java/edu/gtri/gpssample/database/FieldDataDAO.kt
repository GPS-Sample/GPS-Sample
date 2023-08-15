package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.FieldData
import edu.gtri.gpssample.extensions.toBoolean

class FieldDataDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateFieldData( fieldData: FieldData, enumerationItem: EnumerationItem ) : FieldData?
    {
        if (exists( fieldData ))
        {
            updateFieldData( fieldData )
        }
        else
        {
            Log.d("XXXXX", "the field name ${fieldData.name}")
            val values = ContentValues()

            putFieldData( fieldData, values, enumerationItem )

            fieldData.id = dao.writableDatabase.insert(DAO.TABLE_FIELD_DATA, null, values).toInt()
            fieldData.id?.let { id ->
                Log.d( "xxx", "new fieldData id = ${id}")
            } ?: return null
        }

        return fieldData
    }

    //--------------------------------------------------------------------------
    fun putFieldData(fieldData: FieldData, values: ContentValues, enumerationItem: EnumerationItem?)
    {
        fieldData.id?.let {
            values.put( DAO.COLUMN_ID, it )
        }

        enumerationItem?.id?.let {
            values.put( DAO.COLUMN_ENUMERATION_ITEM_ID, it )
        }

        values.put( DAO.COLUMN_UUID, fieldData.uuid )
        values.put( DAO.COLUMN_FIELD_ID, fieldData.field.id )

        values.put(DAO.COLUMN_FIELD_NAME, fieldData.name)
        values.put(DAO.COLUMN_FIELD_TYPE_INDEX, FieldTypeConverter.toIndex(fieldData.type))
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
    fun exists( fieldData: FieldData): Boolean
    {
        fieldData.id?.let { id ->
            getFieldData( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createFieldData(cursor: Cursor): FieldData
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val field_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_ID))

        val field = DAO.fieldDAO.getField(field_id)
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_NAME))
        val type = FieldTypeConverter.fromIndex(cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_TYPE_INDEX)))
        val textValue = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_TEXT_VALUE))
        val numberValue = cursor.getDoubleOrNull(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_NUMBER_VALUE))
        val dateValue = cursor.getLongOrNull(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_DATE_VALUE))
        val dropdownIndex = cursor.getIntOrNull(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_DROPDOWN_INDEX))
        val checkbox1 = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_CHECKBOX1)).toBoolean()
        val checkbox2 = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_CHECKBOX2)).toBoolean()
        val checkbox3 = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_CHECKBOX3)).toBoolean()
        val checkbox4 = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_CHECKBOX4)).toBoolean()

        return FieldData( id, uuid, field!!, name, type, textValue, numberValue, dateValue, dropdownIndex, checkbox1, checkbox2, checkbox3, checkbox4 )
    }

    //--------------------------------------------------------------------------
    fun updateFieldData( fieldData: FieldData )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        fieldData.id?.let { id ->
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putFieldData( fieldData, values, null )

            db.update(DAO.TABLE_FIELD_DATA, values, whereClause, args )
            db.close()
        }
    }

    //--------------------------------------------------------------------------
    fun getFieldData( id: Int ): FieldData?
    {
        var fieldData: FieldData? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            fieldData = createFieldData( cursor )
        }

        cursor.close()
        db.close()

        return fieldData
    }

    //--------------------------------------------------------------------------
    fun getOrCreateFieldData( field_id: Int, enumerationItem: EnumerationItem  ): FieldData
    {
        var fieldData: FieldData? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA} WHERE ${DAO.COLUMN_FIELD_ID} = $field_id AND ${DAO.COLUMN_ENUMERATION_ITEM_ID} = $enumerationItem.id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            fieldData = createFieldData( cursor )
        }
        else
        {
            val field = DAO.fieldDAO.getField(field_id)
            fieldData = FieldData( field!! )
            createOrUpdateFieldData( fieldData, enumerationItem )
        }

        cursor.close()
        db.close()

        return fieldData
    }

    //--------------------------------------------------------------------------
    fun getFieldDataList( enumerationItem: EnumerationItem ): ArrayList<FieldData>
    {
        var fieldDataList = ArrayList<FieldData>()
        val db = dao.writableDatabase

        enumerationItem.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA} WHERE ${DAO.COLUMN_ENUMERATION_ITEM_ID} = $id"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                fieldDataList.add( createFieldData( cursor ))
            }

            cursor.close()
        }

        db.close()

        return fieldDataList
    }

    fun getFieldData(): ArrayList<FieldData>
    {
        var fieldDataList = ArrayList<FieldData>()
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            fieldDataList.add( createFieldData( cursor ))
        }

        cursor.close()

        db.close()

        return fieldDataList
    }

    //--------------------------------------------------------------------------
    fun delete( fieldData: FieldData )
    {
        fieldData.id?.let {id ->
            Log.d( "xxx", "deleting fieldData with id $id" )

            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            db.delete(DAO.TABLE_FIELD_DATA, whereClause, args)
            db.close()
        }
    }

    //--------------------------------------------------------------------------
    fun deleteAllFields( enumerationItem: EnumerationItem )
    {
        val fieldDataList = getFieldDataList( enumerationItem )

        for (fieldData in fieldDataList)
        {
            delete( fieldData )
        }
    }
}
