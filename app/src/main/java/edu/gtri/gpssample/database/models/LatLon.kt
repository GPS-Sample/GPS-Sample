package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class LatLon (
    var uuid : String,
    var creationDate : Long,
    var latitude: Double,
    var longitude: Double)
{
    constructor( creationDate: Long, latitude: Double, longitude: Double ) : this(UUID.randomUUID().toString(), creationDate, latitude, longitude)

    fun toLatLng() : LatLng
    {
        return LatLng( latitude, longitude )
    }

    fun equals( latLon: LatLon ) : Boolean
    {
        if (this.uuid == latLon.uuid &&
            this.creationDate == latLon.creationDate &&
            this.latitude == latLon.latitude &&
            this.longitude == latLon.longitude)
        {
            return true
        }

        return false
    }
}