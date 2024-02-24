package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.LocationType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class Location(
    var id : Int? = null,
    var creationDate: Long,
    var uuid : String,
    var type : LocationType,
    var gpsAccuracy : Int,
    var latitude : Double,
    var longitude : Double,
    var isLandmark: Boolean,
    var description: String,
    var imageData: String,
    var isMultiFamily : Boolean?,
    var enumerationItems: ArrayList<EnumerationItem>)
{
    constructor(type: LocationType, gpsAccuracy: Int, latitude: Double, longitude: Double, isLandmark: Boolean, description: String ) :
            this( null, Date().time, UUID.randomUUID().toString(), type, gpsAccuracy, latitude, longitude, isLandmark, description,"", null, ArrayList<EnumerationItem>())

    fun equals( location: Location ) : Boolean
    {
        if (this.id == location.id &&
            this.creationDate == location.creationDate &&
            this.uuid == location.uuid &&
            this.type == location.type &&
            this.gpsAccuracy == location.gpsAccuracy &&
            this.latitude == location.latitude &&
            this.longitude == location.longitude &&
            this.isLandmark == location.isLandmark &&
            this.description == location.description &&
            this.imageData == location.imageData &&
            this.isMultiFamily == location.isMultiFamily)
        {
            return true
        }

        return false
    }

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : Location
        {
            return Json.decodeFromString<Location>( string )
        }
    }
}