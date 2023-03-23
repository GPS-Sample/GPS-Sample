package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.constants.FieldType
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.database.models.Field

class FieldDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createField( field: Field ) : Int
    {
        val values = ContentValues()

        putField( field, values )

        return dao.writableDatabase.insert(DAO.TABLE_FIELD, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putField( field: Field, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, field.uuid )
        values.put( DAO.COLUMN_FIELD_NAME, field.name )
        values.put( DAO.COLUMN_STUDY_ID, field.study_id )
        values.put( DAO.COLUMN_FIELD_TYPE, field.type )
        values.put( DAO.COLUMN_FIELD_PII, field.pii )
        values.put( DAO.COLUMN_FIELD_REQUIRED, field.required )
        values.put( DAO.COLUMN_FIELD_INTEGER_ONLY, field.integerOnly )
        values.put( DAO.COLUMN_FIELD_DATE, field.date )
        values.put( DAO.COLUMN_FIELD_TIME, field.time )
        values.put( DAO.COLUMN_FIELD_OPTION_1, field.option1 )
        values.put( DAO.COLUMN_FIELD_OPTION_2, field.option2 )
        values.put( DAO.COLUMN_FIELD_OPTION_3, field.option3 )
        values.put( DAO.COLUMN_FIELD_OPTION_4, field.option4 )
    }

    //--------------------------------------------------------------------------
    fun updateField( field: Field )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(field.uuid.toString())
        val values = ContentValues()

        putField( field, values )

        db.update(DAO.TABLE_FIELD, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getField( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun getField( uuid: String ): Field?
    {
        var field: Field? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
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
    @SuppressLint("Range")
    private fun  createField(cursor: Cursor ): Field
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_NAME))
        val study_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_STUDY_UUID))
        val type = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_TYPE))
        val pii = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_PII)).toBoolean()
        val required = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_REQUIRED)).toBoolean()
        val integerOnly = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_INTEGER_ONLY)).toBoolean()
        val date = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_DATE)).toBoolean()
        val time = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_FIELD_TIME)).toBoolean()
        val option1 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_1))
        val option2 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_2))
        val option3 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_3))
        val option4 = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_FIELD_OPTION_4))

        return Field(id, uuid, study_uuid, name, type, pii, required, integerOnly,date, time, option1, option2, option3, option4 )
    }

    //--------------------------------------------------------------------------
    fun getFields( study_id : Int ): List<Field>
    {
        val fields = ArrayList<Field>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_FIELD} WHERE ${DAO.COLUMN_STUDY_ID} = '$study_id'"
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
    fun deleteField( field: Field )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(field.uuid.toString())

        db.delete(DAO.TABLE_FIELD, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteOrphans()
    {
//        val fields = getFields()
//
//        for (field in fields)
//        {
//            if (DAO.studyDAO.doesNotExist( field.study_uuid ))
//            {
//                deleteField( field )
//            }
//        }
    }
}