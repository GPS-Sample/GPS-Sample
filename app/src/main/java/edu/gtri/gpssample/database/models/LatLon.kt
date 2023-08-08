package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable

@Serializable
data class LatLon (
    var id: Int?,
    var latitude: Double,
    var longitude: Double)
{
    constructor( latitude: Double, longitude: Double ) : this( null, latitude, longitude)

    fun toLatLng() : LatLng
    {
        return LatLng( latitude, longitude )
    }
}