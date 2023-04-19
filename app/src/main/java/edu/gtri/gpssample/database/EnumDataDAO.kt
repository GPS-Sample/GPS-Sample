package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.EnumData
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.extensions.toInt

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
    fun updateEnumData( enumData: EnumData)
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(enumData.id!!.toString())
        val values = ContentValues()

        putEnumData( enumData, values )

        db.update(DAO.TABLE_ENUM_DATA, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun putEnumData(enumData: EnumData, values: ContentValues)
    {
        values.put( DAO.COLUMN_USER_ID, enumData.userId )
        values.put( DAO.COLUMN_ENUM_AREA_ID, enumData.enumAreaId )
        values.put( DAO.COLUMN_ENUM_DATA_LATITUDE, enumData.latitude )
        values.put( DAO.COLUMN_ENUM_DATA_LONGITUDE, enumData.longitude )
        values.put( DAO.COLUMN_ENUM_DATA_IS_LOCATION, enumData.isLocation.toInt())
        values.put( DAO.COLUMN_ENUM_DATA_DESCRIPTION, enumData.description )
        values.put( DAO.COLUMN_ENUM_DATA_IMAGE_FILE_NAME, enumData.imageFileName )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createEnumData(cursor: Cursor): EnumData
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val userId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_USER_ID))
        val enumAreaId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_ID))
        val latitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_LATITUDE))
        val longitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_LONGITUDE))
        val isLocation = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_IS_LOCATION)).toBoolean()
        val description = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_DESCRIPTION))
        val imageFileName = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_DATA_IMAGE_FILE_NAME))

        return EnumData( id, userId, enumAreaId, latitude, longitude, isLocation, description, imageFileName )
    }

    fun getEnumData( enumAreaId: Int ) : ArrayList<EnumData>
    {
        var enumDataList = ArrayList<EnumData>()

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_DATA} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumDataList.add( createEnumData( cursor ))
        }

        cursor.close()
        db.close()

        return enumDataList
    }

    fun getEnumData( userId: Int, enumAreaId: Int ) : ArrayList<EnumData>
    {
        var enumDataList = ArrayList<EnumData>()

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_DATA} WHERE ${DAO.COLUMN_USER_ID} = $userId AND ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumDataList.add( createEnumData( cursor ))
        }

        cursor.close()
        db.close()

        return enumDataList
    }

    fun delete( enumData: EnumData )
    {
        enumData.id?.let {enum_data_id ->

            DAO.fieldDataDAO.deleteAllFields( enum_data_id )

            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(enum_data_id.toString())

            db.delete(DAO.TABLE_ENUM_DATA, whereClause, args)
            db.close()
        }
    }
}