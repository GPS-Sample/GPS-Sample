package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.extensions.toInt

class CollectionItemDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateCollectionItem( collectionItem: CollectionItem) : CollectionItem?
    {
        if (exists( collectionItem ))
        {
            updateCollectionItem( collectionItem )
        }
        else
        {
            val values = ContentValues()

            putCollectionItem( collectionItem, values )

            collectionItem.id = dao.writableDatabase.insert(DAO.TABLE_COLLECTION_ITEM, null, values).toInt()
            collectionItem.id?.let { id ->
                Log.d( "xxx", "new CollectionItem id = ${id}")
            } ?: return null
        }

        return collectionItem
    }

    //--------------------------------------------------------------------------
    fun importCollectionItem( collectionItem: CollectionItem ) : CollectionItem?
    {
        val existingCollectionItem = getCollectionItem( collectionItem.uuid )

        existingCollectionItem?.let {
            delete( it )
        }

        val values = ContentValues()

        collectionItem.id = null
        putCollectionItem( collectionItem, values )

        collectionItem.id = dao.writableDatabase.insert(DAO.TABLE_COLLECTION_ITEM, null, values).toInt()
        collectionItem.id?.let { id ->
            Log.d( "xxx", "new CollectionItem id = ${id}")
        } ?: return null

        return collectionItem
    }

    //--------------------------------------------------------------------------
    fun exists( collectionItem: CollectionItem ): Boolean
    {
        collectionItem.id?.let { id ->
            getCollectionItem( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    fun updateCollectionItem( collectionItem: CollectionItem )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(collectionItem.id!!.toString())
        val values = ContentValues()

        putCollectionItem( collectionItem, values )

        db.update(DAO.TABLE_COLLECTION_ITEM, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun putCollectionItem( collectionItem: CollectionItem, values: ContentValues )
    {
        collectionItem.id?.let { id ->
            Log.d( "xxx", "existing CollectionItem id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, collectionItem.creationDate )
        values.put( DAO.COLUMN_UUID, collectionItem.uuid )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ID, collectionItem.enumerationItemId )
        values.put( DAO.COLUMN_COLLECTION_ITEM_VALID, collectionItem.valid )
        values.put( DAO.COLUMN_COLLECTION_ITEM_INCOMPLETE_REASON, collectionItem.incompleteReason )
        values.put( DAO.COLUMN_COLLECTION_ITEM_NOTES, collectionItem.notes )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createCollectionItem(cursor: Cursor): CollectionItem
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val enumerationItemId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ID))
        val valid = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_VALID)).toBoolean()
        val incompleteReason = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_INCOMPLETE_REASON))
        val notes = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_NOTES))

        return CollectionItem( id, creationDate, uuid, enumerationItemId, valid, incompleteReason, notes )
    }

    fun getCollectionItem( uuid: String ) : CollectionItem?
    {
        var collectionItem : CollectionItem? = null
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_ITEM} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            collectionItem = createCollectionItem( cursor )
        }

        cursor.close()
        db.close()

        return collectionItem
    }

    fun getCollectionItems( location: Location ) : ArrayList<CollectionItem>
    {
        var collectionItems = ArrayList<CollectionItem>()
        val db = dao.writableDatabase

        location.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM} WHERE ${DAO.COLUMN_LOCATION_ID} = $id"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val collectionItem = createCollectionItem( cursor )
                collectionItems.add( collectionItem )
            }

            cursor.close()
        }

        db.close()

        return collectionItems
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

    fun getCollectionItems() : ArrayList<CollectionItem>
    {
        var collectionItems = ArrayList<CollectionItem>()
        val db = dao.writableDatabase

        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_ITEM}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val collectionItem = createCollectionItem( cursor )
            collectionItems.add( collectionItem )
        }

        cursor.close()

        db.close()

        return collectionItems
    }

    fun getCollectionItem( id: Int ) : CollectionItem?
    {
        var collectionItem: CollectionItem? = null

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_ITEM} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            collectionItem = createCollectionItem( cursor )
        }

        cursor.close()
        db.close()

        return collectionItem
    }

    fun delete( collectionItem: CollectionItem )
    {
        collectionItem.id?.let { id ->
            Log.d( "xxx", "deleting CollectionItem with ID $id" )

            val db = dao.writableDatabase
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            db.delete(DAO.TABLE_COLLECTION_ITEM, whereClause, args)
            db.close()
        }
    }
}
