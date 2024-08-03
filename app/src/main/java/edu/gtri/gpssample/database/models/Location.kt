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
    var timeZone: Int,
    var distance : Double,      // not stored in DB!
    var distanceUnits: String,  // not stored in DB!
    var type : LocationType,
    var gpsAccuracy : Int,
    var latitude : Double,
    var longitude : Double,
    var altitude : Double,
    var isLandmark: Boolean,
    var description: String,
    var imageData: String,
    var isMultiFamily : Boolean,
    var properties : String,
    var enumerationItems: ArrayList<EnumerationItem>)
{
    constructor( timeZone: Int, type: LocationType, gpsAccuracy: Int, latitude: Double, longitude: Double, altitude: Double, isLandmark: Boolean, description: String, properties: String ) :
            this( UUID.randomUUID().toString(), Date().time, timeZone, 0.0, "", type, gpsAccuracy, latitude, longitude, altitude, isLandmark, description,"", false, properties, ArrayList<EnumerationItem>())

    fun equals( other: Location ) : Boolean
    {
        if (this.uuid == other.uuid &&
            this.creationDate == other.creationDate &&
            this.uuid == other.uuid &&
            this.type == other.type &&
            this.gpsAccuracy == other.gpsAccuracy &&
            this.latitude == other.latitude &&
            this.longitude == other.longitude &&
            this.altitude == other.altitude &&
            this.isLandmark == other.isLandmark &&
            this.description == other.description &&
            this.imageData == other.imageData &&
            this.isMultiFamily == other.isMultiFamily &&
            this.properties == other.properties)
        {
            return true
        }

        return false
    }

    fun doesNotEqual( location: Location ): Boolean
    {
        return !this.equals( location )
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