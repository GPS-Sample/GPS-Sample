package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class LatLon (
    var id: Int?,
    var uuid : String,
    var latitude: Double,
    var longitude: Double)
{
    constructor( latitude: Double, longitude: Double ) : this( null, UUID.randomUUID().toString(), latitude, longitude)

    fun toLatLng() : LatLng
    {
        return LatLng( latitude, longitude )
    }
}