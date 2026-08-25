/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import android.util.Base64
import android.util.Log
import edu.gtri.gpssample.constants.DateFormat
import edu.gtri.gpssample.constants.DistanceFormat
import edu.gtri.gpssample.constants.TimeFormat
import edu.gtri.gpssample.utils.EncryptionUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.collections.ArrayList

@Serializable
data class Config(
    var uuid : String,
    var creationDate: Long,
    var timeZone: Int,
    var name: String,
    var dbVersion: Int,
    var mapEngineIndex: Int,
    var dateFormat: DateFormat,
    var timeFormat: TimeFormat,
    var distanceFormat: DistanceFormat,
    var minGpsPrecision: Int,
    var encryptionPassword: String,
    var allowSupervisorEdits: Boolean,
    var allowManualLocationEntry: Boolean,
    var subaddressIsrequired: Boolean,
    var autoIncrementSubaddress: Boolean,
    var proximityWarningIsEnabled: Boolean,
    var proximityWarningValue: Int,
    var geofenceIsEnabled: Boolean,
    var geofenceBufferValue: Int,
    var isArchived: Boolean,
    var studies : ArrayList<Study>,
    @kotlinx.serialization.Transient
    var enumAreas : ArrayList<EnumArea> = ArrayList(),
    var selectedStudyUuid: String,
    var selectedEnumAreaUuid: String,
    var validUsers : String,
    var version : String )
{
    constructor(timeZone: Int, name: String, dbVersion: Int, mapEngine: Int, dateFormat: DateFormat, timeFormat: TimeFormat, distanceFormat: DistanceFormat, minGpsPrecision: Int, encryptionPassword: String, allowSupervisorEdits: Boolean, allowManualLocationEntry: Boolean, subaddressIsrequired: Boolean, autoIncrementSubaddress: Boolean, proximityWarningIsEnabled: Boolean, proximityWarningValue: Int, geofenceIsEnabled: Boolean, geofenceBufferValue: Int)
            : this(UUID.randomUUID().toString(), Date().time, timeZone, name, dbVersion, mapEngine, dateFormat, timeFormat, distanceFormat, minGpsPrecision, encryptionPassword, allowSupervisorEdits, allowManualLocationEntry, subaddressIsrequired, autoIncrementSubaddress, proximityWarningIsEnabled, proximityWarningValue, geofenceIsEnabled, geofenceBufferValue,
                false, ArrayList<Study>(), ArrayList<EnumArea>(), "", "", "", UUID.randomUUID().toString())
    constructor(uuid: String, creationDate: Long, timeZone: Int, name: String, dbVersion: Int, mapEngine: Int, dateFormat: DateFormat, timeFormat: TimeFormat, distanceFormat: DistanceFormat,
                minGpsPrecision: Int, encryptionPassword: String, allowSupervisorEdits: Boolean, allowManualLocationEntry: Boolean, subaddressIsrequired: Boolean, autoIncrementSubaddress: Boolean, proximityWarningIsEnabled: Boolean, proximityWarningValue: Int, geofenceIsEnabled: Boolean, geofenceBufferValue: Int, isArchived: Boolean, selectedStudyUuid: String, selectedEnumAreaUuid: String, validUsers: String, version: String)
            : this(uuid, creationDate, timeZone, name, dbVersion, mapEngine, dateFormat, timeFormat, distanceFormat, minGpsPrecision, encryptionPassword, allowSupervisorEdits, allowManualLocationEntry, subaddressIsrequired, autoIncrementSubaddress, proximityWarningIsEnabled, proximityWarningValue, geofenceIsEnabled, geofenceBufferValue,
                isArchived, ArrayList<Study>(), ArrayList<EnumArea>(), selectedStudyUuid, selectedEnumAreaUuid, validUsers, version)

    var minimumGPSPrecision : String
        get() {
            return minGpsPrecision.toString()
        }
        set(value){
            value.toIntOrNull()?.let {
                minGpsPrecision = it
            } ?: {minGpsPrecision = 0}

        }

    var proximityWarningStringValue : String
        get() {
            return proximityWarningValue.toString()
        }
        set(value){
            value.toIntOrNull()?.let {
                proximityWarningValue = it
            } ?: {proximityWarningValue = 5}
        }

    var geofenceBufferStringValue : String
        get() {
            return geofenceBufferValue.toString()
        }
        set(value){
            value.toIntOrNull()?.let {
                geofenceBufferValue = it
            } ?: {geofenceBufferValue = 5}
        }

    fun pack() : String
    {
        try
        {
            // step 1: create the json string

            val jsonString = json.encodeToString( this )

            // step 2: compress the json string

            val byteArrayOutputStream = ByteArrayOutputStream(jsonString.length)
            val gzipOutputStream = GZIPOutputStream( byteArrayOutputStream )
            gzipOutputStream.write(jsonString.toByteArray())
            gzipOutputStream.close()
            val byteArray = byteArrayOutputStream.toByteArray()
            byteArrayOutputStream.close()

            val compressedString = Base64.encodeToString( byteArray, Base64.NO_WRAP )

            // step 3: encrypt the json string

            return  EncryptionUtil.Encrypt(compressedString, encryptionPassword)
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
        }

        return ""
    }

    companion object
    {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }

        fun unpack( jsonString: String, password: String ) : Pair<Config?, ErrorCode>
        {
            var config: Config? = null
            var errorCode = ErrorCode.DecryptError

            try
            {
                // step 1: decrypt the json string

                EncryptionUtil.Decrypt(jsonString, password)?.let { clearText ->

                    // step 2: decompress the json string

                    errorCode = ErrorCode.DecompressError

                    val byteArray = Base64.decode( clearText, Base64.NO_WRAP )
                    val byteArrayInputStream = ByteArrayInputStream( byteArray )
                    val gzipInputStream = GZIPInputStream( byteArrayInputStream, byteArray.size )
                    val bytes = gzipInputStream.readBytes()
                    val uncompressedString = bytes.decodeToString()
                    gzipInputStream.close()
                    byteArrayInputStream.close()

                    // step 3: decode the JSON string into a Config object

                    errorCode = ErrorCode.DecodeError

                    config = json.decodeFromString<Config>( uncompressedString )

                    errorCode = ErrorCode.None
                }
            }
            catch( ex: Exception )
            {
                errorCode = ErrorCode.UnknownError
                Log.d( "xxx", ex.stackTraceToString())
            }

            return Pair( config, errorCode  )
        }
    }
}