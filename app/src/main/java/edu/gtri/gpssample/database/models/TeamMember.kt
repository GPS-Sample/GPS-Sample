package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class TeamMember(
    var id : Int? = null,
    var team_id: Int,
    var name: String)
{
    constructor(team_id: Int, name: String) : this(null, team_id, name)

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