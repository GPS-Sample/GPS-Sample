package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.LatLon

class LatLonDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createLatLon( latLon: LatLon ) : Int
    {
        val values = ContentValues()

        putLatLon( latLon, values )

        return dao.writableDatabase.insert(DAO.TABLE_LAT_LON, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putLatLon(latLon: LatLon, values: ContentValues)
    {
        values.put( DAO.COLUMN_LAT, latLon.latitude )
        values.put( DAO.COLUMN_LON, latLon.longitude )
        values.put( DAO.COLUMN_ENUM_AREA_ID, latLon.enumAreaId )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createLatLon(cursor: Cursor): LatLon
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LAT))
        val lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LON))
        val enumAreaId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_ID))

        return LatLon( id, lat, lon, enumAreaId )
    }

    //--------------------------------------------------------------------------
    fun getLatLons( enum_area_id: Int ): ArrayList<LatLon>
    {
        var latLons = ArrayList<LatLon>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_LAT_LON} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enum_area_id"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            latLons.add( createLatLon( cursor ))
        }

        cursor.close()
        db.close()

        return latLons
    }
}
