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
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldOption

class FieldOptionDAO(private var dao: DAO)
{
    fun createOrUpdateFieldOption( fieldOption: FieldOption, field: Field, version: String )
    {
        fieldOption.version = version

        val values = ContentValues()
        putFieldOption( fieldOption, values )

        dao.upsert( DAO.TABLE_FIELD_OPTION, values )

        createConnection( fieldOption, field )
    }

    private fun createConnection( fieldOption: FieldOption, field: Field )
    {
        val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_FIELD__FIELD_OPTION} WHERE ${DAO.COLUMN_FIELD_UUID} = '${field.uuid}' AND ${DAO.COLUMN_FIELD_OPTION_UUID} = '${fieldOption.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)
        if (cursor.count == 0)
        {
            val values = ContentValues()
            values.put( DAO.COLUMN_FIELD_UUID, field.uuid )
            values.put( DAO.COLUMN_FIELD_OPTION_UUID, fieldOption.uuid )
            dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_FIELD__FIELD_OPTION, null, values).toInt()
        }
        cursor.close()
    }

    fun putFieldOption( fieldOption: FieldOption, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, fieldOption.uuid )
        values.put( DAO.COLUMN_VERSION, fieldOption.version )
        values.put( DAO.COLUMN_FIELD_OPTION_NAME, fieldOption.name )
    }

    @SuppressLint("Range")
    private fun  buildFieldOption(cursor: Cursor ): FieldOption
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_NAME))

        return FieldOption(uuid, name, version)
    }

    fun getFieldOption( uuid : String ): FieldOption?
    {
        var fieldOption: FieldOption? = null
        val query = "SELECT * FROM ${DAO.TABLE_FIELD_OPTION} where ${DAO.COLUMN_UUID} = '${uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            fieldOption = buildFieldOption( cursor )
        }

        cursor.close()

        return fieldOption
    }

    @SuppressLint("Range")
    fun getFieldOptions( field: Field ) : ArrayList<FieldOption>
    {
        val fieldOptions = ArrayList<FieldOption>()

        val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_FIELD__FIELD_OPTION} where ${DAO.COLUMN_FIELD_UUID} = '${field.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val fieldOptionId = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_UUID))
            val fieldOption = getFieldOption( fieldOptionId )
            fieldOption?.let {
                fieldOptions.add( it )
            }
        }

        cursor.close()

        return fieldOptions
    }

    fun deleteFieldOption( fieldOption: FieldOption )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(fieldOption.uuid)

        dao.writableDatabase.delete(DAO.TABLE_FIELD_OPTION, whereClause, args)
    }
}