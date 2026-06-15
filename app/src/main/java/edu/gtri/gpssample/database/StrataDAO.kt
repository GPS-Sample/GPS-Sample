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
}
