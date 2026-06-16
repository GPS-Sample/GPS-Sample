package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CREATION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_TEAM_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUM_AREA_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_GROUP_ID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LATITUDE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LONGITUDE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_VERSION
import edu.gtri.gpssample.database.models.Breadcrumb
import edu.gtri.gpssample.database.models.Field

class BreadcrumbDAO(private var dao: DAO)
{
    fun createOrUpdateBreadcrumb( breadcrumb: Breadcrumb, version: String )
    {
        breadcrumb.version = version

        val values = ContentValues()
        putBreadcrumb( breadcrumb, values )

        dao.upsert( DAO.TABLE_BREADCRUMB, values )
    }

    private fun putBreadcrumb( breadcrumb: Breadcrumb, values: ContentValues)
    {
        values.put( DAO.COLUMN_UUID, breadcrumb.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, breadcrumb.creationDate )
        values.put( DAO.COLUMN_VERSION, breadcrumb.version )
        values.put( DAO.COLUMN_ENUM_AREA_UUID, breadcrumb.enumAreaUuid )
        values.put( DAO.COLUMN_ENUMERATION_TEAM_NAME, breadcrumb.enumTeamName )
        values.put( DAO.COLUMN_LATITUDE, breadcrumb.latitude )
        values.put( DAO.COLUMN_LONGITUDE, breadcrumb.longitude )
        values.put( DAO.COLUMN_GROUP_ID, breadcrumb.groupId )
    }

    @SuppressLint("Range")
    private fun buildBreadcrumb(cursor: Cursor): Breadcrumb
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val enumAreaUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_UUID))
        val enumTeamName = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_TEAM_NAME))
        val latitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LATITUDE))
        val longitude = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LONGITUDE))
        val groupId = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_GROUP_ID))

        return Breadcrumb( uuid, creationDate, enumAreaUuid, enumTeamName, latitude, longitude, groupId, version )
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
    }

    companion object
    {
        val columnBindings = listOf(
            ColumnBinding<Breadcrumb>(COLUMN_CREATION_DATE,"INTEGER",Breadcrumb::creationDate ),
            ColumnBinding<Breadcrumb>(COLUMN_VERSION,"TEXT",Breadcrumb::version ),
            ColumnBinding<Breadcrumb>(COLUMN_ENUM_AREA_UUID,"TEXT",Breadcrumb::version ),
            ColumnBinding<Breadcrumb>(COLUMN_ENUMERATION_TEAM_NAME,"TEXT",Breadcrumb::version ),
            ColumnBinding<Breadcrumb>(COLUMN_LATITUDE,"REAL",Breadcrumb::version ),
            ColumnBinding<Breadcrumb>(COLUMN_LONGITUDE,"REAL",Breadcrumb::version ),
            ColumnBinding<Breadcrumb>(COLUMN_GROUP_ID,"TEXT",Breadcrumb::version ),
        )
    }
}