package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Study(
    var uuid: String,
    var config_uuid: String,
    var name: String,
    var samplingMethod: String,
    var sampleSize: Int,
    var sampleSizeIndex: Int )
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