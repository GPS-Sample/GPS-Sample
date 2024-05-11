package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class LatLon (
    var uuid : String,
    var index : Int,
    var latitude: Double,
    var longitude: Double)
{
    constructor( index: Int, latitude: Double, longitude: Double ) : this(UUID.randomUUID().toString(), index, latitude, longitude)

    fun toLatLng() : LatLng
    {
        return LatLng( latitude, longitude )
    }
}