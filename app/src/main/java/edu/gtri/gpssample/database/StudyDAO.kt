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
        values.put( DAO.COLUMN_UUID, study.uuid )
        values.put( DAO.COLUMN_STUDY_CONFIG_UUID, study.config_uuid )
        values.put( DAO.COLUMN_STUDY_NAME, study.name )
        values.put( DAO.COLUMN_STUDY_SAMPLING_METHOD, study.samplingMethod )
        values.put( DAO.COLUMN_STUDY_SAMPLE_SIZE, study.sampleSize )
        values.put( DAO.COLUMN_STUDY_SAMPLE_SIZE_INDEX, study.sampleSizeIndex )
    }

    //--------------------------------------------------------------------------
    fun updateStudy( study: Study )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(study.uuid)
        val values = ContentValues()

        putStudy( study, values )

        db.update(DAO.TABLE_STUDY, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getStudy( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun getStudy( uuid: String ): Study?
    {
        var study: Study? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_STUDY} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
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
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val config_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_CONFIG_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_NAME))
        val samplingMethod = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_SAMPLING_METHOD))
        val sampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_SAMPLE_SIZE))
        val sampleSizeIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_SAMPLE_SIZE_INDEX))

        return Study( uuid, config_uuid, name, samplingMethod, sampleSize, sampleSizeIndex )
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
    fun getStudies( config_uuid: String ): List<Study>
    {
        val studies = ArrayList<Study>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_STUDY} WHERE ${DAO.COLUMN_STUDY_CONFIG_UUID} = '$config_uuid'"
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
        val fields = DAO.fieldDAO.getFields( study.uuid )

        for (field in fields)
        {
            DAO.fieldDAO.deleteField( field )
        }

        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(study.uuid.toString())

        db.delete(DAO.TABLE_STUDY, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteOrphans()
    {
        val studies = getStudies()

        for (study in studies)
        {
            if (DAO.configDAO.doesNotExist( study.config_uuid ))
            {
                deleteStudy( study )
            }
        }
    }
}