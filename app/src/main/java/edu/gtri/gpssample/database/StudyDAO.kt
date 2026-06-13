/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

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
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.extensions.toBoolean
import java.util.UUID

class StudyDAO(private var dao: DAO)
{
    fun createOrUpdateStudy( study: Study, version: String ) : Study?
    {
        study.version = version

        val values = ContentValues()
        putStudy( study, values )

        dao.upsert( DAO.TABLE_STUDY, values )

        // add fields
        for (field in study.fields)
        {
            DAO.fieldDAO.createOrUpdateField( field, study,field.version )
        }

        // add primary rules
        for (rule in study.primaryRules)
        {
            DAO.ruleDAO.createOrUpdateRule( rule )
        }

        // add subset rules
        for (rule in study.subsetRules)
        {
            DAO.ruleDAO.createOrUpdateRule( rule )
        }

        // add primary filters
        for (filter in study.primaryFilters)
        {
            DAO.filterDAO.createOrUpdateFilter( filter, study );
        }

        // add subset filters
        for (filter in study.subsetFilters)
        {
            DAO.filterDAO.createOrUpdateFilter( filter, study );
        }

        // add stratas
        for (strata in study.stratas)
        {
            DAO.strataDAO.createOrUpdateStrata( strata )
        }

        return study
    }

    private fun putStudy( study: Study, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, study.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, study.creationDate )
        values.put( DAO.COLUMN_VERSION, study.version )
        values.put( DAO.COLUMN_STUDY_NAME, study.name )
        values.put( DAO.COLUMN_STUDY_SAMPLE_SIZE, study.sampleSize )
        values.put( DAO.COLUMN_STUDY_SUBSET_SAMPLE_SIZE, study.subsetSampleSize )

        if (study.subsetSampleName.isNotEmpty())
        {
            values.put( DAO.COLUMN_STUDY_SUBSET_SAMPLE_NAME, study.subsetSampleName )
        }

        // convert enum to int.  Maybe not do this and have look up tables?
        var index = SampleTypeConverter.toIndex(study.sampleType)
        values.put( DAO.COLUMN_STUDY_SAMPLE_SIZE_INDEX, index)

        index = SampleTypeConverter.toIndex(study.subsetSampleType)
        values.put( DAO.COLUMN_STUDY_SUBSET_SAMPLE_SIZE_INDEX, index)

        index = SamplingMethodConverter.toIndex(study.samplingMethod)
        values.put( DAO.COLUMN_STUDY_SAMPLING_METHOD_INDEX, index )
    }

    @SuppressLint("Range")
    private fun buildStudy(cursor: Cursor ): Study
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_NAME))
        var subsetSampleName = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_SUBSET_SAMPLE_NAME))
        val samplingMethodIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_SAMPLING_METHOD_INDEX))
        val sampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_SAMPLE_SIZE))
        val sampleSizeIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_SAMPLE_SIZE_INDEX))
        val subsetSampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_SUBSET_SAMPLE_SIZE))
        val subsetSampleSizeIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_SUBSET_SAMPLE_SIZE_INDEX))

        // convert enum to int.  Maybe not do this and have look up tables?
        val sampleType = SampleTypeConverter.fromIndex(sampleSizeIndex)
        val samplingMethod = SamplingMethodConverter.fromIndex(samplingMethodIndex)
        val subsetSampleType = SampleTypeConverter.fromIndex(subsetSampleSizeIndex)

        if (subsetSampleName == null)
        {
            subsetSampleName = ""
        }

        val study = Study( uuid, creationDate, name, samplingMethod, sampleSize, sampleType, subsetSampleName, subsetSampleSize, subsetSampleType, ArrayList<Strata>(), ArrayList<Field>(), ArrayList<Rule>(), ArrayList<Filter>(), ArrayList<Rule>(), ArrayList<Filter>(), version )

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
            study.primaryFilters.addAll(DAO.filterDAO.getPrimaryFilters(study))
            study.subsetFilters.addAll(DAO.filterDAO.getSubsetFilters(study))
            study.stratas = DAO.strataDAO.getStratasWithStudyUuid(study.uuid )
        }

        cursor.close()

        return studies
    }

    fun deleteStudy( study: Study )
    {
        for (filter in study.primaryFilters)
        {
            DAO.filterDAO.deleteFilter( filter )
        }

        for (filter in study.subsetFilters)
        {
            DAO.filterDAO.deleteFilter( filter )
        }

        for (rule in study.primaryRules)
        {
            DAO.ruleDAO.deleteRule( rule )
        }

        for (rule in study.subsetRules)
        {
            DAO.ruleDAO.deleteRule( rule )
        }

        for (field in study.fields)
        {
            DAO.fieldDAO.deleteField( field )
        }

        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(study.uuid)

        dao.writableDatabase.delete(DAO.TABLE_STUDY, whereClause, args)
    }
}