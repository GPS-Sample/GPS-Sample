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

class EnumerationTeamDAO(private var dao: DAO)
{
    fun createOrUpdateEnumerationTeam(enumerationTeam: EnumerationTeam) : EnumerationTeam?
    {
        val existingEnumerationTeam = getEnumerationTeam( enumerationTeam.uuid )

        if (existingEnumerationTeam != null)
        {
            if (enumerationTeam.doesNotEqual( existingEnumerationTeam ))
            {
                updateTeam( enumerationTeam )
                Log.d( "xxx", "Updated EnumerationTeam with ID ${enumerationTeam.uuid}")
            }
        }
        else
        {
            val values = ContentValues()
            putTeam( enumerationTeam, values )
            if (dao.writableDatabase.insert(DAO.TABLE_ENUMERATION_TEAM, null, values) < 0)
            {
                return null
            }
            Log.d( "xxx", "Created EnumerationTeam with ID ${enumerationTeam.uuid}")
        }

        for (latLon in enumerationTeam.polygon)
        {
            DAO.latLonDAO.createOrUpdateLatLon(latLon,null, null)
        }

        updateConnectorTable( enumerationTeam )

        return enumerationTeam
    }

    fun updateConnectorTable( enumerationTeam: EnumerationTeam )
    {
        for (latLon in enumerationTeam.polygon)
        {
            val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_ENUMERATION_TEAM__LAT_LON} WHERE ${DAO.COLUMN_LAT_LON_UUID} = '${latLon.uuid}' AND ${DAO.COLUMN_ENUMERATION_TEAM_UUID} = '${enumerationTeam.uuid}'"
            val cursor = dao.writableDatabase.rawQuery(query, null)
            if (cursor.count == 0)
            {
                val values = ContentValues()
                values.put( DAO.COLUMN_LAT_LON_UUID, latLon.uuid )
                values.put( DAO.COLUMN_ENUMERATION_TEAM_UUID, enumerationTeam.uuid )
                dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_ENUMERATION_TEAM__LAT_LON, null, values)
            }
            cursor.close()
        }

        for (locationUuid in enumerationTeam.locationUuids)
        {
            val query = "SELECT * FROM ${DAO.CONNECTOR_TABLE_LOCATION__ENUMERATION_TEAM} WHERE ${DAO.COLUMN_LOCATION_UUID} = '${locationUuid}' AND ${DAO.COLUMN_ENUMERATION_TEAM_UUID} = '${enumerationTeam.uuid}'"
            val cursor = dao.writableDatabase.rawQuery(query, null)
            if (cursor.count == 0)
            {
                val values = ContentValues()
                values.put( DAO.COLUMN_LOCATION_UUID, locationUuid )
                values.put( DAO.COLUMN_ENUMERATION_TEAM_UUID, enumerationTeam.uuid )
                dao.writableDatabase.insert(DAO.CONNECTOR_TABLE_LOCATION__ENUMERATION_TEAM, null, values)
            }
            cursor.close()
        }
    }

    fun putTeam(enumerationTeam: EnumerationTeam, values: ContentValues )
    {
        values.put( DAO.COLUMN_UUID, enumerationTeam.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, enumerationTeam.creationDate )
        values.put( DAO.COLUMN_ENUM_AREA_UUID, enumerationTeam.enumAreaUuid )
        values.put( DAO.COLUMN_ENUMERATION_TEAM_NAME, enumerationTeam.name )
        values.put( DAO.COLUMN_ENUMERATION_TEAM_MBTILESPATH, enumerationTeam.mbTilesPath )
        values.put( DAO.COLUMN_ENUMERATION_TEAM_MBTILESSIZE, enumerationTeam.mbTilesSize )
    }

    fun exists( enumerationTeam: EnumerationTeam ): Boolean
    {
        getEnumerationTeam( enumerationTeam.uuid )?.let {
            return true
        } ?: return false
    }

    @SuppressLint("Range")
    private fun buildTeam(cursor: Cursor): EnumerationTeam
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val enumAreaUuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUM_AREA_UUID))
        val name = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_TEAM_NAME))
        val mbTilesPath = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_TEAM_MBTILESPATH))
        val mbTilesSize = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_ENUMERATION_TEAM_MBTILESSIZE))

        val enumerationTeam = EnumerationTeam(uuid, creationDate, enumAreaUuid, name, mbTilesPath, mbTilesSize, ArrayList<LatLon>(), ArrayList<String>())

        enumerationTeam.polygon = DAO.latLonDAO.getLatLonsWithEnumerationTeamId( enumerationTeam.uuid )
        enumerationTeam.locationUuids = DAO.locationDAO.getEnumerationTeamLocationUuids( enumerationTeam )

        return enumerationTeam
    }

    fun updateTeam( enumerationTeam: EnumerationTeam )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args: Array<String> = arrayOf(enumerationTeam.uuid)
        val values = ContentValues()

        putTeam( enumerationTeam, values )

        dao.writableDatabase.update(DAO.TABLE_ENUMERATION_TEAM, values, whereClause, args )
    }

    fun getEnumerationTeam( uuid: String ): EnumerationTeam?
    {
        var enumerationTeam: EnumerationTeam? = null
        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM} WHERE ${DAO.COLUMN_UUID} = '$uuid'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            enumerationTeam = buildTeam( cursor )
        }

        cursor.close()

        return enumerationTeam
    }

    fun getEnumerationTeams( enumArea: EnumArea ): ArrayList<EnumerationTeam>
    {
        val enumerationTeams = ArrayList<EnumerationTeam>()

        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM} WHERE ${DAO.COLUMN_ENUM_AREA_UUID} = '${enumArea.uuid}' ORDER BY ${DAO.COLUMN_CREATION_DATE}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumerationTeams.add( buildTeam( cursor ))
        }

        cursor.close()

        return enumerationTeams
    }

    fun getEnumerationTeams(): ArrayList<EnumerationTeam>
    {
        val enumerationTeams = ArrayList<EnumerationTeam>()
        val query = "SELECT * FROM ${DAO.TABLE_ENUMERATION_TEAM}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            enumerationTeams.add( buildTeam( cursor ))
        }

        cursor.close()

        return enumerationTeams
    }

    fun deleteTeam(enumerationTeam: EnumerationTeam )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(enumerationTeam.uuid)

        dao.writableDatabase.delete(DAO.TABLE_ENUMERATION_TEAM, whereClause, args)
    }
}