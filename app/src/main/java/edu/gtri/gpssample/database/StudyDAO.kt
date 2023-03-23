package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.constants.SampleTypeConverter
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
    private fun putStudy( study: Study, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, study.uuid )
        values.put( DAO.COLUMN_STUDY_CONFIG_UUID, study.config_uuid )
        values.put( DAO.COLUMN_STUDY_NAME, study.name )
        values.put( DAO.COLUMN_STUDY_SAMPLING_METHOD, study.samplingMethod )
        values.put( DAO.COLUMN_STUDY_SAMPLE_SIZE, study.sampleSize )

        // convert enum to int.  Maybe not do this and have look up tables?
        val index = SampleTypeConverter.toIndex(study.sampleType)
        values.put( DAO.COLUMN_STUDY_SAMPLE_SIZE_INDEX, index)
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
        val query = "SELECT study.* FROM ${DAO.TABLE_STUDY} as study WHERE ${DAO.COLUMN_UUID} = '$uuid'"
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
    @SuppressLint("Range")
    private fun createStudy(cursor: Cursor ): Study
    {
        val id = cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_ID}"))
        val uuid = cursor.getString(cursor.getColumnIndex("${DAO.COLUMN_UUID}"))
        val config_uuid = cursor.getString(cursor.getColumnIndex("${DAO.COLUMN_STUDY_CONFIG_UUID}"))
        val name = cursor.getString(cursor.getColumnIndex("${DAO.COLUMN_STUDY_NAME}"))
        val samplingMethod = cursor.getString(cursor.getColumnIndex("${DAO.COLUMN_STUDY_SAMPLING_METHOD}"))
        val sampleSize = cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_STUDY_SAMPLE_SIZE}"))
        val sampleSizeIndex = cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_STUDY_SAMPLE_SIZE_INDEX}"))

        // convert enum to int.  Maybe not do this and have look up tables?
        val sampleType = SampleTypeConverter.fromIndex(sampleSizeIndex)
        return Study( id, uuid, config_uuid, name, samplingMethod, sampleSize, sampleType )
    }

    //--------------------------------------------------------------------------
    fun getStudies(): List<Study>
    {
        val studies = ArrayList<Study>()
        val db = dao.writableDatabase
        val query = "SELECT study.* FROM ${DAO.TABLE_STUDY} as study"
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
    fun getStudies( config_id: Int ): ArrayList<Study>
    {
        val studies = ArrayList<Study>()
        var db = dao.writableDatabase
        var query = "SELECT study.*, conn.${DAO.COLUMN_CONFIG_ID}, conn.${DAO.COLUMN_STUDY_ID} FROM ${DAO.TABLE_STUDY} as study, " +
                    "${DAO.TABLE_CONFIG_STUDY} as conn WHERE study.${DAO.COLUMN_ID} = conn.${DAO.COLUMN_STUDY_ID} and "  +
                    "conn.${DAO.COLUMN_CONFIG_ID} = $config_id"


        var cursor = db.rawQuery(query, null)

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
        study.id?.let{ id ->
            val fields = DAO.fieldDAO.getFields( id )

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

    }

    //--------------------------------------------------------------------------

}