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
import androidx.core.database.getLongOrNull
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.constants.OperatorConverter
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldOption
import edu.gtri.gpssample.database.models.Rule
import edu.gtri.gpssample.database.models.Study

class FieldDAO(private var dao: DAO)
{
    fun createOrUpdateField( field: Field, study : Study ) : Field?
    {
        if (exists( field ))
        {
            updateField( field, study )
            Log.d( "xxx", "Updated Field with ID ${field.uuid}")
        }
        else
        {
            val values = ContentValues()
            putField( field, study, values )
            if (dao.writableDatabase.insert(DAO.TABLE_FIELD, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "Created Field with ID ${field.uuid}")
        }

        field.fields?.let { fields ->
            for (field in fields)
            {
                createOrUpdateField( field, study )
            }
        }

        for (fieldOption in field.fieldOptions)
        {
            DAO.fieldOptionDAO.createOrUpdateFieldOption( fieldOption, field )
        }

        return field
    }

    fun putField( field: Field, study : Study, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, field.uuid )
        values.put( DAO.COLUMN_FIELD_PARENT_UUID, field.parentUUID )
        values.put( DAO.COLUMN_CREATION_DATE, field.creationDate )
        values.put( DAO.COLUMN_STUDY_UUID, study.uuid )
        values.put( DAO.COLUMN_FIELD_INDEX, field.index )
        values.put( DAO.COLUMN_FIELD_NAME, field.name )
        values.put( DAO.COLUMN_FIELD_TYPE_INDEX, FieldTypeConverter.toIndex(field.type))
        values.put( DAO.COLUMN_FIELD_PII, field.pii )
        values.put( DAO.COLUMN_FIELD_REQUIRED, field.required )
        values.put( DAO.COLUMN_FIELD_INTEGER_ONLY, field.integerOnly )
        values.put( DAO.COLUMN_FIELD_NUMBER_OF_RESIDENTS, field.numberOfResidents )
        values.put( DAO.COLUMN_FIELD_DATE, field.date )
        values.put( DAO.COLUMN_FIELD_TIME, field.time )
        values.put( DAO.COLUMN_FIELD_MINIMUM, field.minimum )
        values.put( DAO.COLUMN_FIELD_MAXIMUM, field.maximum )

        // TODO: use look up tables
        val type = FieldTypeConverter.toIndex(field.type)
        values.put( DAO.COLUMN_FIELD_TYPE_INDEX, type )
    }

    fun updateField( field: Field, study : Study )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(field.uuid)
        val values = ContentValues()

        putField( field, study, values )

        dao.writableDatabase.update(DAO.TABLE_FIELD, values, whereClause, args )
    }

    fun exists( field: Field ): Boolean
    {
        getField( field.uuid )?.let {
            return true
        } ?: return false
    }

    @SuppressLint("Range")
    private fun  buildField(cursor: Cursor ): Field
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val parentUUID = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_PARENT_UUID))
        val index = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_INDEX))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_NAME))
        val typeIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_TYPE_INDEX))
        val pii = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_PII)).toBoolean()
        val required = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_REQUIRED)).toBoolean()
        val integerOnly = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_INTEGER_ONLY)).toBoolean()
        val numberOfResidents = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_NUMBER_OF_RESIDENTS)).toBoolean()
        val date = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATE)).toBoolean()
        val time = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_TIME)).toBoolean()
        val minimum = cursor.getDoubleOrNull(cursor.getColumnIndex(DAO.COLUMN_FIELD_MINIMUM))
        val maximum = cursor.getDoubleOrNull(cursor.getColumnIndex(DAO.COLUMN_FIELD_MAXIMUM))

        val type = FieldTypeConverter.fromIndex(typeIndex)

        return Field( uuid, creationDate, parentUUID, index, name, type, pii, required, integerOnly, numberOfResidents, date, time, minimum, maximum, ArrayList<FieldOption>(), ArrayList<Field>())
    }

    fun getField( uuid : String ): Field?
    {
        var field: Field? = null
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} where ${DAO.COLUMN_UUID} = '${uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            field = buildField( cursor )
            field.fields = getBlockFields( field.uuid )
            field.fieldOptions = DAO.fieldOptionDAO.getFieldOptions( field )
        }

        cursor.close()

        return field
    }

    fun getBlockFields( parentFieldUUID : String ): ArrayList<Field>?
    {
        val fields = ArrayList<Field>()
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} where ${DAO.COLUMN_FIELD_PARENT_UUID} = '${parentFieldUUID}' ORDER BY ${DAO.COLUMN_FIELD_INDEX} ASC"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val field = buildField( cursor )
            field.fieldOptions = DAO.fieldOptionDAO.getFieldOptions( field )
            fields.add( field )
        }

        cursor.close()

        return if (fields.isEmpty()) null else fields
    }

    fun getFields(study : Study): ArrayList<Field>
    {
        val fields = ArrayList<Field>()
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} where ${DAO.COLUMN_STUDY_UUID} = '${study.uuid}' ORDER BY ${DAO.COLUMN_FIELD_INDEX} ASC"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val field = buildField( cursor )
            if (field.parentUUID == null)
            {
                field.fields = getBlockFields( field.uuid )
                field.fieldOptions = DAO.fieldOptionDAO.getFieldOptions( field )
                val rules = DAO.ruleDAO.getRules(field)
                study.rules.addAll(rules)
                fields.add( field)
            }
        }

        cursor.close()

        return fields
    }

    fun getFields(): List<Field>
    {
        val fields = ArrayList<Field>()
        val query = "SELECT * FROM ${DAO.TABLE_FIELD}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            fields.add( buildField( cursor ))
        }

        cursor.close()

        return fields
    }

    fun deleteField( field: Field )
    {
        field.fields?.let { fields ->
            for (field in fields)
            {
                deleteField( field )
            }
        }

        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(field.uuid)

        dao.writableDatabase.delete(DAO.TABLE_FIELD, whereClause, args)
    }
}