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
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CREATION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_INTEGER_ONLY
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_MAXIMUM
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_MINIMUM
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_NUMBER_OF_RESIDENTS
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_PARENT_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_PII
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_REQUIRED
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_TIME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_FIELD_TYPE_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_VERSION
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldOption
import edu.gtri.gpssample.database.models.Study

class FieldDAO(private var dao: DAO)
{
    fun createOrUpdateField( field: Field, version: String )
    {
        field.version = version

        val values = ContentValues()
        putField( field, values )

        dao.upsert( DAO.TABLE_FIELD, values )

        field.fields?.let { fields ->
            for (blockField in fields)
            {
                blockField.studyUuid = field.studyUuid
                createOrUpdateField( blockField,blockField.version )
            }
        }

        for (fieldOption in field.fieldOptions)
        {
            DAO.fieldOptionDAO.createOrUpdateFieldOption( fieldOption, field, fieldOption.version )
        }
    }

    fun putField( field: Field, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, field.uuid )
        values.put( DAO.COLUMN_FIELD_PARENT_UUID, field.parentUUID )
        values.put( DAO.COLUMN_CREATION_DATE, field.creationDate )
        values.put( DAO.COLUMN_VERSION, field.version )
        values.put( DAO.COLUMN_STUDY_UUID, field.studyUuid )
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

    @SuppressLint("Range")
    private fun  buildField(cursor: Cursor ): Field
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
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
        val studyUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_UUID))

        val type = FieldTypeConverter.fromIndex(typeIndex)

        return Field( uuid, creationDate, parentUUID, index, name, type, pii, required, integerOnly, numberOfResidents, date, time, minimum, maximum, ArrayList<FieldOption>(), null, studyUuid, version )
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

        study.subsetRules.clear()
        study.rules.clear()

        while (cursor.moveToNext())
        {
            val field = buildField( cursor )
            if (field.parentUUID == null)
            {
                // search blockFields for rules so that the study
                // will have a complete set of rules
                getBlockFields( field.uuid )?.let { blockFields ->
                    field.fields = blockFields

                    for (blockField in blockFields)
                    {
                        val primaryRules = DAO.ruleDAO.getPrimaryRules( blockField )
                        study.rules.addAll( primaryRules )

                        val subsetRules = DAO.ruleDAO.getSubsetRules( blockField )
                        study.subsetRules.addAll( subsetRules )
                    }
                }

                field.fieldOptions = DAO.fieldOptionDAO.getFieldOptions( field )

                val primaryRules = DAO.ruleDAO.getPrimaryRules( field )
                study.rules.addAll(primaryRules )

                val subsetRules = DAO.ruleDAO.getSubsetRules( field )
                study.subsetRules.addAll(subsetRules )

                fields.add( field)
            }
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

    companion object
    {
        val columnBindings = listOf(
            ColumnBinding<Field>(COLUMN_CREATION_DATE, "INTEGER",Field::creationDate),
            ColumnBinding<Field>(COLUMN_VERSION,"TEXT",Field::version ),
            ColumnBinding<Field>(COLUMN_STUDY_UUID,"TEXT",Field::studyUuid ),
            ColumnBinding<Field>(COLUMN_FIELD_PARENT_UUID,"TEXT",Field::parentUUID ),
            ColumnBinding<Field>(COLUMN_FIELD_INDEX,"INTEGER",Field::index ),
            ColumnBinding<Field>(COLUMN_FIELD_NAME,"TEXT",Field::name ),
            ColumnBinding<Field>(COLUMN_FIELD_TYPE_INDEX,"INTEGER",{FieldTypeConverter.toIndex(it.type)} ),
            ColumnBinding<Field>(COLUMN_FIELD_PII,"INTEGER",Field::pii ),
            ColumnBinding<Field>(COLUMN_FIELD_REQUIRED,"INTEGER",Field::required ),
            ColumnBinding<Field>(COLUMN_FIELD_INTEGER_ONLY,"INTEGER",Field::integerOnly ),
            ColumnBinding<Field>(COLUMN_FIELD_NUMBER_OF_RESIDENTS,"INTEGER",Field::numberOfResidents ),
            ColumnBinding<Field>(COLUMN_FIELD_DATE,"INTEGER",Field::date ),
            ColumnBinding<Field>(COLUMN_FIELD_TIME,"INTEGER",Field::time ),
            ColumnBinding<Field>(COLUMN_FIELD_MINIMUM,"REAL",Field::minimum ),
            ColumnBinding<Field>(COLUMN_FIELD_MAXIMUM,"REAL",Field::maximum ),
        )
    }
}