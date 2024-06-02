package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@kotlinx.serialization.Serializable
data class CollectionTeam(
    var uuid : String,
    var creationDate: Long,
    var enumAreaUuid: String,
    var name: String,
    var polygon: ArrayList<LatLon>,
    var locations: ArrayList<Location>
)
{
    constructor( enumAreaUuid: String,  name: String, polygon: ArrayList<LatLon>, locations: ArrayList<Location>)
            : this(UUID.randomUUID().toString(), Date().time, enumAreaUuid, name, polygon, locations )

    fun equals( other: CollectionTeam ): Boolean
    {
        if (this.uuid == other.uuid &&
            this.creationDate == other.creationDate &&
            this.enumAreaUuid == other.enumAreaUuid &&
            this.name == other.name)
        {
            return true
        }

        return false
    }

    fun doesNotEqual( collectionTeam: CollectionTeam ): Boolean
    {
        return !this.equals( collectionTeam )
    }

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