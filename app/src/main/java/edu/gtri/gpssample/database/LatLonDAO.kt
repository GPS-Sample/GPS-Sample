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
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import edu.gtri.gpssample.database.models.*

class LatLonDAO(private var dao: DAO)
{
    fun createOrUpdateLatLon( latLon: LatLon, enumArea : EnumArea?, version: String ) : LatLon?
    {
        latLon.version = version

        val values = ContentValues()
        putLatLon( latLon, values )

        dao.upsert( DAO.TABLE_LAT_LON, values )

        enumArea?.let { enumArea ->
            val values = ContentValues()
            putLatLonEnumArea( latLon.uuid, enumArea.uuid, values )
            dao.writableDatabase.insertWithOnConflict(DAO.CONNECTOR_TABLE_ENUM_AREA__LAT_LON, null, values, SQLiteDatabase.CONFLICT_IGNORE )
        }

        return latLon
    }

    private fun putLatLonEnumArea(llID : String, enumAreaUuid: String, values : ContentValues)
    {
        values.put( DAO.COLUMN_LAT_LON_UUID, llID )
        values.put( DAO.COLUMN_ENUM_AREA_UUID, enumAreaUuid )
    }

    private fun putLatLon(latLon: LatLon, values: ContentValues)
    {
        values.put( DAO.COLUMN_UUID, latLon.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, latLon.creationDate )
        values.put( DAO.COLUMN_VERSION, latLon.version )
        values.put( DAO.COLUMN_LAT, latLon.latitude )
        values.put( DAO.COLUMN_LON, latLon.longitude )
    }

    @SuppressLint("Range")
    private fun buildLatLon(cursor: Cursor): LatLon
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LAT))
        val lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_LON))

        return LatLon( uuid, creationDate, lat, lon, version )
    }

    fun getLatLonsWithEnumAreaUuid( enumAreaUuid: String ): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()

        val query = "SELECT latlon.*, conn.${DAO.COLUMN_LAT_LON_UUID}, conn.${DAO.COLUMN_ENUM_AREA_UUID} FROM ${DAO.TABLE_LAT_LON} AS latlon, " +
                "${DAO.CONNECTOR_TABLE_ENUM_AREA__LAT_LON} AS conn WHERE latlon.${DAO.COLUMN_UUID} = conn.${DAO.COLUMN_LAT_LON_UUID} AND conn.${DAO.COLUMN_ENUM_AREA_UUID} = '${enumAreaUuid}' " +
                "ORDER BY ${DAO.COLUMN_CREATION_DATE} ASC"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = buildLatLon(cursor)

            latLons.add( latlon )
        }

        cursor.close()

        return latLons
    }

    fun getLatLonsWithEnumerationTeamId( teamUuid: String ): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()

        val query = "SELECT latlon.*, conn.${DAO.COLUMN_LAT_LON_UUID}, conn.${DAO.COLUMN_ENUMERATION_TEAM_UUID} FROM ${DAO.TABLE_LAT_LON} AS latlon, " +
                "${DAO.CONNECTOR_TABLE_ENUMERATION_TEAM__LAT_LON} AS conn WHERE latlon.${DAO.COLUMN_UUID} = conn.${DAO.COLUMN_LAT_LON_UUID} AND conn.${DAO.COLUMN_ENUMERATION_TEAM_UUID} = '${teamUuid}'" +
                "ORDER BY ${DAO.COLUMN_CREATION_DATE} ASC"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = buildLatLon(cursor)

            latLons.add( latlon )
        }

        cursor.close()

        return latLons
    }

    fun getLatLonsWithCollectionTeamId( teamUuid: String ): ArrayList<LatLon>
    {
        val latLons = ArrayList<LatLon>()

        val query = "SELECT latlon.*, conn.${DAO.COLUMN_LAT_LON_UUID}, conn.${DAO.COLUMN_COLLECTION_TEAM_UUID} FROM ${DAO.TABLE_LAT_LON} AS latlon, " +
                "${DAO.CONNECTOR_TABLE_COLLECTION_TEAM__LAT_LON} AS conn WHERE latlon.${DAO.COLUMN_UUID} = conn.${DAO.COLUMN_LAT_LON_UUID} AND conn.${DAO.COLUMN_COLLECTION_TEAM_UUID} = '${teamUuid}'" +
                "ORDER BY ${DAO.COLUMN_CREATION_DATE} ASC"

        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            val latlon = buildLatLon(cursor)

            latLons.add( latlon )
        }

        cursor.close()

        return latLons
    }

    fun delete( latLon: LatLon )
    {
        var whereClause = "${DAO.COLUMN_LAT_LON_UUID} = ?"
        val args = arrayOf(latLon.uuid)

        dao.writableDatabase.delete(DAO.CONNECTOR_TABLE_ENUM_AREA__LAT_LON, whereClause, args)
        dao.writableDatabase.delete(DAO.CONNECTOR_TABLE_COLLECTION_TEAM__LAT_LON, whereClause, args)
        dao.writableDatabase.delete(DAO.CONNECTOR_TABLE_ENUMERATION_TEAM__LAT_LON, whereClause, args)

        whereClause = "${DAO.COLUMN_UUID} = ?"

        dao.writableDatabase.delete(DAO.TABLE_LAT_LON, whereClause, args)
    }
}
