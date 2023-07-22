package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@kotlinx.serialization.Serializable
data class Team(
    var id : Int? = null,
    var creationDate: Long,
    var studyId: Int,

    var name: String,
    var isEnumerationTeam: Boolean,
    var polygon: ArrayList<LatLon>)
{
    constructor( studyId: Int,  name: String, isEnumerationTeam: Boolean, polygon: ArrayList<LatLon> )
            : this(null, Date().time, studyId, name, isEnumerationTeam, polygon )

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : Team
        {
            return Json.decodeFromString<Team>( message )
        }
    }
}