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
import edu.gtri.gpssample.constants.SampleType
import edu.gtri.gpssample.constants.SampleTypeConverter
import edu.gtri.gpssample.constants.SamplingMethodConverter
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_CREATION_DATE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STRATA_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STRATA_SAMPLE_SIZE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STRATA_SAMPLE_TYPE_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_SAMPLE_SIZE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_SAMPLE_SIZE_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_SAMPLING_METHOD_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_SUBSET_SAMPLE_NAME
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_SUBSET_SAMPLE_SIZE
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_SUBSET_SAMPLE_SIZE_INDEX
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_STUDY_UUID
import edu.gtri.gpssample.database.DAO.Companion.COLUMN_VERSION
import edu.gtri.gpssample.database.models.*

class StrataDAO(private var dao: DAO)
{
    fun createOrUpdateStrata( strata: Strata, version: String ) : Strata?
    {
        strata.version = version

        val values = ContentValues()
        putStrata( strata, values )

        dao.upsert( DAO.TABLE_STRATA, values )

        return strata
    }

    private fun putStrata( strata: Strata, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, strata.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, strata.creationDate )
        values.put( DAO.COLUMN_VERSION, strata.version )
        values.put( DAO.COLUMN_STUDY_UUID, strata.studyUuid )
        values.put( DAO.COLUMN_STRATA_NAME, strata.name )
        values.put( DAO.COLUMN_STRATA_SAMPLE_SIZE, strata.sampleSize )
        values.put( DAO.COLUMN_STRATA_SAMPLE_TYPE_INDEX, strata.sampleType.ordinal )
    }

    @SuppressLint("Range")
    private fun buildStrata(cursor: Cursor): Strata
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val studyUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STUDY_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_STRATA_NAME))
        val sampleSize = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STRATA_SAMPLE_SIZE))
        val sampleTypeIndex = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STRATA_SAMPLE_TYPE_INDEX))

        val sampleType = SampleType.values()[sampleTypeIndex]

        return Strata( uuid, creationDate, studyUuid, name, sampleSize, sampleType, version )
    }

    fun getStrata( uuid: String ): Strata?
    {
        var strata: Strata? = null
        val query = "SELECT * FROM ${DAO.TABLE_STRATA} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            strata = buildStrata( cursor )
        }

        cursor.close()

        return strata
    }

    fun getStratasWithStudyUuid( studyUuid: String ): ArrayList<Strata>
    {
        val stratas = ArrayList<Strata>()
        val query = "SELECT * FROM ${DAO.TABLE_STRATA} WHERE ${DAO.COLUMN_STUDY_UUID} = '$studyUuid' ORDER BY ${DAO.COLUMN_CREATION_DATE}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            stratas.add( buildStrata(cursor))
        }

        cursor.close()

        return stratas
    }

    fun delete( strata: Strata )
    {
        val args = arrayOf(strata.uuid)
        val whereClause = "${DAO.COLUMN_UUID} = ?"

        dao.writableDatabase.delete(DAO.TABLE_STRATA, whereClause, args )
    }

    companion object
    {
        // Note!! CREATION_DATE was moved up to the standard column position
        val columnBindings = listOf(
            ColumnBinding<Strata>(COLUMN_CREATION_DATE, "INTEGER",Strata::creationDate),
            ColumnBinding<Strata>(COLUMN_VERSION,"TEXT",Strata::version ),
            ColumnBinding<Strata>(COLUMN_STUDY_UUID,"TEXT",Strata::studyUuid ),
            ColumnBinding<Strata>(COLUMN_STRATA_NAME,"TEXT",Strata::name ),
            ColumnBinding<Strata>(COLUMN_STRATA_SAMPLE_SIZE,"INTEGER",Strata::sampleSize ),
            ColumnBinding<Strata>(COLUMN_STRATA_SAMPLE_TYPE_INDEX,"INTEGER",{it.sampleType.ordinal} ),
        )
    }
}
