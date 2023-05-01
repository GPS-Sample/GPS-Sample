package edu.gtri.gpssample.database.models

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@kotlinx.serialization.Serializable
data class Team(
    var id : Int? = null,
    var enumAreaId: Int,
    var name: String)
{
    constructor(enumAreaId: Int, name: String) : this(null, enumAreaId, name)

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