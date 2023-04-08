package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import com.google.android.gms.maps.model.LatLng
import edu.gtri.gpssample.database.models.EnumArea

class EnumAreaDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createEnumArea( enumArea: EnumArea) : Int
    {
        val values = ContentValues()

        putEnumArea( enumArea, values )

        return dao.writableDatabase.insert(DAO.TABLE_ENUM_AREA, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putEnumArea( enumArea: EnumArea, values: ContentValues )
    {
        values.put( DAO.COLUMN_CONFIG_ID, enumArea.config_id )
        values.put( DAO.COLUMN_ENUM_AREA_NAME, enumArea.name )
        values.put( DAO.COLUMN_ENUM_AREA_TL_LAT, enumArea.topLeft.latitude )
        values.put( DAO.COLUMN_ENUM_AREA_TL_LON, enumArea.topLeft.longitude )
        values.put( DAO.COLUMN_ENUM_AREA_TR_LAT, enumArea.topRight.latitude )
        values.put( DAO.COLUMN_ENUM_AREA_TR_LON, enumArea.topRight.longitude )
        values.put( DAO.COLUMN_ENUM_AREA_BR_LAT, enumArea.botRight.latitude )
        values.put( DAO.COLUMN_ENUM_AREA_BR_LON, enumArea.botRight.longitude )
        values.put( DAO.COLUMN_ENUM_AREA_BL_LAT, enumArea.botLeft.latitude )
        values.put( DAO.COLUMN_ENUM_AREA_BL_LON, enumArea.botLeft.longitude )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createEnumArea(cursor: Cursor): EnumArea
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val config_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_NAME))
        val tl_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_TL_LAT))
        val tl_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_TL_LON))
        val tr_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_TR_LAT))
        val tr_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_TR_LON))
        val br_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_BR_LAT))
        val br_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_BR_LON))
        val bl_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_BL_LAT))
        val bl_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_BL_LON))

        return EnumArea( id, config_id, name, LatLng(tl_lat,tl_lon), LatLng(tr_lat,tr_lon), LatLng(br_lat,br_lon), LatLng(bl_lat,bl_lon))
    }

    //--------------------------------------------------------------------------
    fun getEnumArea( id: Int ): EnumArea?
    {
        var enumArea: EnumArea? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            enumArea = createEnumArea( cursor )
        }

        cursor.close()
        db.close()

        return enumArea
    }

    //--------------------------------------------------------------------------
    fun getEnumAreas( config_id: Int ): List<EnumArea>
    {
        val enumAreas = ArrayList<EnumArea>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_CONFIG_ID} = $config_id"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumAreas.add( createEnumArea( cursor ))
        }

        cursor.close()
        db.close()

        return enumAreas
    }
}