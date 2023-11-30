package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable

@Serializable
data class MapTileRegion (
    var id: Int?,
    var northEast: LatLon,
    var southWest: LatLon)
{
    constructor( northEast: LatLon, southWest: LatLon ) : this( null, northEast, southWest )
}