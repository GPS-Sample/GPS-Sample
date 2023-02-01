package edu.gtri.gpssample.network.models

import edu.gtri.gpssample.database.models.Field
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NetworkFields( var fields: List<Field> )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : NetworkFields
        {
            return Json.decodeFromString<NetworkFields>( message )
        }
    }
}