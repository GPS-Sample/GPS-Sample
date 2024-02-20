package edu.gtri.gpssample.database.models

import android.util.Log
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.network.models.NetworkCommand
import edu.gtri.gpssample.utils.EncryptionUtil
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
import java.util.*
import kotlin.collections.ArrayList

@Serializable
data class Config(
    var id : Int? = null,
    var creationDate: Long,
    var name: String,
    var dateFormat: DateFormat,
    var timeFormat: TimeFormat,
    var distanceFormat: DistanceFormat,
    var minGpsPrecision: Int,
    var encryptionPassword: String,
    var allowManualLocationEntry: Boolean,
    var subaddressIsrequired: Boolean,
    var autoIncrementSubaddress: Boolean,
    var proximityWarningIsEnabled: Boolean,
    var studies : ArrayList<Study>,
    var enumAreas : ArrayList<EnumArea>,
    var selectedStudyId: Int,
    var selectedEnumAreaId: Int,
    var mapTileRegions: ArrayList<MapTileRegion>)
{
    constructor(name: String, dateFormat: DateFormat, timeFormat: TimeFormat, distanceFormat: DistanceFormat, minGpsPrecision: Int, encryptionPassword: String, allowManualLocationEntry: Boolean, subaddressIsrequired: Boolean, autoIncrementSubaddress: Boolean, proximityWarningIsEnabled: Boolean)
            : this(null, Date().time, name, dateFormat,timeFormat, distanceFormat, minGpsPrecision, encryptionPassword, allowManualLocationEntry, subaddressIsrequired, autoIncrementSubaddress, proximityWarningIsEnabled,
                                    ArrayList<Study>(), ArrayList<EnumArea>(), -1, -1, ArrayList<MapTileRegion>())
    constructor(id: Int?, creationDate: Long, name: String, dateFormat: DateFormat, timeFormat: TimeFormat, distanceFormat: DistanceFormat,
                minGpsPrecision: Int, encryptionPassword: String, allowManualLocationEntry: Boolean, subaddressIsrequired: Boolean, autoIncrementSubaddress: Boolean, proximityWarningIsEnabled: Boolean, selectedStudyId: Int, selectedEnumAreaId: Int)
            : this(id, creationDate, name, dateFormat, timeFormat, distanceFormat, minGpsPrecision, encryptionPassword, allowManualLocationEntry, subaddressIsrequired, autoIncrementSubaddress, proximityWarningIsEnabled,
        ArrayList<Study>(), ArrayList<EnumArea>(), selectedStudyId, selectedEnumAreaId, ArrayList<MapTileRegion>())

    var minimumGPSPrecision : String
        get() {
            if (distanceFormat == DistanceFormat.Meters)
            {
                return "${minGpsPrecision} meters"
            }
            else if (distanceFormat == DistanceFormat.Feet)
            {
                return "${minGpsPrecision} feet"
            }
            else
            {
                return minGpsPrecision.toString()
            }
        }
        set(value){
            value.toIntOrNull()?.let {
                minGpsPrecision = it
            } ?: {minGpsPrecision = 0}

        }
    fun pack() : String
    {
       // return Json.encodeToString( this )
        val jsonString = Json.encodeToString( this )
        return  EncryptionUtil.Encrypt(jsonString)
    }

    companion object
    {
        fun unpack( message: String ) : Config?
        {
            try
            {
                val decrypted = EncryptionUtil.Decrypt(message)
                decrypted?.let {decrypted ->
                    return Json.decodeFromString<Config>( decrypted )
                }
            }
            catch( ex: Exception )
            {
                Log.d( "xxXXx", ex.stackTrace.toString())
            }

            return null
        }
    }
}