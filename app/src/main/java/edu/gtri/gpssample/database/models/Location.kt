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
    var latitude : Double,
    var longitude : Double,
    var isLandmark: Boolean,
    var description: String,
    var imageData: String,
    var imageFileName: String,
    var isMultiFamily : Boolean?,
    var enumerationItems: ArrayList<EnumerationItem>)
{
    constructor(type: LocationType, latitude: Double, longitude: Double, isLandmark: Boolean, description: String ) :
            this( null, Date().time, UUID.randomUUID().toString(), type, latitude, longitude, isLandmark, description, "", "", null, ArrayList<EnumerationItem>())

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