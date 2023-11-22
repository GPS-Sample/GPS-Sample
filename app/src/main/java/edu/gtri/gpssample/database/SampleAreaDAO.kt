package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*

class SampleAreaDAO(private var dao: DAO)
{
    fun createOrUpdateSampleArea( sampleArea: SampleArea, study: Study ) : SampleArea?
    {
        if (exists( sampleArea ))
        {
            updateSampleArea( sampleArea, study )
            updateSampleAreaElements(sampleArea)
        }
        else
        {
            val values = ContentValues()
            putSampleArea( sampleArea, study, values )
            sampleArea.id = dao.writableDatabase.insert(DAO.TABLE_SAMPLE_AREA, null, values).toInt()
            sampleArea.id?.let {
            } ?: return null

            updateSampleAreaElements(sampleArea)
        }

        return sampleArea
    }

    private fun updateSampleAreaElements(sampleArea : SampleArea) : SampleArea?
    {
        sampleArea.id?.let {id ->
            Log.d( "xxx", "new sampleArea id = ${id}")

            for (latLon in sampleArea.vertices)
            {
                DAO.latLonDAO.createOrUpdateLatLon(latLon, sampleArea, null)
            }

            for (location in sampleArea.locations)
            {
                DAO.locationDAO.updateConnectorTable( location, sampleArea )
//                DAO.locationDAO.createOrUpdateLocation( location, sampleArea )
            }

            return sampleArea
        } ?: return null
    }

    fun exists( sampleArea: SampleArea ): Boolean
    {
        sampleArea.id?.let { id ->
            getSampleArea( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    fun putSampleArea( sampleArea: SampleArea, study: Study, values: ContentValues )
    {
        sampleArea.id?.let { id ->
            Log.d( "xxx", "existing sampleArea id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_CREATION_DATE, sampleArea.creationDate )
        values.put( DAO.COLUMN_STUDY_ID, study.id )
    }

    @SuppressLint("Range")
    private fun createSampleArea(cursor: Cursor): SampleArea
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val sampleArea = SampleArea( id, creationDate )

        sampleArea.vertices = DAO.latLonDAO.getLatLonsWithSampleAreaId( id )
        sampleArea.locations = DAO.locationDAO.getLocations( sampleArea )

        return sampleArea
    }

    fun updateSampleArea( sampleArea: SampleArea, study: Study )
    {
        sampleArea.id?.let{ id ->
            Log.d( "xxx", "update sampleArea id ${id}")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putSampleArea( sampleArea, study, values )

            dao.writableDatabase.update(DAO.TABLE_SAMPLE_AREA, values, whereClause, args )
        }
    }

    private fun getSampleArea( id: Int ): SampleArea?
    {
        var sampleArea: SampleArea? = null
        val query = "SELECT * FROM ${DAO.TABLE_SAMPLE_AREA} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            sampleArea = createSampleArea( cursor )
        }

        cursor.close()

        return sampleArea
    }

    fun getSampleAreas( study: Study ): ArrayList<SampleArea>
    {
        val sampleAreas = ArrayList<SampleArea>()

        study.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_SAMPLE_AREA} WHERE ${DAO.COLUMN_STUDY_ID} = $id"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                sampleAreas.add( createSampleArea( cursor ))
            }

            cursor.close()
        }

        return sampleAreas
    }

    fun delete( sampleArea: SampleArea )
    {
        sampleArea.id?.let {sampleAreaId ->

            // collection teams are dependent on SampleAreas
//            DAO.enumerationTeamDAO.getCollectionTeams( sampleAreaId ).map {
//                DAO.enumerationTeamDAO.deleteTeam( it )
//            }

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(sampleAreaId.toString())

            dao.writableDatabase.delete(DAO.TABLE_SAMPLE_AREA, whereClause, args)
        }
    }
}