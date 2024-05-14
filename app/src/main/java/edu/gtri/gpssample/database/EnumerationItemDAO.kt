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
import java.util.*
import kotlin.collections.ArrayList

class EnumerationItemDAO(private var dao: DAO)
{
    fun createOrUpdateEnumerationItem( enumerationItem: EnumerationItem, location : Location ) : EnumerationItem?
    {
        if (exists( enumerationItem ))
        {
            updateEnumerationItem( enumerationItem, location )
            Log.d( "xxx", "Updated EnumerationItem with ID $enumerationItem.uuid" )
        }
        else
        {
            if (enumerationItem.uuid.isEmpty())
            {
                enumerationItem.uuid = UUID.randomUUID().toString()
            }
            val values = ContentValues()
            putEnumerationItem( enumerationItem, location, values )
            if (dao.writableDatabase.insert(DAO.TABLE_ENUMERATION_ITEM, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "Created EnumerationItem with ID $enumerationItem.uuid" )
        }

        enumerationItem.fieldDataList?.let { fieldDataList ->
            for (fieldData in fieldDataList)
            {
                DAO.fieldDataDAO.createOrUpdateFieldData( fieldData, enumerationItem )
            }
        }

        return enumerationItem
    }

    fun exists( enumerationItem: EnumerationItem ): Boolean
    {
        getEnumerationItem( enumerationItem.uuid )?.let {
            return true
        } ?: return false
    }

    fun updateEnumerationItem( enumerationItem: EnumerationItem, location : Location )
    {
        val existingEnumerationItem = getEnumerationItem( enumerationItem.uuid )

        if (existingEnumerationItem != null && enumerationItem.syncCode > existingEnumerationItem.syncCode)
        {
            val whereClause = "${DAO.COLUMN_UUID} = ?"
            val args: Array<String> = arrayOf(enumerationItem.uuid)
            val values = ContentValues()
            putEnumerationItem( enumerationItem, location, values )
            dao.writableDatabase.update(DAO.TABLE_ENUMERATION_ITEM, values, whereClause, args )
        }
    }

    fun putEnumerationItem( enumerationItem: EnumerationItem, location : Location, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, enumerationItem.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, enumerationItem.creationDate )
        values.put( DAO.COLUMN_SYNC_CODE, enumerationItem.syncCode)
        values.put( DAO.COLUMN_LOCATION_UUID, location.uuid)
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
    private fun buildEnumerationItem(cursor: Cursor): EnumerationItem {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val syncCode = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_SYNC_CODE))
        val locationUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_LOCATION_UUID))
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
            uuid,
            creationDate,
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
            locationUuid
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
            enumerationItem = buildEnumerationItem( cursor )
        }

        cursor.close()

        return enumerationItem
    }

    fun getEnumerationItems( location: Location ) : ArrayList<EnumerationItem>
    {
        val enumerationItems = ArrayList<EnumerationItem>()

        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM} WHERE ${DAO.COLUMN_LOCATION_UUID} = '${location.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val enumerationItem = buildEnumerationItem( cursor )
            enumerationItem.fieldDataList = DAO.fieldDataDAO.getFieldDataList( enumerationItem )
            enumerationItems.add( enumerationItem )
        }

        cursor.close()

        return enumerationItems
    }

    fun getEnumerationItems() : ArrayList<EnumerationItem>
    {
        val enumerationItems = ArrayList<EnumerationItem>()

        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val enumerationItem = buildEnumerationItem( cursor )
            enumerationItems.add( enumerationItem )
        }

        cursor.close()

        return enumerationItems
    }

    fun delete( enumerationItem: EnumerationItem, shouldDeleteFieldData: Boolean = true )
    {
        val fieldDataList = DAO.fieldDataDAO.getFieldDataList( enumerationItem )

        if (shouldDeleteFieldData)
        {
            for (fieldData in fieldDataList)
            {
                DAO.fieldDataDAO.delete( fieldData)
            }
        }

        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(enumerationItem.uuid)

        dao.writableDatabase.delete(DAO.TABLE_ENUMERATION_ITEM, whereClause, args)
    }
}