package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.*

class CircleDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createCircle( circle: Circle) : Int
    {
        val values = ContentValues()

        putCircle( circle, values )

        return dao.writableDatabase.insert(DAO.TABLE_CIRCLE, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putCircle( circle: Circle, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, circle.uuid )
        values.put( DAO.COLUMN_CIRCLE_LAT, circle.lat )
        values.put( DAO.COLUMN_CIRCLE_LON, circle.lon )
        values.put( DAO.COLUMN_CIRCLE_RADIUS, circle.radius )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createCircle(cursor: Cursor): Circle
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_CIRCLE_LAT))
        val lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_CIRCLE_LON))
        val radius = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_CIRCLE_RADIUS))

        return Circle( id, uuid, lat, lon, radius )
    }

    //--------------------------------------------------------------------------
    fun updateCircle( circle: Circle )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(circle.uuid)
        val values = ContentValues()

        putCircle( circle, values )

        db.update(DAO.TABLE_CIRCLE, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getCircle( uuid: String ): Circle?
    {
        var circle: Circle? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_CIRCLE} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            circle = createCircle( cursor )
        }

        cursor.close()
        db.close()

        return circle
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getCircle( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun deleteCircle( circle: Circle )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(circle.uuid.toString())

        db.delete(DAO.TABLE_CIRCLE, whereClause, args)
        db.close()
    }
}