package edu.gtri.gpssample.network.models

import edu.gtri.gpssample.database.models.Rule
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NetworkRules( var rules: List<Rule> )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : NetworkRules
        {
            return Json.decodeFromString<NetworkRules>( message )
        }
    }
}