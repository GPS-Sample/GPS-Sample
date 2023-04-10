package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.FieldData

class FieldDataDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createFieldData( fieldData: FieldData) : Int
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
        values.put( DAO.COLUMN_FIELD_ID, fieldData.fieldId )
        values.put( DAO.COLUMN_ENUM_DATA_ID, fieldData.enumDataId )
        values.put( DAO.COLUMN_FIELD_DATA_RESPONSE, fieldData.response )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createFieldData(cursor: Cursor): FieldData
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val field_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_ID))
        val enum_data_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_ID))
        val response = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_RESPONSE))

        return FieldData( id, field_id, enum_data_id, response )
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
}
