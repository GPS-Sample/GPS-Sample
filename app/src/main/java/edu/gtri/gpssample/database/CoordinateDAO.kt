package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.Coordinate

class CoordinateDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createCoordinate( coordinate: Coordinate) : Int
    {
        val values = ContentValues()

        putCoordinate( coordinate, values )

        return dao.writableDatabase.insert(DAO.TABLE_COORDINATE, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putCoordinate( coordinate: Coordinate, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, coordinate.uuid )
        values.put( DAO.COLUMN_COORDINATE_LAT, coordinate.lat )
        values.put( DAO.COLUMN_COORDINATE_LON, coordinate.lon )
    }

    //--------------------------------------------------------------------------
    private fun createCoordinate( cursor: Cursor): Coordinate
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_COORDINATE_LAT))
        val lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_COORDINATE_LON))

        return Coordinate( uuid, lat, lon )
    }

    //--------------------------------------------------------------------------
    fun updateCoordinate( coordinate: Coordinate )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(coordinate.uuid)
        val values = ContentValues()

        putCoordinate( coordinate, values )

        db.update(DAO.TABLE_COORDINATE, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getCoordinate( uuid: String ): Coordinate?
    {
        var coordinate: Coordinate? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_COORDINATE} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            coordinate = createCoordinate( cursor )
        }

        cursor.close()
        db.close()

        return coordinate
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getCoordinate( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun deleteCoordinate( coordinate: Coordinate )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(coordinate.uuid.toString())

        db.delete(DAO.TABLE_COORDINATE, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteOrphans()
    {
    }
}