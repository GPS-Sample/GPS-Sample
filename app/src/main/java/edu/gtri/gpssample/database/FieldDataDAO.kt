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
import androidx.core.database.getDoubleOrNull
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CREATION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_ENUMERATION_ITEM_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_DATA_BLOCK_NUMBER
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_DATA_DATE_VALUE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_DATA_DROPDOWN_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_DATA_NUMBER_VALUE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_DATA_TEXT_VALUE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_TYPE_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_VERSION
import edu.gtri.gpssample.database.models.*

class FieldDataDAO(private var dao: DAO)
{
    fun createOrUpdateFieldData( fieldData: FieldData, version: String )
    {
        fieldData.version = version

        val values = ContentValues()
        putFieldData( fieldData, values )

        dao.upsert( DAO.TABLE_FIELD_DATA, values )

        for (fieldDataOption in fieldData.fieldDataOptions)
        {
            DAO.fieldDataOptionDAO.createOrUpdateFieldDataOption( fieldDataOption, fieldData, fieldDataOption.version )
        }
    }

    fun putFieldData(fieldData: FieldData, values: ContentValues)
    {
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
        values.put( DAO.COLUMN_ENUMERATION_ITEM_UUID, fieldData.enumerationItemUuid )
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
        val enumerationItemUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_ITEM_UUID))

        return FieldData( uuid, creationDate, fieldUuid, name, type, textValue, numberValue, dateValue, dropdownIndex, blockNumber, ArrayList<FieldDataOption>(), enumerationItemUuid, version )
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

    companion object
    {
        val columnBindings = listOf(
            ColumnBinding<FieldData>(COLUMN_CREATION_DATE,"INTEGER",FieldData::creationDate ),
            ColumnBinding<FieldData>(COLUMN_VERSION,"TEXT",FieldData::version ),
            ColumnBinding<FieldData>(COLUMN_FIELD_UUID,"TEXT",FieldData::fieldUuid ),
            ColumnBinding<FieldData>(COLUMN_ENUMERATION_ITEM_UUID,"TEXT",FieldData::enumerationItemUuid ),
            ColumnBinding<FieldData>(COLUMN_FIELD_NAME,"TEXT",FieldData::name ),
            ColumnBinding<FieldData>(COLUMN_FIELD_TYPE_INDEX,"INTEGER",{FieldTypeConverter.toIndex(it.type)} ),
            ColumnBinding<FieldData>(COLUMN_FIELD_DATA_TEXT_VALUE,"TEXT",FieldData::textValue ),
            ColumnBinding<FieldData>(COLUMN_FIELD_DATA_NUMBER_VALUE,"REAL",FieldData::numberValue ),
            ColumnBinding<FieldData>(COLUMN_FIELD_DATA_DATE_VALUE,"INTEGER",FieldData::dateValue ),
            ColumnBinding<FieldData>(COLUMN_FIELD_DATA_DROPDOWN_INDEX,"INTEGER",FieldData::dropdownIndex ),
            ColumnBinding<FieldData>(COLUMN_FIELD_DATA_BLOCK_NUMBER,"INTEGER",FieldData::blockNumber ),
        )
    }
}
