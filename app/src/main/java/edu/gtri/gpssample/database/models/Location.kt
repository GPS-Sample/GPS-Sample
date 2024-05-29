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
    var uuid : String,
    var creationDate: Long,
    var type : LocationType,
    var gpsAccuracy : Int,
    var latitude : Double,
    var longitude : Double,
    var altitude : Double,
    var isLandmark: Boolean,
    var description: String,
    var imageData: String,
    var isMultiFamily : Boolean?,
    var enumerationItems: ArrayList<EnumerationItem>)
{
    constructor(type: LocationType, gpsAccuracy: Int, latitude: Double, longitude: Double, altitude: Double, isLandmark: Boolean, description: String ) :
            this( UUID.randomUUID().toString(), Date().time, type, gpsAccuracy, latitude, longitude, altitude, isLandmark, description,"", null, ArrayList<EnumerationItem>())

    fun equals( location: Location ) : Boolean
    {
        if (this.uuid == location.uuid &&
            this.creationDate == location.creationDate &&
            this.uuid == location.uuid &&
            this.type == location.type &&
            this.gpsAccuracy == location.gpsAccuracy &&
            this.latitude == location.latitude &&
            this.longitude == location.longitude &&
            this.altitude == location.altitude &&
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