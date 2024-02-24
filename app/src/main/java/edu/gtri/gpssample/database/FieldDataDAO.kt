package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean

class FieldDataDAO(private var dao: DAO)
{
    fun createOrUpdateFieldData( fieldData: FieldData, enumerationItem: EnumerationItem ) : FieldData?
    {
        if (exists( fieldData ))
        {
            if (modified( fieldData ))
            {
                updateFieldData( fieldData )
            }
        }
        else
        {
            fieldData.id = null
            val values = ContentValues()
            putFieldData( fieldData, values, enumerationItem )
            fieldData.id = dao.writableDatabase.insert(DAO.TABLE_FIELD_DATA, null, values).toInt()
            fieldData.id?.let { id ->
                Log.d( "xxx", "created FieldData with ID $id" )
            }
        }

        fieldData.id?.let { id ->
            for (fieldDataOption in fieldData.fieldDataOptions)
            {
                DAO.fieldDataOptionDAO.createOrUpdateFieldDataOption( fieldDataOption, fieldData )
            }
        } ?: return null

        return fieldData
    }

    fun putFieldData(fieldData: FieldData, values: ContentValues, enumerationItem: EnumerationItem?)
    {
        fieldData.id?.let {
            values.put( DAO.COLUMN_ID, it )
        }

        enumerationItem?.id?.let {
            values.put( DAO.COLUMN_ENUMERATION_ITEM_ID, it )
        }

        fieldData.field?.let { field ->
            values.put( DAO.COLUMN_FIELD_ID, field.id )
        }

        values.put( DAO.COLUMN_UUID, fieldData.uuid )
        values.put( DAO.COLUMN_FIELD_NAME, fieldData.name )
        values.put( DAO.COLUMN_FIELD_TYPE_INDEX, FieldTypeConverter.toIndex(fieldData.type))
        values.put( DAO.COLUMN_FIELD_DATA_TEXT_VALUE, fieldData.textValue )
        values.put( DAO.COLUMN_FIELD_DATA_NUMBER_VALUE, fieldData.numberValue )
        values.put( DAO.COLUMN_FIELD_DATA_DATE_VALUE, fieldData.dateValue )
        values.put( DAO.COLUMN_FIELD_DATA_DROPDOWN_INDEX, fieldData.dropdownIndex )
        values.put( DAO.COLUMN_FIELD_DATA_BLOCK_NUMBER, fieldData.blockNumber )
    }

    fun exists( fieldData: FieldData ): Boolean
    {
        getFieldData( fieldData.uuid )?.let {
            return true
        } ?: return false
    }

    fun modified( fieldData : FieldData ) : Boolean
    {
        getFieldData( fieldData.uuid )?.let {
            if (!fieldData.equals(it))
            {
                fieldData.id = it.id
                return true
            }
        }

        return false
    }

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
        val blockNumber = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_BLOCK_NUMBER))

        return FieldData( id, uuid, field, name, type, textValue, numberValue, dateValue, dropdownIndex, blockNumber, ArrayList<FieldDataOption>())
    }

    fun updateFieldData( fieldData: FieldData )
    {
        fieldData.id?.let { id ->
            Log.d( "xxx", "updated FieldData with ID $id" )

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putFieldData( fieldData, values, null )

            dao.writableDatabase.update(DAO.TABLE_FIELD_DATA, values, whereClause, args )
        }
    }

    fun getFieldData( id: Int ): FieldData?
    {
        var fieldData: FieldData? = null
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            fieldData = createFieldData( cursor )
            fieldData.fieldDataOptions = DAO.fieldDataOptionDAO.getFieldDataOptions( fieldData )
        }

        cursor.close()

        return fieldData
    }

    fun getFieldData( uuid: String ): FieldData?
    {
        var fieldData: FieldData? = null
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA} WHERE ${DAO.COLUMN_UUID}='$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            fieldData = createFieldData( cursor )
            fieldData.fieldDataOptions = DAO.fieldDataOptionDAO.getFieldDataOptions( fieldData )
        }

        cursor.close()

        return fieldData
    }

    fun getFieldDataList( enumerationItem: EnumerationItem ): ArrayList<FieldData>
    {
        val fieldDataList = ArrayList<FieldData>()

        enumerationItem.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA} WHERE ${DAO.COLUMN_ENUMERATION_ITEM_ID} = $id"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val fieldData = createFieldData( cursor )
                fieldData.fieldDataOptions = DAO.fieldDataOptionDAO.getFieldDataOptions( fieldData )
                fieldDataList.add( fieldData )
            }

            cursor.close()
        }

        return fieldDataList
    }

    fun getFieldData(): ArrayList<FieldData>
    {
        val fieldDataList = ArrayList<FieldData>()

        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            fieldDataList.add( createFieldData( cursor ))
        }

        cursor.close()

        return fieldDataList
    }

    fun delete( fieldData: FieldData )
    {
        fieldData.id?.let { id ->
            Log.d( "xxx", "deleted FieldData with ID $id" )

            for (fieldDataOption in fieldData.fieldDataOptions)
            {
                DAO.fieldDataOptionDAO.delete(fieldDataOption)
            }

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            dao.writableDatabase.delete(DAO.TABLE_FIELD_DATA, whereClause, args)
        }
    }
}
