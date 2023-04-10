package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.EnumData

class EnumDataDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createEnumData( enumData: EnumData) : Int
    {
        val values = ContentValues()

        putEnumData( enumData, values )

        return dao.writableDatabase.insert(DAO.TABLE_ENUM_DATA, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putEnumData(enumData: EnumData, values: ContentValues)
    {
        values.put( DAO.COLUMN_USER_ID, enumData.userId )
        values.put( DAO.COLUMN_STUDY_ID, enumData.studyId )
        values.put( DAO.COLUMN_ENUM_DATA_LATITUDE, enumData.latitude )
        values.put( DAO.COLUMN_ENUM_DATA_LONGITUDE, enumData.longitude )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createEnumData(cursor: Cursor): EnumData
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val user_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_USER_ID))
        val study_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val latitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_LATITUDE))
        val longitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_LONGITUDE))

        return EnumData( id, user_id, study_id, latitude, longitude )
    }

    //--------------------------------------------------------------------------
    fun getEnumData( id: Int ): EnumData?
    {
        var enumData: EnumData? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_DATA} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            enumData = createEnumData( cursor )
        }

        cursor.close()
        db.close()

        return enumData
    }
}