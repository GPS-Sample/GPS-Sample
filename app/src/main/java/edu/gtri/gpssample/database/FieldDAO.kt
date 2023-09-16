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
        values.put( DAO.COLUMN_FIELD_OPTION_1, field.option1 )
        values.put( DAO.COLUMN_FIELD_OPTION_2, field.option2 )
        values.put( DAO.COLUMN_FIELD_OPTION_3, field.option3 )
        values.put( DAO.COLUMN_FIELD_OPTION_4, field.option4 )

        // TODO: use look up tables
        val type = FieldTypeConverter.toIndex(field.type)
        values.put( DAO.COLUMN_FIELD_TYPE_INDEX, type )
    }

    //--------------------------------------------------------------------------
    fun updateField( field: Field, study : Study )
    {
        val db = dao.writableDatabase

        field.id?.let{ id ->
            Log.d( "xxx", "update field id ${id}")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putField( field, study, values )

            db.update(DAO.TABLE_FIELD, values, whereClause, args )
        }

        db.close()
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
        val option1 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_1))
        val option2 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_2))
        val option3 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_3))
        val option4 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_4))

        val type = FieldTypeConverter.fromIndex(typeIndex)

        return Field(id, name, type, fieldBlockContainer, fieldBlockUUID, pii, required, integerOnly, date, time, ArrayList<FieldOption>(), option1, option2, option3, option4 )
    }

    //--------------------------------------------------------------------------
    fun getField( id : Int ): Field?
    {
        var field: Field? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} where id=${id}"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            field = buildField( cursor )
            field.fieldOptions = DAO.fieldOptionDAO.getFieldOptions( field )
        }

        cursor.close()
        db.close()

        return field
    }

    //--------------------------------------------------------------------------
    fun getFields(study : Study): ArrayList<Field>
    {
        val fields = ArrayList<Field>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} where study_id=${study.id}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val field = buildField( cursor )
            field.fieldOptions = DAO.fieldOptionDAO.getFieldOptions( field )
            val rules = DAO.ruleDAO.getRulesForField(field)
            study.rules.addAll(rules)
            fields.add( field)
        }

        cursor.close()
        db.close()

        return fields
    }

    //--------------------------------------------------------------------------
    fun getFields(): List<Field>
    {
        val fields = ArrayList<Field>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            fields.add( buildField( cursor ))
        }

        cursor.close()
        db.close()

        return fields
    }

    //--------------------------------------------------------------------------
    fun deleteField( field: Field )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(field.id.toString())

        db.delete(DAO.TABLE_FIELD, whereClause, args)
        db.close()
    }
}