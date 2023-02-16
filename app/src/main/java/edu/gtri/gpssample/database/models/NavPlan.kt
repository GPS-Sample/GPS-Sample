package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NavPlan(
    var uuid: String,
    var sample_uuid: String,
    var name: String)
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : NavPlan
        {
            return Json.decodeFromString<NavPlan>( message )
        }
    }
}