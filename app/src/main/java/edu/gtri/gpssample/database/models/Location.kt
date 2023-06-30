package edu.gtri.gpssample.database.models

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
    var enumAreaId : Int,
    var enumerationTeamId : Int,
    var collectionTeamId : Int,
    var latitude : Double,
    var longitude : Double,
    var isLandmark: Boolean,
    var enumerationItems: ArrayList<EnumerationItem>)
{
    constructor( enumAreaId: Int, latitude: Double, longitude: Double, isLandmark: Boolean ) :
            this( null, Date().time, UUID.randomUUID().toString(), enumAreaId, -1, -1, latitude, longitude, isLandmark, ArrayList<EnumerationItem>())

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : EnumData
        {
            return Json.decodeFromString<EnumData>( string )
        }
    }
}