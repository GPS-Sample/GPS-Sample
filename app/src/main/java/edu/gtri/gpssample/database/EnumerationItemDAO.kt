package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean

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
            enumerationItem.id = null
            val values = ContentValues()
            putEnumerationItem( enumerationItem, location, values )
            enumerationItem.id = dao.writableDatabase.insert(DAO.TABLE_ENUMERATION_ITEM, null, values).toInt()
            enumerationItem.id?.let { id ->
                Log.d( "xxx", "created EnumerationItem with ID $id" )
            }
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
        else if (enumerationItem.syncCode > existingEnumerationItem.syncCode)
        {
            delete( existingEnumerationItem, false )
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
        val existingEnumerationItem = getEnumerationItem( enumerationItem.uuid )

        if (existingEnumerationItem != null && enumerationItem.syncCode > existingEnumerationItem.syncCode)
        {
            delete( existingEnumerationItem, false )
            createOrUpdateEnumerationItem( enumerationItem, location )
        }
    }

    fun putEnumerationItem( enumerationItem: EnumerationItem, location : Location, values: ContentValues )
    {
        enumerationItem.id?.let { id ->
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, enumerationItem.creationDate )
        values.put( DAO.COLUMN_UUID, enumerationItem.uuid )
        values.put( DAO.COLUMN_SYNC_CODE, enumerationItem.syncCode)
        values.put( DAO.COLUMN_LOCATION_ID, location.id)
        values.put( DAO.COLUMN_ENUMERATION_ITEM_SUB_ADDRESS, enumerationItem.subAddress )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATOR_NAME, enumerationItem.enumeratorName )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE, enumerationItem.enumerationState.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_DATE, enumerationItem.enumerationDate )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_INCOMPLETE_REASON, enumerationItem.enumerationIncompleteReason )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_NOTES, enumerationItem.enumerationNotes )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SAMPLING, enumerationItem.enumerationEligibleForSampling )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_SAMPLING_STATE, enumerationItem.samplingState.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTOR_NAME, enumerationItem.collectorName )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_STATE, enumerationItem.collectionState.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_DATE, enumerationItem.collectionDate )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_INCOMPLETE_REASON, enumerationItem.collectionIncompleteReason )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_NOTES, enumerationItem.collectionNotes )
    }

    @SuppressLint("Range")
    private fun createEnumerationItem(cursor: Cursor): EnumerationItem {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val syncCode = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_SYNC_CODE))
        val locationId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_LOCATION_ID))
        val subAddress = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_SUB_ADDRESS))
        val enumeratorName = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATOR_NAME))
        val enumerationState = EnumerationState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE)))
        val enumerationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_DATE))
        val enumerationIncompleteReason = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_INCOMPLETE_REASON))
        val enumerationNotes = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_NOTES))
        val enumerationEligibleForSampling = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SAMPLING)).toBoolean()
        val samplingState = SamplingState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_SAMPLING_STATE)))
        val collectorName = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTOR_NAME))
        val collectionState = CollectionState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_STATE)))
        val collectionDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_DATE))
        val collectionIncompleteReason = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_INCOMPLETE_REASON))
        val collectionNotes = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_NOTES))

        val fieldDataList = ArrayList<FieldData>()

        return EnumerationItem(
            id,
            creationDate,
            uuid,
            syncCode,
            0.0,
            "",
            subAddress,
            enumeratorName,
            enumerationState,
            enumerationDate,
            enumerationIncompleteReason,
            enumerationNotes,
            enumerationEligibleForSampling,
            samplingState,
            collectorName,
            collectionState,
            collectionDate,
            collectionIncompleteReason,
            collectionNotes,
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

    fun delete( enumerationItem: EnumerationItem, shouldDeleteFieldData: Boolean = true )
    {
        enumerationItem.id?.let { id ->
            Log.d( "xxx", "deleted EnumerationItem with ID $id" )

            val fieldDataList = DAO.fieldDataDAO.getFieldDataList( enumerationItem )

            if (shouldDeleteFieldData)
            {
                for (fieldData in fieldDataList)
                {
                    DAO.fieldDataDAO.delete( fieldData)
                }
            }

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            dao.writableDatabase.delete(DAO.TABLE_ENUMERATION_ITEM, whereClause, args)
        }
    }
}