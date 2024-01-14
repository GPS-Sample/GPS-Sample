package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@kotlinx.serialization.Serializable
data class EnumerationTeam(
    var id : Int? = null,
    var uuid : String,
    var creationDate: Long,
    var enumerAreaId: Int,
    var name: String,
    var polygon: ArrayList<LatLon>,
    var locations: ArrayList<Location>)
{
    constructor( enumAreaId: Int,  name: String, polygon: ArrayList<LatLon>, locations: ArrayList<Location> )
            : this(null, UUID.randomUUID().toString(), Date().time, enumAreaId, name, polygon, locations )

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : EnumerationTeam
        {
            return Json.decodeFromString<EnumerationTeam>( message )
        }
    }
}