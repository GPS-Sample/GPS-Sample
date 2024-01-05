package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.models.*

class EnumerationItemDAO(private var dao: DAO)
{
    fun createOrUpdateEnumerationItem( enumerationItem: EnumerationItem, location : Location ) : EnumerationItem?
    {
        if (exists( enumerationItem ))
        {
            updateEnumerationItem( enumerationItem, location)
        }
        else
        {
            val values = ContentValues()
            putEnumerationItem( enumerationItem, location, values )
            enumerationItem.id = dao.writableDatabase.insert(DAO.TABLE_ENUMERATION_ITEM, null, values).toInt()
        }

        enumerationItem.id?.let { id ->
            enumerationItem.fieldDataList?.let { fieldDataList ->
                for (fieldData in fieldDataList)
                {
                    DAO.fieldDataDAO.createOrUpdateFieldData( fieldData, enumerationItem )
                }
            }
        } ?: return null

        return enumerationItem
    }

    fun importEnumerationItem( enumerationItem: EnumerationItem, location : Location, enumArea: EnumArea )
    {
        val existingEnumerationItem = getEnumerationItem( enumerationItem.uuid )

        if (existingEnumerationItem == null)
        {
            enumerationItem.id = null // force the new item be created
            createOrUpdateEnumerationItem( enumerationItem, location )
        }
        else if (enumerationItem.creationDate > existingEnumerationItem.creationDate)
        {
            delete( existingEnumerationItem )
            enumerationItem.id = null // force the new item be created
            createOrUpdateEnumerationItem( enumerationItem, location )
        }
    }

    fun exists( enumerationItem: EnumerationItem ): Boolean
    {
        val enumerationItems = getEnumerationItems()

        for (existingEnumerationItem in enumerationItems)
        {
            if (enumerationItem.uuid == existingEnumerationItem.uuid)
            {
                return true
            }
        }

        return false
    }

    fun updateEnumerationItem( enumerationItem: EnumerationItem, location : Location )
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(enumerationItem.id!!.toString())
        val values = ContentValues()

        putEnumerationItem( enumerationItem, location, values )

        dao.writableDatabase.update(DAO.TABLE_ENUMERATION_ITEM, values, whereClause, args )
    }

    fun putEnumerationItem( enumerationItem: EnumerationItem, location : Location, values: ContentValues )
    {
        enumerationItem.id?.let { id ->
            Log.d( "xxx", "existing EnumerationItem id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, enumerationItem.creationDate )
        values.put( DAO.COLUMN_UUID, enumerationItem.uuid )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_SUB_ADDRESS, enumerationItem.subAddress )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE, enumerationItem.enumerationState.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_SAMPLING_STATE, enumerationItem.samplingState.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_STATE, enumerationItem.collectionState.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_INCOMPLETE_REASON, enumerationItem.incompleteReason )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_NOTES, enumerationItem.notes )
        values.put( DAO.COLUMN_LOCATION_ID, location.id)
    }

    @SuppressLint("Range")
    private fun createEnumerationItem(cursor: Cursor): EnumerationItem {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val subAddress = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_SUB_ADDRESS))
        val enumerationState = EnumerationState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE)))
        val samplingState = SamplingState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_SAMPLING_STATE)))
        val collectionState = CollectionState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_STATE)))
        val incompleteReason = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_INCOMPLETE_REASON))
        val notes = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_NOTES))
        val locationId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_ID))

        val fieldDataList = ArrayList<FieldData>()

        return EnumerationItem(
            id,
            creationDate,
            uuid,
            subAddress,
            enumerationState,
            samplingState,
            collectionState,
            incompleteReason,
            notes,
            fieldDataList,
            locationId
        )
    }

    private fun getEnumerationItem( uuid: String ) : EnumerationItem?
    {
        var enumerationItem : EnumerationItem? = null

        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumerationItem = createEnumerationItem( cursor )
        }

        cursor.close()

        return enumerationItem
    }

    fun getEnumerationItems( location: Location ) : ArrayList<EnumerationItem>
    {
        val enumerationItems = ArrayList<EnumerationItem>()

        location.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM} WHERE ${DAO.COLUMN_LOCATION_ID} = $id"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val enumerationItem = createEnumerationItem( cursor )
                enumerationItem.fieldDataList = DAO.fieldDataDAO.getFieldDataList( enumerationItem )
                enumerationItems.add( enumerationItem )
            }

            cursor.close()
        }

        return enumerationItems
    }

    fun getEnumerationItems() : ArrayList<EnumerationItem>
    {
        val enumerationItems = ArrayList<EnumerationItem>()

        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val enumerationItem = createEnumerationItem( cursor )
            enumerationItems.add( enumerationItem )
        }

        cursor.close()

        return enumerationItems
    }

    fun getEnumerationItem( id: Int ) : EnumerationItem?
    {
        var enumerationItem: EnumerationItem? = null

        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumerationItem = createEnumerationItem( cursor )
        }

        cursor.close()

        return enumerationItem
    }

    fun delete( enumerationItem: EnumerationItem )
    {
        enumerationItem.id?.let { id ->
            Log.d( "xxx", "deleting EnumerationItem with ID $id" )

            val fieldDataList = DAO.fieldDataDAO.getFieldDataList( enumerationItem )

            for (fieldData in fieldDataList)
            {
                DAO.fieldDataDAO.delete( fieldData)
            }

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            dao.writableDatabase.delete(DAO.TABLE_ENUMERATION_ITEM, whereClause, args)
        }
    }
}