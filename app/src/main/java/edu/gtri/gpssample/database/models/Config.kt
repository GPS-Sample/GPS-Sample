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
    var id : Int? = null,
    var uuid: String,
    var name: String,
    var dateFormat: String,
    var timeFormat: String,
    var distanceFormat: String,
    var minGpsPrecision: Int,
    var currentStudy : Study?,
    var studies : List<Study>
    )
{
    constructor(uuid: String, name: String, dateFormat: String, imeFormat: String, distanceFormat: String,
        minGpsPrecision: Int) : this(null, uuid, name, dateFormat, imeFormat, distanceFormat, minGpsPrecision,
                                    null,ArrayList<Study>())
    constructor(id: Int?, uuid: String, name: String, dateFormat: String, imeFormat: String, distanceFormat: String,
                minGpsPrecision: Int) : this(id, uuid, name, dateFormat, imeFormat, distanceFormat, minGpsPrecision,
                null,ArrayList<Study>())

    var minimumGPSPrecision : String
        get() = minGpsPrecision.toString()
        set(value){
            value.toIntOrNull()?.let {
                minGpsPrecision = it
            } ?: {minGpsPrecision = 0}

        }
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