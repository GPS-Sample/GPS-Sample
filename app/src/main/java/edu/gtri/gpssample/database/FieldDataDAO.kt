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
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean
import java.util.HashMap

class FieldDataDAO(private var dao: DAO)
{
    fun createOrUpdateFieldData( fieldData: FieldData, enumerationItem: EnumerationItem, version: String )
    {
        fieldData.version = version

        val values = ContentValues()
        putFieldData( fieldData, values, enumerationItem )

        dao.upsert( DAO.TABLE_FIELD_DATA, values )

        for (fieldDataOption in fieldData.fieldDataOptions)
        {
            DAO.fieldDataOptionDAO.createOrUpdateFieldDataOption( fieldDataOption, fieldData, fieldDataOption.version )
        }
    }

    fun putFieldData(fieldData: FieldData, values: ContentValues, enumerationItem: EnumerationItem?)
    {
        enumerationItem?.let {
            values.put( DAO.COLUMN_ENUMERATION_ITEM_UUID, it.uuid )
        }

        values.put( DAO.COLUMN_UUID, fieldData.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, fieldData.creationDate )
        values.put( DAO.COLUMN_VERSION, fieldData.version )
        values.put( DAO.COLUMN_FIELD_UUID, fieldData.fieldUuid )
        values.put( DAO.COLUMN_FIELD_NAME, fieldData.name )
        values.put( DAO.COLUMN_FIELD_TYPE_INDEX, FieldTypeConverter.toIndex(fieldData.type))
        values.put( DAO.COLUMN_FIELD_DATA_TEXT_VALUE, fieldData.textValue )
        values.put( DAO.COLUMN_FIELD_DATA_NUMBER_VALUE, fieldData.numberValue )
        values.put( DAO.COLUMN_FIELD_DATA_DATE_VALUE, fieldData.dateValue )
        values.put( DAO.COLUMN_FIELD_DATA_DROPDOWN_INDEX, fieldData.dropdownIndex )
        values.put( DAO.COLUMN_FIELD_DATA_BLOCK_NUMBER, fieldData.blockNumber )
    }

    @SuppressLint("Range")
    private fun buildFieldData(cursor: Cursor): FieldData
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val fieldUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_NAME))
        val type = FieldTypeConverter.fromIndex(cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_TYPE_INDEX)))
        val textValue = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_TEXT_VALUE))
        val numberValue = cursor.getDoubleOrNull(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_NUMBER_VALUE))
        val dateValue = cursor.getLongOrNull(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_DATE_VALUE))
        val dropdownIndex = cursor.getIntOrNull(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_DROPDOWN_INDEX))
        val blockNumber = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATA_BLOCK_NUMBER))

        return FieldData( uuid, creationDate, fieldUuid, name, type, textValue, numberValue, dateValue, dropdownIndex, blockNumber, ArrayList<FieldDataOption>(), version )
    }

    fun getFieldDataList( enumerationItem: EnumerationItem ): ArrayList<FieldData>
    {
        val fieldDataList = ArrayList<FieldData>()

        val query = "SELECT * FROM ${DAO.TABLE_FIELD_DATA} WHERE ${DAO.COLUMN_ENUMERATION_ITEM_UUID} = '${enumerationItem.uuid}' ORDER BY ${DAO.COLUMN_CREATION_DATE} ASC"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val fieldData = buildFieldData( cursor )
            fieldData.fieldDataOptions = DAO.fieldDataOptionDAO.getFieldDataOptions( fieldData )
            fieldDataList.add( fieldData )
        }

        cursor.close()

        return fieldDataList
    }

    fun delete( fieldData: FieldData )
    {
        for (fieldDataOption in fieldData.fieldDataOptions)
        {
            DAO.fieldDataOptionDAO.delete(fieldDataOption)
        }

        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(fieldData.uuid)

        dao.writableDatabase.delete(DAO.TABLE_FIELD_DATA, whereClause, args)
    }
}
