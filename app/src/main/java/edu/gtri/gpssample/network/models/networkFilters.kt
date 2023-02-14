package edu.gtri.gpssample.network.models

import edu.gtri.gpssample.database.models.Filter
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NetworkFilters( var filters: List<Filter> )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : NetworkFilters
        {
            return Json.decodeFromString<NetworkFilters>( message )
        }
    }
}