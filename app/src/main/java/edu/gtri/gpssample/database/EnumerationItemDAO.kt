/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.ReviewStatus
import edu.gtri.gpssample.constants.SamplingState
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CREATION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_COLLECTION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_COLLECTION_INCOMPLETE_REASON
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_COLLECTION_NOTES
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_COLLECTION_STATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_COLLECTOR_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_ENUMERATION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SAMPLING
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SUBSET_SAMPLING
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_ENUMERATION_INCOMPLETE_REASON
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_ENUMERATION_NOTES
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_ENUMERATOR_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_EXCLUSION_NOTES
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_EXCLUSION_REASON
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_ODK_RECORD_URI
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_REVIEW_STATUS
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_SAMPLING_STATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_SUBSET_SAMPLING_STATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_SUB_ADDRESS
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_LOCATION_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_VERSION
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean
import java.util.*
import kotlin.collections.ArrayList

class EnumerationItemDAO(private var dao: DAO)
{
    fun createOrUpdateEnumerationItem( enumerationItem: EnumerationItem, version: String )
    {
        enumerationItem.version = version

        val values = ContentValues()
        putEnumerationItem( enumerationItem, enumerationItem.locationUuid, values )

        dao.upsert( DAO.TABLE_ENUMERATION_ITEM, values )

        enumerationItem.fieldDataList.let { fieldDataList ->
            for (fieldData in fieldDataList)
            {
                fieldData.enumerationItemUuid = enumerationItem.uuid
                DAO.fieldDataDAO.createOrUpdateFieldData( fieldData,fieldData.version )
            }
        }
    }

    fun createOrUpdateEnumerationItems( enumerationItems: List<EnumerationItem> )
    {
        val start = Date().time / 1000L

        DAO.instance().writableDatabase.beginTransaction()

        for (enumerationItem in enumerationItems)
        {
            createOrUpdateEnumerationItem( enumerationItem, enumerationItem.version )
        }

        DAO.instance().writableDatabase.setTransactionSuccessful()
        DAO.instance().writableDatabase.endTransaction()

        val duration= Date().time / 1000L - start
        val minutes = duration / 60
        val seconds = duration % 60

        Log.d("xxx", "EnumerationItem update time: %d:%02d".format(minutes, seconds))
    }

    fun putEnumerationItem( enumerationItem: EnumerationItem, locationUuid : String, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, enumerationItem.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, enumerationItem.creationDate )
        values.put( DAO.COLUMN_VERSION, enumerationItem.version )
        values.put( DAO.COLUMN_LOCATION_UUID, locationUuid)
        values.put( DAO.COLUMN_ENUMERATION_ITEM_SUB_ADDRESS, enumerationItem.subAddress )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATOR_NAME, enumerationItem.enumeratorName )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE, enumerationItem.enumerationState.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_DATE, enumerationItem.enumerationDate )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_INCOMPLETE_REASON, enumerationItem.enumerationIncompleteReason )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_NOTES, enumerationItem.enumerationNotes )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SAMPLING, enumerationItem.enumerationEligibleForSampling )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SUBSET_SAMPLING, enumerationItem.enumerationEligibleForSubsetSampling )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_SAMPLING_STATE, enumerationItem.samplingState.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_SUBSET_SAMPLING_STATE, enumerationItem.subsetSamplingState.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTOR_NAME, enumerationItem.collectorName )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_STATE, enumerationItem.collectionState.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_DATE, enumerationItem.collectionDate )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_INCOMPLETE_REASON, enumerationItem.collectionIncompleteReason )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_NOTES, enumerationItem.collectionNotes )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_REVIEW_STATUS, enumerationItem.reviewStatus.format )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_EXCLUSION_REASON, enumerationItem.exclusionReason )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_EXCLUSION_NOTES, enumerationItem.exclusionNotes )
        values.put( DAO.COLUMN_ENUMERATION_ITEM_ODK_RECORD_URI, enumerationItem.odkRecordUri )
    }

    @SuppressLint("Range")
    fun buildEnumerationItem(cursor: Cursor): EnumerationItem {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val locationUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_LOCATION_UUID))
        val subAddress = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_SUB_ADDRESS))
        val enumeratorName = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATOR_NAME))
        val enumerationState = EnumerationState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE)))
        val enumerationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_DATE))
        val enumerationIncompleteReason = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_INCOMPLETE_REASON))
        val enumerationNotes = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_NOTES))
        val enumerationEligibleForSampling = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SAMPLING)).toBoolean()
        val enumerationEligibleForSubsetSampling = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SUBSET_SAMPLING)).toBoolean()
        val samplingState = SamplingState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_SAMPLING_STATE)))
        val subsetSamplingState = SamplingState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_SUBSET_SAMPLING_STATE)))
        val collectorName = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTOR_NAME))
        val collectionState = CollectionState.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_STATE)))
        val collectionDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_DATE))
        val collectionIncompleteReason = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_INCOMPLETE_REASON))
        val collectionNotes = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_COLLECTION_NOTES))
        val reviewStatus = ReviewStatus.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_REVIEW_STATUS)))
        val exclusionReason = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_EXCLUSION_REASON))
        val exclusionNotes = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_EXCLUSION_NOTES))
        val odkInstanceUri = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_ODK_RECORD_URI))

        val fieldDataList = ArrayList<FieldData>()

        return EnumerationItem(
            uuid,
            creationDate,
            0.0,
            "",
            true,
            subAddress,
            enumeratorName,
            enumerationState,
            enumerationDate,
            enumerationIncompleteReason,
            enumerationNotes,
            enumerationEligibleForSampling,
            enumerationEligibleForSubsetSampling,
            samplingState,
            subsetSamplingState,
            collectorName,
            collectionState,
            collectionDate,
            collectionIncompleteReason,
            collectionNotes,
            reviewStatus,
            exclusionReason,
            exclusionNotes,
            fieldDataList,
            locationUuid,
            odkInstanceUri,
            version
        )
    }

    fun getEnumerationItem( uuid: String ): EnumerationItem?
    {
        var enumerationItem: EnumerationItem? = null

        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM} WHERE ${DAO.COLUMN_UUID} = '$uuid'"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumerationItem = buildEnumerationItem( cursor )
            enumerationItem.fieldDataList = DAO.fieldDataDAO.getFieldDataList( enumerationItem )
        }

        cursor.close()

        return enumerationItem
    }

    fun getEnumerationItems( location: Location ) : ArrayList<EnumerationItem>
    {
        val enumerationItems = ArrayList<EnumerationItem>()

        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_ITEM} WHERE ${DAO.COLUMN_LOCATION_UUID} = '${location.uuid}' ORDER BY ${DAO.COLUMN_CREATION_DATE} ASC"
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

    companion object
    {
        val columnBindings = listOf(
            ColumnBinding<EnumerationItem>(COLUMN_CREATION_DATE,"INTEGER",EnumerationItem::creationDate ),
            ColumnBinding<EnumerationItem>(COLUMN_VERSION,"TEXT",EnumerationItem::version ),
            ColumnBinding<EnumerationItem>(COLUMN_LOCATION_UUID,"TEXT",EnumerationItem::locationUuid ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_SUB_ADDRESS,"TEXT",EnumerationItem::subAddress ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_ENUMERATOR_NAME,"TEXT",EnumerationItem::enumeratorName ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_ENUMERATION_STATE,"TEXT",EnumerationItem::enumerationState ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_ENUMERATION_DATE,"INTEGER",EnumerationItem::enumerationDate ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_ENUMERATION_INCOMPLETE_REASON,"TEXT",EnumerationItem::enumerationIncompleteReason ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_ENUMERATION_NOTES,"TEXT",EnumerationItem::enumerationNotes ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SAMPLING,"TEXT",EnumerationItem::enumerationEligibleForSampling ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_ENUMERATION_ELIGIBLE_FOR_SUBSET_SAMPLING,"TEXT",EnumerationItem::enumerationEligibleForSubsetSampling ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_SAMPLING_STATE,"TEXT",EnumerationItem::samplingState ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_SUBSET_SAMPLING_STATE,"TEXT",EnumerationItem::subsetSamplingState ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_COLLECTOR_NAME,"TEXT",EnumerationItem::collectorName ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_COLLECTION_STATE,"TEXT",EnumerationItem::collectionState ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_COLLECTION_DATE,"INTEGER",EnumerationItem::collectionDate ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_COLLECTION_INCOMPLETE_REASON,"TEXT",EnumerationItem::collectionIncompleteReason ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_COLLECTION_NOTES,"TEXT",EnumerationItem::collectionNotes ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_REVIEW_STATUS,"TEXT",EnumerationItem::reviewStatus ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_EXCLUSION_REASON,"TEXT",EnumerationItem::exclusionReason ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_EXCLUSION_NOTES,"TEXT",EnumerationItem::exclusionNotes ),
            ColumnBinding<EnumerationItem>(COLUMN_ENUMERATION_ITEM_ODK_RECORD_URI,"TEXT",EnumerationItem::odkRecordUri ),
        )
    }
}