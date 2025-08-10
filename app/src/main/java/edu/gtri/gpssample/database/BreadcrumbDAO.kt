package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.Breadcrumb

class BreadcrumbDAO(private var dao: DAO)
{
    fun createOrUpdateBreadcrumb(breadcrumb: Breadcrumb): Breadcrumb?
    {
        val existingBreadcrumb = getBreadcrumb( breadcrumb.uuid )

        if (existingBreadcrumb != null)
        {
            updateBreadcrumb( breadcrumb )
            Log.d( "xxx", "Updated Breadcrumb with ID ${breadcrumb.uuid}" )
        }
        else
        {
            val values = ContentValues()
            putBreadcrumb(breadcrumb, values)
            if (dao.writableDatabase.insert(DAO.TABLE_BREADCRUMB, null, values) < 0) {
                return null
            }

            Log.d("xxx", "Created Breadcrumb with ID ${breadcrumb.uuid}")
        }

        return breadcrumb
    }

    fun updateBreadcrumb(breadcrumb: Breadcrumb)
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(breadcrumb.uuid)
        val values = ContentValues()

        putBreadcrumb( breadcrumb, values )

        dao.writableDatabase.update(DAO.TABLE_BREADCRUMB, values, whereClause, args )
    }

    private fun putBreadcrumb( breadcrumb: Breadcrumb, values: ContentValues)
    {
        values.put( DAO.COLUMN_UUID, breadcrumb.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, breadcrumb.creationDate )
        values.put( DAO.COLUMN_ENUM_AREA_UUID, breadcrumb.enumAreaUuid )
        values.put( DAO.COLUMN_LATITUDE, breadcrumb.latitude )
        values.put( DAO.COLUMN_LONGITUDE, breadcrumb.longitude )
    }

    @SuppressLint("Range")
    private fun buildBreadcrumb(cursor: Cursor): Breadcrumb
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val enumAreaUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_UUID))
        val latitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LATITUDE))
        val longitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LONGITUDE))

        return Breadcrumb( uuid, creationDate, enumAreaUuid, latitude, longitude )
    }

    fun getBreadcrumb( uuid: String ): Breadcrumb?
    {
        var breadcrumb: Breadcrumb? = null

        val query = "SELECT * FROM ${DAO.TABLE_BREADCRUMB} where ${DAO.COLUMN_UUID}='${uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            breadcrumb = buildBreadcrumb( cursor )
        }

        cursor.close()

        return breadcrumb
    }

    fun getBreadcrumbs( enumAreaUuid: String ): ArrayList<Breadcrumb>
    {
        val breadcrumbs = ArrayList<Breadcrumb>()
        val query = "SELECT * FROM ${DAO.TABLE_BREADCRUMB} where ${DAO.COLUMN_ENUM_AREA_UUID}='${enumAreaUuid}' ORDER BY ${DAO.COLUMN_CREATION_DATE}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            breadcrumbs.add( buildBreadcrumb( cursor ))
        }

        cursor.close()

        return breadcrumbs
    }

    fun delete( breadcrumb: Breadcrumb )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(breadcrumb.uuid)

        dao.writableDatabase.delete(DAO.TABLE_BREADCRUMB, whereClause, args)
    }}