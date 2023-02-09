package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Study(
    var id: Int,
    var configId: Int,
    var name: String,
    var samplingMethod: String,
    var isValid: Boolean )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : Study
        {
            return Json.decodeFromString<Study>( message )
        }
    }
}