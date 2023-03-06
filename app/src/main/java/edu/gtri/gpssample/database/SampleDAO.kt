package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.FilterRule
import edu.gtri.gpssample.database.models.Sample
import edu.gtri.gpssample.database.models.Study

class SampleDAO(private var dao: DAO)
{
    //--------------------------------------------------------------------------
    fun createSample( sample: Sample ) : Int
    {
        val values = ContentValues()

        putSample( sample, values )

        return dao.writableDatabase.insert(DAO.TABLE_SAMPLE, null, values).toInt()
    }

    //--------------------------------------------------------------------------
    fun putSample(sample: Sample, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, sample.uuid )
        values.put( DAO.COLUMN_SAMPLE_STUDY_UUID, sample.study_uuid )
        values.put( DAO.COLUMN_SAMPLE_NAME, sample.name )
        values.put( DAO.COLUMN_SAMPLE_NUM_ENUMERATORS, sample.numEnumerators )
    }

    //--------------------------------------------------------------------------
    @SuppressLint("Range")
    private fun createSample(cursor: Cursor): Sample
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val study_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_SAMPLE_STUDY_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_SAMPLE_NAME))
        val numEnumerators = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_SAMPLE_NUM_ENUMERATORS))

        return Sample(id, uuid, study_uuid, name, numEnumerators )
    }

    //--------------------------------------------------------------------------
    fun updateSample( sample: Sample )
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(sample.uuid)
        val values = ContentValues()

        putSample( sample, values )

        db.update(DAO.TABLE_SAMPLE, values, whereClause, args )
        db.close()
    }

    //--------------------------------------------------------------------------
    fun getSample( uuid: String ): Sample?
    {
        var sample: Sample? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_SAMPLE} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            sample = createSample( cursor )
        }

        cursor.close()
        db.close()

        return sample
    }

    //--------------------------------------------------------------------------
    fun exists( uuid: String ) : Boolean
    {
        return getSample( uuid ) != null
    }

    //--------------------------------------------------------------------------
    fun doesNotExist( uuid: String ) : Boolean
    {
        return !exists( uuid )
    }

    //--------------------------------------------------------------------------
    fun getSamples( study_uuid: String ): List<Sample>
    {
        val samples = ArrayList<Sample>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_SAMPLE} WHERE ${DAO.COLUMN_SAMPLE_STUDY_UUID} = '$study_uuid'"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            samples.add( createSample( cursor ))
        }

        cursor.close()
        db.close()

        return samples
    }

    //--------------------------------------------------------------------------
    fun getSamples(): List<Sample>
    {
        val samples = ArrayList<Sample>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_SAMPLE}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            samples.add( createSample( cursor ))
        }

        cursor.close()
        db.close()

        return samples
    }

    //--------------------------------------------------------------------------
    fun deleteSample( sample: Sample )
    {
        val navPlans = DAO.navPlanDAO.getNavPlans( sample.uuid )

        for (navPlan in navPlans)
        {
            DAO.navPlanDAO.deleteNavPlan( navPlan )
        }

        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(sample.uuid.toString())

        db.delete(DAO.TABLE_SAMPLE, whereClause, args)
        db.close()
    }

    //--------------------------------------------------------------------------
    fun deleteOrphans()
    {
        val samples = getSamples()

        for (sample in samples)
        {
            if (DAO.studyDAO.doesNotExist( sample.study_uuid ))
            {
                deleteSample( sample )
            }
        }
    }
}