package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.CollectionTeam
import edu.gtri.gpssample.database.models.LatLon
import edu.gtri.gpssample.database.models.Location
import edu.gtri.gpssample.database.models.Study

class CollectionTeamDAO(private var dao: DAO)
{
    fun createOrUpdateTeam(collectionTeam: CollectionTeam) : CollectionTeam?
    {
        if (exists( collectionTeam ))
        {
            updateTeam( collectionTeam )
        }
        else
        {
            collectionTeam.id = null
            val values = ContentValues()
            putTeam( collectionTeam, values )
            collectionTeam.id = dao.writableDatabase.insert(DAO.TABLE_COLLECTION_TEAM, null, values).toInt()
            collectionTeam.id?.let { id ->
                Log.d( "xxx", "new Team id = ${id}")
            } ?: return null
        }

        collectionTeam.id?.let {
            for (latLon in collectionTeam.polygon)
            {
                DAO.latLonDAO.createOrUpdateLatLon(latLon,null, null)
            }
        }

        updateConnectorTable( collectionTeam )

        return collectionTeam
    }

    fun updateConnectorTable( collectionTeam: CollectionTeam)
    {
        collectionTeam.id?.let { collectionTeamId ->
            for (latLon in collectionTeam.polygon)
            {
                latLon.id?.let { latLonId ->
                    val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_TEAM__LAT_LON} WHERE ${DAO.COLUMN_LAT_LON_ID} = $latLonId AND ${DAO.COLUMN_COLLECTION_TEAM_ID} = $collectionTeamId"
                    val cursor = dao.writableDatabase.rawQuery(query, null)
                    if (cursor.count == 0)
                    {
                        val values = ContentValues()
                        values.put( DAO.COLUMN_LAT_LON_ID, latLonId )
                        values.put( DAO.COLUMN_COLLECTION_TEAM_ID, collectionTeamId )
                        dao.writableDatabase.insert(DAO.TABLE_COLLECTION_TEAM__LAT_LON, null, values)
                    }
                    cursor.close()
                }
            }

            for (location in collectionTeam.locations)
            {
                location.id?.let { locationId ->
                    val query = "SELECT * FROM ${DAO.TABLE_LOCATION__COLLECTION_TEAM} WHERE ${DAO.COLUMN_LOCATION_ID} = $locationId AND ${DAO.COLUMN_COLLECTION_TEAM_ID} = $collectionTeamId"
                    val cursor = dao.writableDatabase.rawQuery(query, null)
                    if (cursor.count == 0)
                    {
                        val values = ContentValues()
                        values.put( DAO.COLUMN_LOCATION_ID, locationId )
                        values.put( DAO.COLUMN_COLLECTION_TEAM_ID, collectionTeamId )
                        dao.writableDatabase.insert(DAO.TABLE_LOCATION__COLLECTION_TEAM, null, values)
                    }
                    cursor.close()
                }
            }
        }
    }

    fun putTeam(collectionTeam: CollectionTeam, values: ContentValues)
    {
        collectionTeam.id?.let { id ->
            Log.d( "xxx", "existing team id = ${id}")
            values.put( DAO.COLUMN_ID, id )
        }

        values.put( DAO.COLUMN_UUID, collectionTeam.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, collectionTeam.creationDate )
        values.put( DAO.COLUMN_ENUM_AREA_ID, collectionTeam.enumAreaId )
        values.put( DAO.COLUMN_STUDY_ID, collectionTeam.studyId )
        values.put( DAO.COLUMN_COLLECTION_TEAM_NAME, collectionTeam.name )
    }

    fun exists(collectionTeam: CollectionTeam): Boolean
    {
        for (existingCollectionTeam in getCollectionTeams())
        {
            if (existingCollectionTeam.uuid == collectionTeam.uuid)
            {
                return true
            }
        }

        return false
    }

    @SuppressLint("Range")
    private fun createTeam(cursor: Cursor): CollectionTeam
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val enum_area_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_ID))
        val study_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_COLLECTION_TEAM_NAME))

        val collectionTeam = CollectionTeam(id, uuid, creationDate, enum_area_id, study_id, name, ArrayList<LatLon>(), ArrayList<Location>())

        collectionTeam.polygon = DAO.latLonDAO.getLatLonsWithCollectionTeamId( id )
        collectionTeam.locations = DAO.locationDAO.getLocations( collectionTeam )

        return collectionTeam
    }

    fun updateTeam( collectionTeam: CollectionTeam)
    {
        collectionTeam.id?.let { id ->
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putTeam( collectionTeam, values )

            dao.writableDatabase.update(DAO.TABLE_COLLECTION_TEAM, values, whereClause, args )
        }
    }

    fun getTeam( id: Int ): CollectionTeam?
    {
        var collectionTeam: CollectionTeam? = null
        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_TEAM} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            collectionTeam = createTeam( cursor )
        }

        cursor.close()

        return collectionTeam
    }

    fun getCollectionTeams( study: Study ): ArrayList<CollectionTeam>
    {
        val collectionTeam = ArrayList<CollectionTeam>()

        study.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_TEAM} WHERE ${DAO.COLUMN_STUDY_ID} = $id"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                collectionTeam.add( createTeam( cursor ))
            }

            cursor.close()
        }

        return collectionTeam
    }

    fun getCollectionTeams(): ArrayList<CollectionTeam>
    {
        val collectionTeam = ArrayList<CollectionTeam>()
        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_TEAM}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            collectionTeam.add( createTeam( cursor ))
        }

        cursor.close()

        return collectionTeam
    }

    fun deleteTeam(collectionTeam: CollectionTeam)
    {
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(collectionTeam.id.toString())

        dao.writableDatabase.delete(DAO.TABLE_COLLECTION_TEAM, whereClause, args)
    }
}