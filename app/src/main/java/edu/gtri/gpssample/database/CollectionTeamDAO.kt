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
import edu.gtri.gpssample.database.models.*

class CollectionTeamDAO(private var dao: DAO)
{
    fun createOrUpdateCollectionTeam(collectionTeam: CollectionTeam) : CollectionTeam?
    {
        val existingCollectionTeam = getCollectionTeam( collectionTeam.uuid )

        if (existingCollectionTeam != null)
        {
            if (collectionTeam.doesNotEqual( existingCollectionTeam ))
            {
                updateTeam( collectionTeam )
                Log.d( "xxx", "Updated CollectionTeam with ID ${collectionTeam.uuid}" )
            }
        }
        else
        {
            val values = ContentValues()
            putTeam( collectionTeam, values )
            if (dao.writableDatabase.insert(DAO.TABLE_COLLECTION_TEAM, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "Created CollectionTeam with ID = ${collectionTeam.uuid}")
        }

        for (latLon in collectionTeam.polygon)
        {
            DAO.latLonDAO.createOrUpdateLatLon(latLon,null, null)
        }

        updateConnectorTable( collectionTeam )

        return collectionTeam
    }

    private fun updateConnectorTable( collectionTeam: CollectionTeam )
    {
        for (latLon in collectionTeam.polygon)
        {
            val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_COLLECTION_TEAM__LAT_LON} WHERE ${DAO.COLUMN_LAT_LON_UUID} = '${latLon.uuid}' AND ${DAO.COLUMN_COLLECTION_TEAM_UUID} = '${collectionTeam.uuid}'"
            val cursor = dao.writableDatabase.rawQuery(query, null)
            if (cursor.count == 0)
            {
                val values = ContentValues()
                values.put( DAO.COLUMN_LAT_LON_UUID, latLon.uuid )
                values.put( DAO.COLUMN_COLLECTION_TEAM_UUID, collectionTeam.uuid )
                dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_COLLECTION_TEAM__LAT_LON, null, values)
            }
            cursor.close()
        }

        for (locationUuid in collectionTeam.locationUuids)
        {
            val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_LOCATION__COLLECTION_TEAM} WHERE ${DAO.COLUMN_LOCATION_UUID} = '${locationUuid}' AND ${DAO.COLUMN_COLLECTION_TEAM_UUID} = '${collectionTeam.uuid}'"
            val cursor = dao.writableDatabase.rawQuery(query, null)
            if (cursor.count == 0)
            {
                val values = ContentValues()
                values.put( DAO.COLUMN_LOCATION_UUID, locationUuid )
                values.put( DAO.COLUMN_COLLECTION_TEAM_UUID, collectionTeam.uuid )
                dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_LOCATION__COLLECTION_TEAM, null, values)
            }
            cursor.close()
        }
    }

    fun putTeam(collectionTeam: CollectionTeam, values: ContentValues)
    {
        values.put( DAO.COLUMN_UUID, collectionTeam.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, collectionTeam.creationDate )
        values.put( DAO.COLUMN_ENUM_AREA_UUID, collectionTeam.enumAreaUuid )
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
    private fun buildTeam(cursor: Cursor): CollectionTeam
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val enum_area_uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_COLLECTION_TEAM_NAME))

        val collectionTeam = CollectionTeam(uuid, creationDate, enum_area_uuid, name, ArrayList<LatLon>(), ArrayList<String>())

        collectionTeam.polygon = DAO.latLonDAO.getLatLonsWithCollectionTeamId( collectionTeam.uuid )
        collectionTeam.locationUuids = DAO.locationDAO.getCollectionTeamLocationUuids( collectionTeam )

        return collectionTeam
    }

    fun updateTeam( collectionTeam: CollectionTeam)
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(collectionTeam.uuid)
        val values = ContentValues()

        putTeam( collectionTeam, values )

        dao.writableDatabase.update(DAO.TABLE_COLLECTION_TEAM, values, whereClause, args )
    }

    fun getCollectionTeam( uuid: String ): CollectionTeam?
    {
        var collectionTeam: CollectionTeam? = null
        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_TEAM} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            collectionTeam = buildTeam( cursor )
        }

        cursor.close()

        return collectionTeam
    }

    fun getCollectionTeams( enumArea: EnumArea ): ArrayList<CollectionTeam>
    {
        val collectionTeam = ArrayList<CollectionTeam>()

        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_TEAM} WHERE ${DAO.COLUMN_ENUM_AREA_UUID} = '${enumArea.uuid}' ORDER BY ${DAO.COLUMN_CREATION_DATE}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            collectionTeam.add( buildTeam( cursor ))
        }

        cursor.close()

        return collectionTeam
    }

    fun getCollectionTeams(): ArrayList<CollectionTeam>
    {
        val collectionTeam = ArrayList<CollectionTeam>()
        val query = "SELECT * FROM ${DAO.TABLE_COLLECTION_TEAM}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            collectionTeam.add( buildTeam( cursor ))
        }

        cursor.close()

        return collectionTeam
    }

    fun deleteTeam(collectionTeam: CollectionTeam)
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(collectionTeam.uuid)

        dao.writableDatabase.delete(DAO.TABLE_COLLECTION_TEAM, whereClause, args)
    }
}