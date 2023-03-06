package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Study(
    var id : Int? = null,
    var uuid: String,
    var config_uuid: String,
    var name: String,
    var samplingMethod: String,
    var sampleSize: Int,
    var sampleSizeIndex: Int )
{
    constructor(uuid: String, config_uuid: String, name: String, samplingMethod: String,
                sampleSize: Int, sampleSizeIndex: Int) : this(null, uuid, config_uuid,
                name, samplingMethod, sampleSize, sampleSizeIndex)
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