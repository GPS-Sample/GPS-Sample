package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@kotlinx.serialization.Serializable
data class CollectionTeam(
    var id : Int? = null,
    var creationDate: Long,
    var enumAreaId: Int,
    var studyId: Int,
    var name: String,
    var polygon: ArrayList<LatLon>,
    var locations: ArrayList<Location>
)
{
    constructor( enumAreaId: Int, studyId: Int,  name: String, polygon: ArrayList<LatLon>, locations: ArrayList<Location>)
            : this(null, Date().time, enumAreaId, studyId, name, polygon, locations )

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