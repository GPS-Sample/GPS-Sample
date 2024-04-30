package edu.gtri.gpssample.database.models

import android.util.Base64
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
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
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
    var proximityWarningValue: Int,
    var studies : ArrayList<Study>,
    var enumAreas : ArrayList<EnumArea>,
    var selectedStudyId: Int,
    var selectedEnumAreaId: Int,
    var mapTileRegions: ArrayList<MapTileRegion>)
{
    constructor(name: String, dateFormat: DateFormat, timeFormat: TimeFormat, distanceFormat: DistanceFormat, minGpsPrecision: Int, encryptionPassword: String, allowManualLocationEntry: Boolean, subaddressIsrequired: Boolean, autoIncrementSubaddress: Boolean, proximityWarningIsEnabled: Boolean, proximityWarningValue: Int)
            : this(null, Date().time, name, dateFormat,timeFormat, distanceFormat, minGpsPrecision, encryptionPassword, allowManualLocationEntry, subaddressIsrequired, autoIncrementSubaddress, proximityWarningIsEnabled, proximityWarningValue,
                                    ArrayList<Study>(), ArrayList<EnumArea>(), -1, -1, ArrayList<MapTileRegion>())
    constructor(id: Int?, creationDate: Long, name: String, dateFormat: DateFormat, timeFormat: TimeFormat, distanceFormat: DistanceFormat,
                minGpsPrecision: Int, encryptionPassword: String, allowManualLocationEntry: Boolean, subaddressIsrequired: Boolean, autoIncrementSubaddress: Boolean, proximityWarningIsEnabled: Boolean, proximityWarningValue: Int, selectedStudyId: Int, selectedEnumAreaId: Int)
            : this(id, creationDate, name, dateFormat, timeFormat, distanceFormat, minGpsPrecision, encryptionPassword, allowManualLocationEntry, subaddressIsrequired, autoIncrementSubaddress, proximityWarningIsEnabled, proximityWarningValue,
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

    var proximityWarningStringValue : String
        get() {
            if (distanceFormat == DistanceFormat.Meters)
            {
                return "${proximityWarningValue} meters"
            }
            else if (distanceFormat == DistanceFormat.Feet)
            {
                return "${proximityWarningValue} feet"
            }
            else
            {
                return proximityWarningValue.toString()
            }
        }
        set(value){
            value.toIntOrNull()?.let {
                proximityWarningValue = it
            } ?: {proximityWarningValue = 10}
        }

    fun pack() : String
    {
        try
        {
            // step 1: create the json string

            val jsonString = Json.encodeToString( this )

            // step 2: compress the json string

            val byteArrayOutputStream = ByteArrayOutputStream(jsonString.length)
            val gzipOutputStream = GZIPOutputStream( byteArrayOutputStream )
            gzipOutputStream.write(jsonString.toByteArray())
            gzipOutputStream.close()
            val byteArray = byteArrayOutputStream.toByteArray()
            byteArrayOutputStream.close()

            val compressedString = Base64.encodeToString( byteArray, Base64.DEFAULT )

            // step 3: encrypt the json string, if necessary

            if (encryptionPassword.isEmpty())
            {
                return  compressedString
            }
            else
            {
                return  EncryptionUtil.Encrypt(compressedString, encryptionPassword)
            }
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
        }

        return ""
    }

    companion object
    {
        fun unpack( jsonString: String, password: String ) : Config?
        {
            try
            {
                var clearText = jsonString

                // step 1: decrypt the json string, if necessary

                if (password.isNotEmpty())
                {
                    EncryptionUtil.Decrypt(jsonString, password)?.let {
                        clearText = it
                    }
                }

                // step 2: decompress the json string

                val byteArray = Base64.decode( clearText, Base64.DEFAULT )
                val byteArrayInputStream = ByteArrayInputStream( byteArray )
                val gzipInputStream = GZIPInputStream( byteArrayInputStream, byteArray.size )
                val bytes = gzipInputStream.readBytes()
                val uncompressedString = bytes.decodeToString()
                gzipInputStream.close()
                byteArrayInputStream.close()

                // step 3: decode the JSON string into a Config object

                return Json.decodeFromString<Config>( uncompressedString )
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
            }

            return null
        }
    }
}