package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class TeamMember(
    var id : Int? = null,
    var uuid: String,
    var team_uuid: String,
    var name: String)
{
    constructor(uuid: String, team_uuid: String, name: String) : this(null, uuid, team_uuid, name)
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : TeamMember
        {
            return Json.decodeFromString<TeamMember>( message )
        }
    }
}