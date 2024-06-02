package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class MapTileRegion (
    var uuid: String,
    var northEast: LatLon,
    var southWest: LatLon)
{
    constructor( northEast: LatLon, southWest: LatLon ) : this( UUID.randomUUID().toString(), northEast, southWest )

    fun equals( other: MapTileRegion ): Boolean
    {
        if (this.uuid == other.uuid &&
            this.northEast.latitude == other.northEast.latitude &&
            this.northEast.longitude == other.northEast.longitude &&
            this.southWest.latitude == other.southWest.latitude &&
            this.southWest.longitude == other.southWest.longitude )
        {
            return true
        }

        return false
    }

    fun doesNotEqual( mapTileRegion: MapTileRegion ): Boolean
    {
        return !this.equals( mapTileRegion )
    }
}