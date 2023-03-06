package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class Team(
    var id : Int? = null,
    var uuid: String,
    var enumArea_uuid: String,
    var name: String)
{
    constructor(uuid: String, enumArea_uuid: String, name: String) :
                this(null, uuid, enumArea_uuid, name)
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