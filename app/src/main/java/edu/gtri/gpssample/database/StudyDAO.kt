package edu.gtri.gpssample.database

import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.database.models.Study

class StudyDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createStudy( study: Study ) : Int
    {
        val values = ContentValues()

        putStudy( study, values )

        return dao.writableDatabase.insert(DAO.TABLE_STUDY, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putStudy( study: Study, values: ContentValues )
    {
        values.put( DAO.COLUMN_STUDY_NAME, study.name )
        values.put( DAO.COLUMN_STUDY_CONFIG_ID, study.configId )
        values.put( DAO.COLUMN_STUDY_IS_VALID, study.isValid )
    }

    //--------------------------------------------------------------------------
    fun updateStudy( study: Study )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args: Array<String> = arrayOf(study.id.toString())
        val values = ContentValues()

        putStudy( study, values )

        db.update(DAO.TABLE_STUDY, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getStudy( id: Int ): Study?
    {
        var study: Study? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_STUDY} WHERE ${DAO.COLUMN_ID} = $id"
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
    fun createStudy( cursor: Cursor ): Study
    {
        val study = Study()

        study.id = Integer.parseInt(cursor.getString(0))
        study.name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_NAME))
        study.configId = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_CONFIG_ID))
        study.isValid = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_IS_VALID)).toBoolean()

        return study
    }

    //--------------------------------------------------------------------------
    fun getStudies(): List<Study>
    {
        val studies = ArrayList<Study>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_STUDY}"
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
        val query = "SELECT * FROM ${DAO.TABLE_STUDY} WHERE ${DAO.COLUMN_STUDY_CONFIG_ID} = $configId"
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
        val query = "SELECT * FROM ${DAO.TABLE_STUDY} WHERE ${DAO.COLUMN_STUDY_CONFIG_ID} = $configId AND ${DAO.COLUMN_STUDY_IS_VALID} = 1"
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
    fun deleteStudy( study: Study )
    {
        val fields = DAO.fieldDAO.getFields( study.id )

        for (field in fields)
        {
            DAO.fieldDAO.deleteField( field )
        }

        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(study.id.toString())

        db.delete(DAO.TABLE_STUDY, whereClause, args)
        db.close()
    }
}