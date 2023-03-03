package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
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
        values.put( DAO.COLUMN_UUID, enumArea.uuid )
        values.put( DAO.COLUMN_ENUM_AREA_CONFIG_UUID, enumArea.config_uuid )
        values.put( DAO.COLUMN_ENUM_AREA_NAME, enumArea.name )
        values.put( DAO.COLUMN_ENUM_AREA_SHAPE, enumArea.shape )
        values.put( DAO.COLUMN_ENUM_AREA_SHAPE_UUID, enumArea.shape_uuid )
    }

    //--------------------------------------------------------------------------
    private fun createEnumArea( cursor: Cursor): EnumArea
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val config_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_CONFIG_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_NAME))
        val shape = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_SHAPE))
        val shape_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_SHAPE_UUID))

        return EnumArea( uuid, config_uuid, name, shape, shape_uuid )
    }

    //--------------------------------------------------------------------------
    fun updateEnumArea( enumArea: EnumArea )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(enumArea.uuid)
        val values = ContentValues()

        putEnumArea( enumArea, values )

        db.update(DAO.TABLE_ENUM_AREA, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getEnumArea( uuid: String ): EnumArea?
    {
        var enumArea: EnumArea? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
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
    fun exists( uuid: String ) : Boolean
    {
        return getEnumArea( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun getEnumAreas( config_uuid: String ): List<EnumArea>
    {
        val enumAreas = ArrayList<EnumArea>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_ENUM_AREA_CONFIG_UUID} = '$config_uuid'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumAreas.add( createEnumArea( cursor ))
        }

        cursor.close()
        db.close()

        return enumAreas
    }

    //--------------------------------------------------------------------------
    fun getEnumAreas(): List<EnumArea>
    {
        val enumAreas = ArrayList<EnumArea>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUM_AREA}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumAreas.add( createEnumArea( cursor ))
        }

        cursor.close()
        db.close()

        return enumAreas
    }

    //--------------------------------------------------------------------------
    fun deleteEnumArea( enumArea: EnumArea )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(enumArea.uuid.toString())

        db.delete(DAO.TABLE_ENUM_AREA, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteOrphans()
    {
        val enumAreas = getEnumAreas()

        for (enumArea in enumAreas)
        {
            if (DAO.configDAO.doesNotExist( enumArea.config_uuid ))
            {
                deleteEnumArea( enumArea )
            }
        }
    }
}