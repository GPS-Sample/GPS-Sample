package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.extensions.toInt

class EnumerationItemDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateEnumerationItem( enumerationItem: EnumerationItem) : EnumerationItem?
    {
        if (exists( enumerationItem ))
        {
            updateEnumerationItem( enumerationItem )
        }
        else
        {
            val values = ContentValues()

            putEnumerationItem( enumerationItem, values )

            enumerationItem.id = dao.writableDatabase.insert(DAO.TABLE_ENUMERATION_ITEM, null, values).toInt()
            enumerationItem.id?.let { id ->
                Log.d( "xxx", "new location id = ${id}")
//                enumData.fieldDataList?.let { fieldDataList ->
//                    for (fieldData in fieldDataList)
//                    {
//                        DAO.fieldDataDAO.createOrUpdateFieldData( fieldData )
//                    }
//                }
            } ?: return null
        }

        return enumerationItem
    }

    //--------------------------------------------------------------------------
    fun importEnumerationItem( enumerationItem: EnumerationItem ) : EnumerationItem?
    {
        val existingEnumerationItem = getEnumerationItem( enumerationItem.uuid )

        existingEnumerationItem?.let {
            delete( it )
        }

        val values = ContentValues()

        enumerationItem.id = null
        putEnumerationItem( enumerationItem, values )

        enumerationItem.id = dao.writableDatabase.insert(DAO.TABLE_ENUMERATION_ITEM, null, values).toInt()
        enumerationItem.id?.let { id ->
            Log.d( "xxx", "new EnumerationItem id = ${id}")
//            enumData.fieldDataList?.let { fieldDataList ->
//                for (fieldData in fieldDataList)
//                {
//                    fieldData.id = null
//                    fieldData.enumDataId = id
//                    DAO.fieldDataDAO.createOrUpdateFieldData( fieldData )
//                }
//            }
        } ?: return null

        return enumerationItem
    }

    //--------------------------------------------------------------------------
    fun exists( enumerationItem: EnumerationItem ): Boolean
    {
        enumerationItem.id?.let { id ->
            getEnumerationItem( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    fun updateEnumerationItem( enumerationItem: EnumerationItem )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(enumerationItem.id!!.toString())
        val values = ContentValues()

        putEnumerationItem( enumerationItem, values )

        db.update(DAO.TABLE_ENUMERATION_ITEM, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun putEnumerationItem( enumerationItem: EnumerationItem, values: ContentValues )
    {
        enumerationItem.id?.let { id ->
            Log.d( "xxx", "existing EnumerationItem id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, enumerationItem.creationDate )
        values.put( DAO.COLUMN_UUID, enumerationItem.uuid )
        values.put( DAO.COLUMN_LOCATION_ID, enumerationItem.locationId )
        values.put( DAO.COLUMN_COLLECTION_ITEM_ID, enumerationItem.collectionItemId )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_SUB_ADDRESS, enumerationItem.subAddress )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_VALID, enumerationItem.valid )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_INCOMPLETE_REASON, enumerationItem.incompleteReason )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_NOTES, enumerationItem.notes )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createEnumerationItem(cursor: Cursor): EnumerationItem
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val locationId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_ID))
        val collectionItemId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_COLLECTION_ITEM_ID))
        val subAddress = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_SUB_ADDRESS))
        val valid = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_VALID)).toBoolean()
        val incompleteReason = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_INCOMPLETE_REASON))
        val notes = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_NOTES))

        val fieldDataList = ArrayList<FieldData>()

        return EnumerationItem( id, creationDate, uuid, locationId, collectionItemId, subAddress, valid, incompleteReason, notes, fieldDataList )
    }

    fun getEnumerationItem( uuid: String ) : EnumerationItem?
    {
        var enumerationItem : EnumerationItem? = null
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumerationItem = createEnumerationItem( cursor )
        }

        cursor.close()
        db.close()

        return enumerationItem
    }

    fun getEnumerationItems( location: Location ) : ArrayList<EnumerationItem>
    {
        var enumerationItems = ArrayList<EnumerationItem>()
        val db = dao.writableDatabase

        location.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM} WHERE ${DAO.COLUMN_LOCATION_ID} = $id"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val enumerationItem = createEnumerationItem( cursor )
                enumerationItems.add( enumerationItem )
            }

            cursor.close()
        }

        db.close()

        return enumerationItems
    }

//    fun getLocation( enumArea: EnumArea, team: Team ) : ArrayList<Location>
//    {
//        var locations = ArrayList<Location>()
//        val db = dao.writableDatabase
//
//        enumArea.id?.let { enumAreaId ->
//            team.id?.let { teamId ->
//                var query = ""
//
//                if (team.isEnumerationTeam)
//                {
//                    query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND ${DAO.COLUMN_ENUMERATION_TEAM_ID} = $teamId"
//                }
//                else
//                {
//                    query = "SELECT * FROM ${DAO.TABLE_LOCATION} WHERE ${DAO.COLUMN_ENUM_AREA_ID} = $enumAreaId AND ${DAO.COLUMN_COLLECTION_TEAM_ID} = $teamId"
//                }
//
//                val cursor = db.rawQuery(query, null)
//
//                while (cursor.moveToNext())
//                {
//                    val location = createLocation( cursor )
//                    locations.add( location )
//                }
//
//                cursor.close()
//            }
//        }
//
//        db.close()
//
//        return locations
//    }

    fun getEnumerationItems() : ArrayList<EnumerationItem>
    {
        var enumerationItems = ArrayList<EnumerationItem>()
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val enumerationItem = createEnumerationItem( cursor )
            enumerationItems.add( enumerationItem )
        }

        cursor.close()

        db.close()

        return enumerationItems
    }

    fun getEnumerationItem( id: Int ) : EnumerationItem?
    {
        var enumerationItem: EnumerationItem? = null

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumerationItem = createEnumerationItem( cursor )
        }

        cursor.close()
        db.close()

        return enumerationItem
    }

    fun delete( enumerationItem: EnumerationItem )
    {
        enumerationItem.id?.let { id ->
            Log.d( "xxx", "deleting EnumerationItem with ID $id" )

            val collectionItem = DAO.collectionItemDAO.getCollectionItem( enumerationItem.collectionItemId )

            collectionItem?.let {
                DAO.collectionItemDAO.delete( it )
            }

            val fieldDataList = DAO.fieldDataDAO.getFieldDataList( enumerationItem )

            for (fieldData in fieldDataList)
            {
                DAO.fieldDataDAO.delete( fieldData)
            }

            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            db.delete(DAO.TABLE_ENUMERATION_ITEM, whereClause, args)
            db.close()
        }
    }
}