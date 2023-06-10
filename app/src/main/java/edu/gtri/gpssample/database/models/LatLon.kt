package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable

@Serializable
data class LatLon (
    var id: Int?,
    var latitude: Double,
    var longitude: Double,
    var enumAreaId: Int,
    var teamId: Int)
{
    constructor( latitude: Double, longitude: Double ) : this( null, latitude, longitude, -1, -1 )

    fun toLatLng() : LatLng
    {
        return LatLng( latitude, longitude )
    }
}