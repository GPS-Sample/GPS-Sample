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
    var allowManualLocationEntry: Boolean,
    var subaddressIsrequired: Boolean,
    var autoIncrementSubaddress: Boolean,
    var proximityWarningIsEnabled: Boolean,
    var proximityWarningValue: Int,
    var studies : ArrayList<Study>,
    var enumAreas : ArrayList<EnumArea>,
    var selectedStudyUuid: String,
    var selectedEnumAreaUuid: String,
    var validUsers : String)
{
    constructor(timeZone: Int, name: String, dbVersion: Int, mapEngine: Int, dateFormat: DateFormat, timeFormat: TimeFormat, distanceFormat: DistanceFormat, minGpsPrecision: Int, encryptionPassword: String, allowManualLocationEntry: Boolean, subaddressIsrequired: Boolean, autoIncrementSubaddress: Boolean, proximityWarningIsEnabled: Boolean, proximityWarningValue: Int)
            : this(UUID.randomUUID().toString(), Date().time, timeZone, name, dbVersion, mapEngine, dateFormat, timeFormat, distanceFormat, minGpsPrecision, encryptionPassword, allowManualLocationEntry, subaddressIsrequired, autoIncrementSubaddress, proximityWarningIsEnabled, proximityWarningValue,
                ArrayList<Study>(), ArrayList<EnumArea>(), "", "", "")
    constructor(uuid: String, creationDate: Long, timeZone: Int, name: String, dbVersion: Int, mapEngine: Int, dateFormat: DateFormat, timeFormat: TimeFormat, distanceFormat: DistanceFormat,
                minGpsPrecision: Int, encryptionPassword: String, allowManualLocationEntry: Boolean, subaddressIsrequired: Boolean, autoIncrementSubaddress: Boolean, proximityWarningIsEnabled: Boolean, proximityWarningValue: Int, selectedStudyUuid: String, selectedEnumAreaUuid: String, validUsers: String)
            : this(uuid, creationDate, timeZone, name, dbVersion, mapEngine, dateFormat, timeFormat, distanceFormat, minGpsPrecision, encryptionPassword, allowManualLocationEntry, subaddressIsrequired, autoIncrementSubaddress, proximityWarningIsEnabled, proximityWarningValue,
                ArrayList<Study>(), ArrayList<EnumArea>(), selectedStudyUuid, selectedEnumAreaUuid, validUsers)

    fun equals( other: Config ): Boolean
    {
        if (this.uuid == other.uuid &&
            this.creationDate == other.creationDate &&
            this.timeZone == other.timeZone &&
            this.name == other.name &&
            this.mapEngineIndex == other.mapEngineIndex &&
            this.dateFormat == other.dateFormat &&
            this.timeFormat == other.timeFormat &&
            this.distanceFormat == other.distanceFormat &&
            this.minGpsPrecision == other.minGpsPrecision &&
            this.encryptionPassword == other.encryptionPassword &&
            this.allowManualLocationEntry == other.allowManualLocationEntry &&
            this.subaddressIsrequired == other.subaddressIsrequired &&
            this.autoIncrementSubaddress == other.autoIncrementSubaddress &&
            this.proximityWarningIsEnabled == other.proximityWarningIsEnabled &&
            this.proximityWarningValue == other.proximityWarningValue &&
            this.selectedStudyUuid == other.selectedStudyUuid &&
            this.selectedEnumAreaUuid == other.selectedEnumAreaUuid &&
            this.validUsers == other.validUsers)
        {
            return true
        }

        return false
    }

    fun doesNotEqual( config: Config ): Boolean
    {
        return !this.equals( config )
    }

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

    fun packMinimal() : String
    {
        var packedConfig = this.pack()

        if (this.selectedEnumAreaUuid.isNotEmpty())
        {
            unpack( packedConfig, encryptionPassword )?.let { configCopy ->

                for (enumArea in configCopy.enumAreas)
                {
                    if (enumArea.uuid == configCopy.selectedEnumAreaUuid)
                    {
                        configCopy.enumAreas.clear()
                        configCopy.enumAreas.add( enumArea )
                        break
                    }
                }

                val enumArea = configCopy.enumAreas[0]

                if (enumArea.selectedCollectionTeamUuid.isNotEmpty())
                {
                    enumArea.enumerationTeams.clear()

                    for (collectionTeam in enumArea.collectionTeams)
                    {
                        if (collectionTeam.uuid == enumArea.selectedCollectionTeamUuid)
                        {
                            enumArea.collectionTeams.clear()
                            enumArea.collectionTeams.add( collectionTeam )
                            break
                        }
                    }
                }
                else if (enumArea.selectedEnumerationTeamUuid.isNotEmpty())
                {
                    enumArea.collectionTeams.clear()

                    for (enumerationTeam in enumArea.enumerationTeams)
                    {
                        if (enumerationTeam.uuid == enumArea.selectedEnumerationTeamUuid)
                        {
                            enumArea.enumerationTeams.clear()
                            enumArea.enumerationTeams.add( enumerationTeam )
                            break
                        }
                    }
                }

                packedConfig = configCopy.pack()
            }
        }

        return packedConfig
    }

    companion object
    {
        fun unpack( jsonString: String, password: String ) : Config?
        {
            try
            {
                // check for a cleartext string

                if (jsonString.isNotEmpty() && jsonString.first() == '{')
                {
                    return Json.decodeFromString<Config>( jsonString )
                }

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