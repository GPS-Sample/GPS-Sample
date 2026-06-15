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

class MapTileRegionDAO(private var dao: DAO)
{
    fun createOrUpdateMapTileRegion( mapTileRegion: MapTileRegion, enumArea: EnumArea ) : MapTileRegion?
    {
        val values = ContentValues()
        putMapTileRegion( mapTileRegion, enumArea, values )

        dao.upsert( DAO.TABLE_MAP_TILE_REGION, values )

        return mapTileRegion
    }

    private fun putMapTileRegion( mapTileRegion: MapTileRegion, enumArea: EnumArea, values: ContentValues)
    {
        values.put( DAO.COLUMN_UUID, mapTileRegion.uuid )
        values.put( DAO.COLUMN_CREATION_DATE, mapTileRegion.creationDate )
        values.put( DAO.COLUMN_VERSION, mapTileRegion.version )
        values.put( DAO.COLUMN_ENUM_AREA_UUID, enumArea.uuid )
        values.put( DAO.COLUMN_NORTH_EAST_LAT, mapTileRegion.northEast.latitude )
        values.put( DAO.COLUMN_NORTH_EAST_LON, mapTileRegion.northEast.longitude )
        values.put( DAO.COLUMN_SOUTH_WEST_LAT, mapTileRegion.southWest.latitude )
        values.put( DAO.COLUMN_SOUTH_WEST_LON, mapTileRegion.southWest.longitude )
    }

    @SuppressLint("Range")
    private fun buildMapTileRegion(cursor: Cursor): MapTileRegion
    {
        val uuid = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_UUID))
        val creationDate = cursor.getLong(cursor.getColumnIndex(DAO.COLUMN_CREATION_DATE))
        val version = cursor.getString(cursor.getColumnIndex(DAO.COLUMN_VERSION))
        val ne_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_NORTH_EAST_LAT))
        val ne_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_NORTH_EAST_LON))
        val sw_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_SOUTH_WEST_LAT))
        val sw_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_SOUTH_WEST_LON))

        return MapTileRegion( uuid, creationDate, version, LatLon( 0, ne_lat, ne_lon ), LatLon( 0, sw_lat, sw_lon ))
    }

    fun getMapTileRegion( enumArea: EnumArea ): MapTileRegion?
    {
        var mapTileRegion: MapTileRegion? = null

        val query = "SELECT * FROM ${DAO.TABLE_MAP_TILE_REGION} where ${DAO.COLUMN_ENUM_AREA_UUID}='${enumArea.uuid}'"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            mapTileRegion = buildMapTileRegion( cursor )
        }

        cursor.close()

        return mapTileRegion
    }

    fun delete( mapTileRegion: MapTileRegion )
    {
        val whereClause = "${DAO.COLUMN_UUID} = ?"
        val args = arrayOf(mapTileRegion.uuid)

        dao.writableDatabase.delete(DAO.TABLE_MAP_TILE_REGION, whereClause, args)
    }
}
