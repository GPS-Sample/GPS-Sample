package edu.gtri.gpssample.network.models

import edu.gtri.gpssample.database.models.NavPlan
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NetworkNavPlans( var navPlans: List<NavPlan> )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : NetworkNavPlans
        {
            return Json.decodeFromString<NetworkNavPlans>( message )
        }
    }
}