package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

@Serializable
data class Config(
    var id : Int? = null,
    var uuid: String,
    var name: String,
    var dateFormat: DateFormat,
    var timeFormat: TimeFormat,
    var distanceFormat: DistanceFormat,
    var minGpsPrecision: Int,
    var studies : ArrayList<Study>,
    var enumAreas : List<EnumArea>?
    )
{
    constructor(uuid: String, name: String, dateFormat: DateFormat, timeFormat: TimeFormat, distanceFormat: DistanceFormat,
        minGpsPrecision: Int) : this(null, uuid, name, dateFormat,timeFormat, distanceFormat, minGpsPrecision,
                                    ArrayList<Study>(), null)
    constructor(id: Int?, uuid: String, name: String, dateFormat: DateFormat, timeFormat: TimeFormat, distanceFormat: DistanceFormat,
                minGpsPrecision: Int) : this(id, uuid, name, dateFormat, timeFormat, distanceFormat, minGpsPrecision,
                ArrayList<Study>(), null)

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