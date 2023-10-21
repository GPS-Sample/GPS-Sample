package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.CollectionTeam
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

        updateConnectorTable( collectionTeam )

        return collectionTeam
    }

    fun updateConnectorTable( collectionTeam: CollectionTeam)
    {
        collectionTeam.id?.let { collectionTeamId ->
            for (location in collectionTeam.locations)
            {
                location.id?.let { locationId ->
                    val values = ContentValues()
                    values.put( DAO.COLUMN_LOCATION_ID, locationId )
                    values.put( DAO.COLUMN_COLLECTION_TEAM_ID, collectionTeamId )
                    dao.writableDatabase.insert(DAO.TABLE_LOCATION__COLLECTION_TEAM, null, values)
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

        values.put( DAO.COLUMN_CREATION_DATE, collectionTeam.creationDate )
        values.put( DAO.COLUMN_STUDY_ID, collectionTeam.studyId )
        values.put( DAO.COLUMN_COLLECTION_TEAM_NAME, collectionTeam.name )
    }

    fun exists(collectionTeam: CollectionTeam): Boolean
    {
        collectionTeam.id?.let { id ->
            getTeam( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    @SuppressLint("Range")
    private fun createTeam(cursor: Cursor): CollectionTeam
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val study_id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_STUDY_ID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_COLLECTION_TEAM_NAME))

        val collectionTeam = CollectionTeam(id, creationDate, study_id, name, ArrayList<Location>())

        collectionTeam.locations = DAO.locationDAO.getLocations( collectionTeam )

        return collectionTeam
    }

    fun updateTeam( collectionTeam: CollectionTeam)
    {
        val db = dao.writableDatabase

        collectionTeam.id?.let { id ->
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putTeam( collectionTeam, values )

            db.update(DAO.TABLE_COLLECTION_TEAM, values, whereClause, args )
        }

        db.close()
    }

    fun getTeam( id: Int ): CollectionTeam?
    {
        var collectionTeam: CollectionTeam? = null
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_TEAM} WHERE ${DAO.COLUMN_ID} = $id"
        val cursor = db.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()

            collectionTeam = createTeam( cursor )
        }

        cursor.close()
        db.close()

        return collectionTeam
    }

    fun getCollectionTeams( study: Study ): ArrayList<CollectionTeam>
    {
        val collectionTeam = ArrayList<CollectionTeam>()
        val db = dao.writableDatabase

        study.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_TEAM} WHERE ${DAO.COLUMN_STUDY_ID} = $id"
            val cursor = db.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                collectionTeam.add( createTeam( cursor ))
            }

            cursor.close()
        }

        db.close()

        return collectionTeam
    }

    fun getCollectionTeams(): ArrayList<CollectionTeam>
    {
        val collectionTeam = ArrayList<CollectionTeam>()
        val db = dao.writableDatabase
        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_TEAM}"
        val cursor = db.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            collectionTeam.add( createTeam( cursor ))
        }

        cursor.close()
        db.close()

        return collectionTeam
    }

    fun deleteTeam(collectionTeam: CollectionTeam)
    {
        val db = dao.writableDatabase
        val whereClause = "${DAO.COLUMN_ID} = ?"
        val args = arrayOf(collectionTeam.id.toString())

        db.delete(DAO.TABLE_COLLECTION_TEAM, whereClause, args)
        db.close()
    }
}