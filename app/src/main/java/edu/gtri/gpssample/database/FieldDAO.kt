package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.constants.FieldTypeConverter
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FieldOption
import edu.gtri.gpssample.database.models.Study

class FieldDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createOrUpdateField( field: Field, study : Study ) : Field?
    {
        if (exists( field ))
        {
            updateField( field, study )
        }
        else
        {
            val values = ContentValues()
            putField( field, study, values )
            field.id = dao.writableDatabase.insert(DAO.TABLE_FIELD, null, values).toInt()
            field.id?.let { id ->
                Log.d( "xxx", "new field id = ${id}")
                for (fieldOption in field.fieldOptions)
                {
                    DAO.fieldOptionDAO.createOrUpdateFieldOption( fieldOption, field )
                }
            } ?: return null
        }

        return field
    }

    //--------------------------------------------------------------------------
    fun putField( field: Field, study : Study, values: ContentValues )
    {
        field.id?.let { id ->
            Log.d( "xxx", "existing field id = ${id}")
            values.put( DAO.COLUMN_ID, field.id )
        }

        values.put( DAO.COLUMN_STUDY_ID, study.id )
        values.put( DAO.COLUMN_FIELD_NAME, field.name )
        values.put( DAO.COLUMN_FIELD_TYPE_INDEX, FieldTypeConverter.toIndex(field.type))
        values.put( DAO.COLUMN_FIELD_BLOCK_CONTAINER, field.fieldBlockContainer )
        values.put( DAO.COLUMN_FIELD_BLOCK_UUID, field.fieldBlockUUID )
        values.put( DAO.COLUMN_FIELD_PII, field.pii )
        values.put( DAO.COLUMN_FIELD_REQUIRED, field.required )
        values.put( DAO.COLUMN_FIELD_INTEGER_ONLY, field.integerOnly )
        values.put( DAO.COLUMN_FIELD_DATE, field.date )
        values.put( DAO.COLUMN_FIELD_TIME, field.time )

        // TODO: use look up tables
        val type = FieldTypeConverter.toIndex(field.type)
        values.put( DAO.COLUMN_FIELD_TYPE_INDEX, type )
    }

    //--------------------------------------------------------------------------
    fun updateField( field: Field, study : Study )
    {
        field.id?.let{ id ->
            Log.d( "xxx", "update field id ${id}")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putField( field, study, values )

            dao.writableDatabase.update(DAO.TABLE_FIELD, values, whereClause, args )
        }
    }

    //--------------------------------------------------------------------------
    fun exists( field: Field ): Boolean
    {
        field.id?.let { id ->
            getField( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun  buildField(cursor: Cursor ): Field
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_NAME))
        val typeIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_TYPE_INDEX))
        val fieldBlockContainer = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_BLOCK_CONTAINER)).toBoolean()
        val fieldBlockUUID = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_BLOCK_UUID))
        val pii = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_PII)).toBoolean()
        val required = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_REQUIRED)).toBoolean()
        val integerOnly = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_INTEGER_ONLY)).toBoolean()
        val date = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATE)).toBoolean()
        val time = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_TIME)).toBoolean()

        val type = FieldTypeConverter.fromIndex(typeIndex)

        return Field(id, name, type, fieldBlockContainer, fieldBlockUUID, pii, required, integerOnly, date, time, ArrayList<FieldOption>())
    }

    //--------------------------------------------------------------------------
    fun getField( id : Int ): Field?
    {
        var field: Field? = null
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} where id=${id}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            field = buildField( cursor )
            field.fieldOptions = DAO.fieldOptionDAO.getFieldOptions( field )
        }

        cursor.close()

        return field
    }

    //--------------------------------------------------------------------------
    fun getFields(study : Study): ArrayList<Field>
    {
        val fields = ArrayList<Field>()
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} where study_id=${study.id}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val field = buildField( cursor )
            field.fieldOptions = DAO.fieldOptionDAO.getFieldOptions( field )
            val rules = DAO.ruleDAO.getRulesForField(field)
            study.rules.addAll(rules)
            fields.add( field)
        }

        cursor.close()

        return fields
    }

    //--------------------------------------------------------------------------
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

    //--------------------------------------------------------------------------
    fun deleteField( field: Field )
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(field.id.toString())

        dao.writableDatabase.delete(DAO.TABLE_FIELD, whereClause, args)
    }
}