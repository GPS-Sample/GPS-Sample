package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.*

class RectangleDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createRectangle( rectangle: Rectangle) : Int
    {
        val values = ContentValues()

        putRectangle( rectangle, values )

        return dao.writableDatabase.insert(DAO.TABLE_RECTANGLE, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putRectangle( rectangle: Rectangle, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, rectangle.uuid )
        values.put( DAO.COLUMN_RECTANGLE_TL_LAT, rectangle.topLeft_lat )
        values.put( DAO.COLUMN_RECTANGLE_TL_LON, rectangle.topLeft_lon )
        values.put( DAO.COLUMN_RECTANGLE_TR_LAT, rectangle.topRight_lat )
        values.put( DAO.COLUMN_RECTANGLE_TR_LON, rectangle.topRight_lon )
        values.put( DAO.COLUMN_RECTANGLE_BR_LAT, rectangle.botRight_lat )
        values.put( DAO.COLUMN_RECTANGLE_BR_LON, rectangle.botRight_lon )
        values.put( DAO.COLUMN_RECTANGLE_BL_LAT, rectangle.botLeft_lat )
        values.put( DAO.COLUMN_RECTANGLE_BL_LON, rectangle.botLeft_lon )
    }

    //--------------------------------------------------------------------------
    private fun createRectangle( cursor: Cursor): Rectangle
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val tl_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_RECTANGLE_TL_LAT))
        val tl_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_RECTANGLE_TL_LON))
        val tr_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_RECTANGLE_TR_LAT))
        val tr_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_RECTANGLE_TR_LON))
        val br_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_RECTANGLE_BR_LAT))
        val br_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_RECTANGLE_BR_LON))
        val bl_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_RECTANGLE_BL_LAT))
        val bl_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_RECTANGLE_BL_LON))

        return Rectangle( uuid, tl_lat, tl_lon, tr_lat, tr_lon, br_lat, br_lon, bl_lat, bl_lon )
    }

    //--------------------------------------------------------------------------
    fun updateRectangle( rectangle: Rectangle )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(rectangle.uuid)
        val values = ContentValues()

        putRectangle( rectangle, values )

        db.update(DAO.TABLE_RECTANGLE, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getRectangle( uuid: String ): Rectangle?
    {
        var rectangle: Rectangle? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_RECTANGLE} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            rectangle = createRectangle( cursor )
        }

        cursor.close()
        db.close()

        return rectangle
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getRectangle( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun deleteRectangle( rectangle: Rectangle )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(rectangle.uuid.toString())

        db.delete(DAO.TABLE_RECTANGLE, whereClause, args)
        db.close()
    }
}