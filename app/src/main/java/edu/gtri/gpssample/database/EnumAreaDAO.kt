package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import com.google.android.gms.maps.model.LatLng
import edu.gtri.gpssample.database.models.EnumArea

class EnumAreaDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createEnumArea( enumArea: EnumArea ) : Int
    {
        val values = ContentValues()

        putEnumArea( enumArea, values )

        enumArea.id = dao.writableDatabase.insert(DAO.TABLE_ENUM_AREA, null, values).toInt()

        enumArea.id?.let {enum_area_id ->
            for (latLon in enumArea.vertices)
            {
                latLon.enumAreaId = enum_area_id
                latLon.id = DAO.latLonDAO.createLatLon(latLon)
            }

            return enum_area_id
        }

        return -1
    }

    //--------------------------------------------------------------------------
    fun putEnumArea( enumArea: EnumArea, values: ContentValues )
    {
        values.put( DAO.COLUMN_CONFIG_ID, enumArea.config_id )
        values.put( DAO.COLUMN_ENUM_AREA_NAME, enumArea.name )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createEnumArea(cursor: Cursor): EnumArea
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val config_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_CONFIG_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_NAME))

        return EnumArea( id, config_id, name )
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
            val enumArea = createEnumArea( cursor )
            enumArea.vertices = DAO.latLonDAO.getLatLons( enumArea.id!! )
            enumAreas.add( enumArea )
        }

        cursor.close()
        db.close()

        return enumAreas
    }
}