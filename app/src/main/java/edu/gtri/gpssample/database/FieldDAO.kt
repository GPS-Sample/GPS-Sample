package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.database.models.Field

class FieldDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createField( field: Field) : Int
    {
        val values = ContentValues()

        putField( field, values )

        return dao.writableDatabase.insert(DAO.TABLE_FIELD, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putField(field: Field, values: ContentValues)
    {
        values.put(DAO.COLUMN_FIELD_NAME, field.name )
        values.put(DAO.COLUMN_FIELD_STUDY_ID, field.studyId )
        values.put(DAO.COLUMN_FIELD_TYPE, field.type.toString() )
        values.put(DAO.COLUMN_FIELD_PII, field.pii )
        values.put(DAO.COLUMN_FIELD_REQUIRED, field.required )
        values.put(DAO.COLUMN_FIELD_INTEGER_ONLY, field.integerOnly )
        values.put(DAO.COLUMN_FIELD_DATE, field.date )
        values.put(DAO.COLUMN_FIELD_TIME, field.time )
        values.put(DAO.COLUMN_FIELD_OPTION_1, field.option1 )
        values.put(DAO.COLUMN_FIELD_OPTION_2, field.option2 )
        values.put(DAO.COLUMN_FIELD_OPTION_3, field.option3 )
        values.put(DAO.COLUMN_FIELD_OPTION_4, field.option4 )
    }

    //--------------------------------------------------------------------------
    fun updateField( field: Field)
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(field.id.toString())

        val values = ContentValues()

        putField( field, values )

        db.update(DAO.TABLE_FIELD, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getField( fieldId: Int ): Field?
    {
        var field: Field? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} WHERE ${DAO.COLUMN_ID} = $fieldId"

        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            field = createField( cursor )
        }

        cursor.close()
        db.close()

        return field
    }

    //--------------------------------------------------------------------------
    fun  createField( cursor: Cursor): Field
    {
        val field = Field()

        field.id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        field.name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_NAME))
        field.studyId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_STUDY_ID))
        field.type = FieldType.valueOf(cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_TYPE)))
        field.pii = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_PII)).toBoolean()
        field.required = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_REQUIRED)).toBoolean()
        field.integerOnly = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_INTEGER_ONLY)).toBoolean()
        field.date = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATE)).toBoolean()
        field.time = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_TIME)).toBoolean()
        field.option1 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_1))
        field.option2 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_2))
        field.option3 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_3))
        field.option4 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_4))

        return field
    }

    //--------------------------------------------------------------------------
    fun getFields( studyId: Int ): List<Field>
    {
        val fields = ArrayList<Field>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} WHERE ${DAO.COLUMN_FIELD_STUDY_ID} = $studyId"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            fields.add( createField( cursor ))
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
            fields.add( createField( cursor ))
        }

        cursor.close()
        db.close()

        return fields
    }

    //--------------------------------------------------------------------------
    fun deleteField( field: Field)
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(field.id.toString())
        db.delete(DAO.TABLE_FIELD, whereClause, args)
        db.close()
    }
}