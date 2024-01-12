package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.constants.SamplingMethod
import edu.gtri.gpssample.constants.SamplingMethodConverter
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.extensions.toBoolean
import edu.gtri.gpssample.database.models.Study

class StudyDAO(private var dao: DAO)
{
    fun createOrUpdateStudy( study: Study ) : Study?
    {
        // if study exists and is untouched, use it.
        // if study exists and is modified, created a new one.

        study.id?.let{id ->
            var testStudy = DAO.studyDAO.getStudy(id)

            if(study.equals( testStudy))
            {
                Log.d( "xxx", "study with id ${id} already exists!")
                return study
            }
        }

        if (exists( study ))
        {
            updateStudy( study )
        }
        else
        {
            val values = ContentValues()
            putStudy(study, values)
            study.id = dao.writableDatabase.insert(DAO.TABLE_STUDY, null, values).toInt()
            study.id?.let { id ->
                Log.d( "xxx", "new study id = ${id}")
            } ?: return null
        }

        study.id?.let { id ->
            for (collectionTeam in study.collectionTeams)
            {
                DAO.collectionTeamDAO.createOrUpdateTeam( collectionTeam )
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

            for(rule in study.rules)
            {
                Log.d("XXXXXXX", "the id ${rule.id}")
            }

            // add filters
            for(filter in study.filters)
            {
                DAO.filterDAO.createOrUpdateFilter(filter, study);
            }

        }

        return study
    }

    private fun putStudy( study: Study, values: ContentValues )
    {
        study.id?.let { id ->
            Log.d( "xxx", "existing study id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, study.creationDate )
        values.put( DAO.COLUMN_STUDY_NAME, study.name )
        values.put( DAO.COLUMN_STUDY_SAMPLE_SIZE, study.sampleSize )
        values.put( DAO.COLUMN_COLLECTION_TEAM_ID, study.selectedCollectionTeamId )

        // convert enum to int.  Maybe not do this and have look up tables?
        var index = SampleTypeConverter.toIndex(study.sampleType)
        values.put( DAO.COLUMN_STUDY_SAMPLE_SIZE_INDEX, index)
        index = SamplingMethodConverter.toIndex(study.samplingMethod)
        values.put( DAO.COLUMN_STUDY_SAMPLING_METHOD_INDEX, index )
    }

    fun exists( study: Study ): Boolean
    {
        study.id?.let { id ->
            getStudy( id )?.let { study
                return true
            } ?: return false
        } ?: return false
    }

    @SuppressLint("Range")
    private fun buildStudy(cursor: Cursor ): Study
    {
        val id = cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_ID}"))
        val selectedCollectionTeamId = cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_COLLECTION_TEAM_ID}"))
        val creationDate = cursor.getLong(cursor.getColumnIndex("${DAO.COLUMN_CREATION_DATE}"))
        val name = cursor.getString(cursor.getColumnIndex("${DAO.COLUMN_STUDY_NAME}"))
        val samplingMethodIndex = cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_STUDY_SAMPLING_METHOD_INDEX}"))
        val sampleSize = cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_STUDY_SAMPLE_SIZE}"))
        val sampleSizeIndex = cursor.getInt(cursor.getColumnIndex("${DAO.COLUMN_STUDY_SAMPLE_SIZE_INDEX}"))

        // convert enum to int.  Maybe not do this and have look up tables?
        val sampleType = SampleTypeConverter.fromIndex(sampleSizeIndex)
        val samplingMethod = SamplingMethodConverter.fromIndex(samplingMethodIndex)

        val study = Study( id, creationDate, name, samplingMethod, sampleSize, sampleType, selectedCollectionTeamId )

        return study
    }

    fun getStudy( id: Int ): Study?
    {
        var study: Study? = null
        val query = "SELECT study.* FROM ${DAO.TABLE_STUDY} as study WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            study = buildStudy( cursor )
            study.fields = DAO.fieldDAO.getFields(study) as ArrayList<Field>
            study.filters.addAll(DAO.filterDAO.getFilters(study))
            study.collectionTeams = DAO.collectionTeamDAO.getCollectionTeams( study )
        }

        cursor.close()

        return study
    }

    fun getStudies( config: Config ): ArrayList<Study>
    {
        val studies = ArrayList<Study>()

        config.id?.let { id ->
            val query = "SELECT study.*, conn.${DAO.COLUMN_CONFIG_ID}, conn.${DAO.COLUMN_STUDY_ID} FROM ${DAO.TABLE_STUDY} as study, " +
                    "${DAO.TABLE_CONFIG_STUDY} as conn WHERE study.${DAO.COLUMN_ID} = conn.${DAO.COLUMN_STUDY_ID} and "  +
                    "conn.${DAO.COLUMN_CONFIG_ID} = ${id}"

            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val study = buildStudy( cursor )
                studies.add( study )
                study.fields = DAO.fieldDAO.getFields(study)
                study.filters.addAll(DAO.filterDAO.getFilters(study))
                study.collectionTeams = DAO.collectionTeamDAO.getCollectionTeams( study )
            }

            cursor.close()
        }

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
            study.filters.addAll(DAO.filterDAO.getFilters(study))
            study.collectionTeams = DAO.collectionTeamDAO.getCollectionTeams( study )
            studies.add( study )
        }

        cursor.close()

        return studies
    }

    fun updateStudy( study: Study )
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        study.id?.let { id ->
            Log.d( "xxx", "update study id ${id}")

            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putStudy( study, values )

            dao.writableDatabase.update(DAO.TABLE_STUDY, values, whereClause, args )
        }
    }

    fun deleteStudy( study: Study )
    {
        study.id?.let{ id ->

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

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(study.id.toString())

            dao.writableDatabase.delete(DAO.TABLE_STUDY, whereClause, args)
        }
    }
}