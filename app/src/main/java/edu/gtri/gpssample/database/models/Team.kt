package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class Team(
    var uuid: String,
    var enumArea_uuid: String,
    var name: String)
{
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