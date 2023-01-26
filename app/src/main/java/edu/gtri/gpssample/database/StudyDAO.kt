package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.models.Study

class StudyDAO(private var dao: GPSSampleDAO)
{
    //--------------------------------------------------------------------------
    fun createStudy( study: Study) : Int
    {
        val values = ContentValues()

        putStudy( study, values )

        return dao.writableDatabase.insert(GPSSampleDAO.TABLE_STUDY, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putStudy(study: Study, values: ContentValues)
    {
        values.put(GPSSampleDAO.COLUMN_STUDY_NAME, study.name )
        values.put(GPSSampleDAO.COLUMN_STUDY_CONFIG_ID, study.configId )
        values.put(GPSSampleDAO.COLUMN_STUDY_IS_VALID, study.isValid )
    }

    //--------------------------------------------------------------------------
    fun updateStudy( study: Study)
    {
        val db = dao.writableDatabase
        val whereClause = "${GPSSampleDAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(study.id.toString())

        val values = ContentValues()

        putStudy( study, values )

        db.update(GPSSampleDAO.TABLE_STUDY, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getStudy( id: Int ): Study?
    {
        var study: Study? = null

        val db = dao.writableDatabase
        val query = "SELECT * FROM ${GPSSampleDAO.TABLE_STUDY} WHERE ${GPSSampleDAO.COLUMN_ID} = $id"

        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            study = createStudy( cursor )
        }

        cursor.close()
        db.close()

        return study
    }

    //--------------------------------------------------------------------------
    fun createStudy( cursor: Cursor): Study
    {
        val study = Study()

        study.id = Integer.parseInt(cursor.getString(0))
        study.name = cursor.getString(cursor.getColumnIndex(GPSSampleDAO.COLUMN_STUDY_NAME))
        study.configId = cursor.getInt(cursor.getColumnIndex(GPSSampleDAO.COLUMN_STUDY_CONFIG_ID))
        study.isValid = cursor.getInt(cursor.getColumnIndex(GPSSampleDAO.COLUMN_STUDY_IS_VALID)).toBoolean()

        return study
    }

    //--------------------------------------------------------------------------
    fun getStudies(): List<Study>
    {
        val studies = ArrayList<Study>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${GPSSampleDAO.TABLE_STUDY}"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            studies.add( createStudy( cursor ))
        }

        cursor.close()
        db.close()

        return studies
    }

    //--------------------------------------------------------------------------
    fun getStudies( configId: Int ): List<Study>
    {
        val studies = ArrayList<Study>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${GPSSampleDAO.TABLE_STUDY} WHERE ${GPSSampleDAO.COLUMN_STUDY_CONFIG_ID} = $configId"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            studies.add( createStudy( cursor ))
        }

        cursor.close()
        db.close()

        return studies
    }

    //--------------------------------------------------------------------------
    fun getValidStudies( configId: Int ): List<Study>
    {
        val studies = ArrayList<Study>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${GPSSampleDAO.TABLE_STUDY} WHERE ${GPSSampleDAO.COLUMN_STUDY_CONFIG_ID} = $configId AND ${GPSSampleDAO.COLUMN_STUDY_IS_VALID} = 1"

        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            studies.add( createStudy( cursor ))
        }

        cursor.close()
        db.close()

        return studies
    }

    //--------------------------------------------------------------------------
    fun deleteStudy( study: Study)
    {
        val fields = GPSSampleDAO.fieldDAO.getFields( study.id )

        for (field in fields)
        {
            GPSSampleDAO.fieldDAO.deleteField( field )
        }

        val db = dao.writableDatabase
        val whereClause = "${GPSSampleDAO.COLUMN_ID} = ?"
        val args = arrayOf(study.id.toString())
        db.delete(GPSSampleDAO.TABLE_STUDY, whereClause, args)
        db.close()
    }
}