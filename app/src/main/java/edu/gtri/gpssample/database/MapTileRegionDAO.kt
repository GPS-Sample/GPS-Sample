package edu.gtri.gpssample.database

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import edu.gtri.gpssample.database.models.*

class MapTileRegionDAO(private var dao: DAO)
{
    fun createOrUpdateMapTileRegion( mapTileRegion: MapTileRegion, config: Config ) : MapTileRegion?
    {
        if (exists( mapTileRegion ))
        {
            updateMapTileRegion( mapTileRegion, config )
        }
        else
        {
            val values = ContentValues()
            putMapTileRegion( mapTileRegion, config, values )
            mapTileRegion.id = dao.writableDatabase.insert(DAO.TABLE_MAP_TILE_REGION, null, values).toInt()
        }

        return mapTileRegion
    }

    private fun putMapTileRegion( mapTileRegion: MapTileRegion, config: Config, values: ContentValues)
    {
        mapTileRegion.id?.let { id ->
            values.put( DAO.COLUMN_ID, id )
        }

        config.id?.let { id ->
            values.put( DAO.COLUMN_CONFIG_ID, id )
        }

        values.put( DAO.COLUMN_NORTH_EAST_LAT, mapTileRegion.northEast.latitude )
        values.put( DAO.COLUMN_NORTH_EAST_LON, mapTileRegion.northEast.longitude )
        values.put( DAO.COLUMN_SOUTH_WEST_LAT, mapTileRegion.southWest.latitude )
        values.put( DAO.COLUMN_SOUTH_WEST_LON, mapTileRegion.southWest.longitude )
    }

    fun exists( mapTileRegion: MapTileRegion ): Boolean
    {
        mapTileRegion.id?.let { id ->
            getMapTileRegion( id )?.let {
                return true
            } ?: return false
        } ?: return false
    }

    @SuppressLint("Range")
    private fun createMapTileRegion(cursor: Cursor): MapTileRegion
    {
        val id = cursor.getInt(cursor.getColumnIndex(DAO.COLUMN_ID))
        val ne_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_NORTH_EAST_LAT))
        val ne_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_NORTH_EAST_LON))
        val sw_lat = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_SOUTH_WEST_LAT))
        val sw_lon = cursor.getDouble(cursor.getColumnIndex(DAO.COLUMN_SOUTH_WEST_LON))

        return MapTileRegion( id, LatLon( ne_lat, ne_lon ), LatLon( sw_lat, sw_lon ))
    }

    fun updateMapTileRegion( mapTileRegion: MapTileRegion, config: Config )
    {
        mapTileRegion.id?.let{ id ->
            Log.d( "xxx", "update mapTileRegion id ${id}")

            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args: Array<String> = arrayOf(id.toString())
            val values = ContentValues()

            putMapTileRegion( mapTileRegion, config, values )

            dao.writableDatabase.update(DAO.TABLE_MAP_TILE_REGION, values, whereClause, args )
        }
    }

    fun getMapTileRegion( id : Int ): MapTileRegion?
    {
        var mapTileRegion: MapTileRegion? = null
        val query = "SELECT * FROM ${DAO.TABLE_MAP_TILE_REGION} where id=${id}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        if (cursor.count > 0)
        {
            cursor.moveToNext()
            mapTileRegion = createMapTileRegion( cursor )
        }

        cursor.close()

        return mapTileRegion
    }

    fun getMapTileRegions( config: Config ): ArrayList<MapTileRegion>
    {
        val mapTileRegions = ArrayList<MapTileRegion>()

        config.id?.let { id ->
            val query = "SELECT * FROM ${DAO.TABLE_MAP_TILE_REGION} where ${DAO.COLUMN_CONFIG_ID}=${id}"
            val cursor = dao.writableDatabase.rawQuery(query, null)

            while (cursor.moveToNext())
            {
                val mapTileRegion = createMapTileRegion(cursor)

                mapTileRegions.add( mapTileRegion )
            }

            cursor.close()
        }

        return mapTileRegions
    }

    fun getMapTileRegions(): ArrayList<MapTileRegion>
    {
        val mapTileRegions = ArrayList<MapTileRegion>()
        val query = "SELECT * FROM ${DAO.TABLE_MAP_TILE_REGION}"
        val cursor = dao.writableDatabase.rawQuery(query, null)

        while (cursor.moveToNext())
        {
            mapTileRegions.add( createMapTileRegion( cursor ))
        }

        cursor.close()

        return mapTileRegions
    }

    fun delete( mapTileRegion: MapTileRegion )
    {
        mapTileRegion.id?.let { id ->
            val whereClause = "${DAO.COLUMN_ID} = ?"
            val args = arrayOf(id.toString())

            dao.writableDatabase.delete(DAO.TABLE_MAP_TILE_REGION, whereClause, args)
        }
    }
}
