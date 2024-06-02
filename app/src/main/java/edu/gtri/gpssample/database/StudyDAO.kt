package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.constants.SamplingMethod
import edu.gtri.gpssample.constants.SamplingMethodConverter
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.extensions.toBoolean

class StudyDAO(private var dao: DAO)
{
    fun createOrUpdateStudy( study: Study ) : Study?
    {
        val existingStudy = getStudy( study.uuid )

        if (existingStudy != null)
        {
            if (study.doesNotEqual( existingStudy ))
            {
                updateStudy( study )
                Log.d( "xxx", "Updated Study with ID ${study.uuid}")
            }
        }
        else
        {
            val values = ContentValues()
            putStudy(study, values)
            if (dao.writableDatabase.insert(DAO.TABLE_STUDY, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "Created Study with ID ${study.uuid}")
        }

        // add fields
        for (field in study.fields)
        {
            DAO.fieldDAO.createOrUpdateField( field, study )
        }

        // add rules
        for (rule in study.rules)
        {
            DAO.ruleDAO.createOrUpdateRule( rule )
        }

        // add filters
        for(filter in study.filters)
        {
            DAO.filterDAO.createOrUpdateFilter(filter, study);
        }

        return study
    }

    private fun putStudy( study: Study, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, study.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, study.creationDate )
        values.put( DAO.COLUMN_STUDY_NAME, study.name )
        values.put( DAO.COLUMN_STUDY_SAMPLE_SIZE, study.sampleSize )

        // convert enum to int.  Maybe not do this and have look up tables?
        var index = SampleTypeConverter.toIndex(study.sampleType)
        values.put( DAO.COLUMN_STUDY_SAMPLE_SIZE_INDEX, index)
        index = SamplingMethodConverter.toIndex(study.samplingMethod)
        values.put( DAO.COLUMN_STUDY_SAMPLING_METHOD_INDEX, index )
    }

    fun exists( study: Study ): Boolean
    {
        getStudy( study.uuid )?.let {
            return true
        } ?: return false
    }

    @SuppressLint("Range")
    private fun buildStudy(cursor: Cursor ): Study
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_NAME))
        val samplingMethodIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_SAMPLING_METHOD_INDEX))
        val sampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_SAMPLE_SIZE))
        val sampleSizeIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_SAMPLE_SIZE_INDEX))

        // convert enum to int.  Maybe not do this and have look up tables?
        val sampleType = SampleTypeConverter.fromIndex(sampleSizeIndex)
        val samplingMethod = SamplingMethodConverter.fromIndex(samplingMethodIndex)

        val study = Study( uuid, creationDate, name, samplingMethod, sampleSize, sampleType )

        return study
    }

    fun getStudy( uuid: String ): Study?
    {
        var study: Study? = null
        val query = "SELECT study.* FROM ${DAO.TABLE_STUDY} as study WHERE ${DAO.COLUMN_UUID} = '${uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            study = buildStudy( cursor )
            study.fields = DAO.fieldDAO.getFields(study)
            // study.rules is loaded by getFields()
            study.filters.addAll(DAO.filterDAO.getFilters(study))
        }

        cursor.close()

        return study
    }

    fun getStudies( config: Config ): ArrayList<Study>
    {
        val studies = ArrayList<Study>()

        val query = "SELECT study.*, conn.${DAO.COLUMN_CONFIG_UUID}, conn.${DAO.COLUMN_STUDY_UUID} FROM ${DAO.TABLE_STUDY} as study, " +
                "${DAO.CONNECTOR_TABLE_CONFIG__STUDY} as conn WHERE study.${DAO.COLUMN_UUID} = conn.${DAO.COLUMN_STUDY_UUID} and "  + "conn.${DAO.COLUMN_CONFIG_UUID} = '${config.uuid}'"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val study = buildStudy( cursor )
            studies.add( study )
            study.fields = DAO.fieldDAO.getFields(study)
            // study.rules is loaded by getFields()
            study.filters.addAll(DAO.filterDAO.getFilters(study))
        }

        cursor.close()

        return studies
    }

    fun getStudies(): ArrayList<Study>
    {
        val studies = ArrayList<Study>()

        val query = "SELECT * FROM ${DAO.TABLE_STUDY}"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val study = buildStudy( cursor )
            study.fields = DAO.fieldDAO.getFields(study)
            // study.rules is loaded by getFields()
            study.filters.addAll(DAO.filterDAO.getFilters(study))
            studies.add( study )
        }

        cursor.close()

        return studies
    }

    fun updateStudy( study: Study )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(study.uuid)
        val values = ContentValues()

        putStudy( study, values )

        dao.writableDatabase.update(DAO.TABLE_STUDY, values, whereClause, args )
    }

    fun deleteStudy( study: Study )
    {
        val filters = DAO.filterDAO.getFilters( study )

        for (filter in filters)
        {
            DAO.filterDAO.deleteFilter( filter )
        }

        // val rules = DAO.ruleDAO.getRules( study )

        for (rule in study.rules)
        {
            DAO.ruleDAO.deleteRule( rule )
        }

        val fields = DAO.fieldDAO.getFields( study )

        for (field in fields)
        {
            DAO.fieldDAO.deleteField( field )
        }

        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(study.uuid)

        dao.writableDatabase.delete(DAO.TABLE_STUDY, whereClause, args)
    }
}