package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@kotlinx.serialization.Serializable
data class CollectionTeam(
    var id : Int? = null,
    var creationDate: Long,
    var studyId: Int,
    var name: String,
    var locations: ArrayList<Location>
)
{
    constructor( studyId: Int,  name: String, locations: ArrayList<Location>) : this(null, Date().time, studyId, name, locations )

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