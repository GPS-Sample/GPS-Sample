package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Config(
    var id: Int,
    var name: String,
    var dateFormat: String,
    var timeFormat: String,
    var distanceFormat: String,
    var minGpsPrecision: Int )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : Config
        {
            return Json.decodeFromString<Config>( message )
        }
    }
}