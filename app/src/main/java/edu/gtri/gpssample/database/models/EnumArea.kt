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
data class EnumArea (
    var uuid : String,
    var creationDate: Long,
    var configUuid: String,
    var strataUuid: String,
    var name: String,
    var mbTilesPath: String,
    var mbTilesSize: Long,
    var vertices: ArrayList<LatLon>,
    var locations: ArrayList<Location>,
    var enumerationTeams: ArrayList<EnumerationTeam>,
    var selectedEnumerationTeamUuid: String,
    var collectionTeams: ArrayList<CollectionTeam>,
    var selectedCollectionTeamUuid: String,
    var mapTileRegion: MapTileRegion?,
    var breadcrumbs: ArrayList<Breadcrumb>,
    var version: String ) {
    constructor(
        uuid: String,
        creationDate: Long,
        configUuid: String,
        strataUuid: String,
        name: String,
        mbTilesPath: String,
        mbTilesSize: Long,
        selectedEnumerationTeamUuid: String,
        selectedCollectionTeamUuid: String,
        version: String
    )
            : this(
        uuid,
        creationDate,
        configUuid,
        strataUuid,
        name,
        mbTilesPath,
        mbTilesSize,
        ArrayList<LatLon>(),
        ArrayList<Location>(),
        ArrayList<EnumerationTeam>(),
        selectedEnumerationTeamUuid,
        ArrayList<CollectionTeam>(),
        selectedCollectionTeamUuid,
        null,
        ArrayList<Breadcrumb>(),
        version
    )

    constructor(
        configUuid: String,
        strataUuid: String,
        name: String,
        mbTilesPath: String,
        mbTilesSize: Long,
        vertices: ArrayList<LatLon>,
        mapTileRegion: MapTileRegion?
    )
            : this(
        UUID.randomUUID().toString(),
        Date().time,
        configUuid,
        strataUuid,
        name,
        mbTilesPath,
        mbTilesSize,
        vertices,
        ArrayList<Location>(),
        ArrayList<EnumerationTeam>(),
        "",
        ArrayList<CollectionTeam>(),
        "",
        mapTileRegion,
        ArrayList<Breadcrumb>(),
        UUID.randomUUID().toString()
    )

    constructor(
        creationDate: Long,
        configUuid: String,
        strataUuid: String,
        name: String,
        mbTilesPath: String,
        mbTilesSize: Long,
        vertices: ArrayList<LatLon>,
        mapTileRegion: MapTileRegion?
    )
            : this(
        UUID.randomUUID().toString(),
        creationDate,
        configUuid,
        strataUuid,
        name,
        mbTilesPath,
        mbTilesSize,
        vertices,
        ArrayList<Location>(),
        ArrayList<EnumerationTeam>(),
        "",
        ArrayList<CollectionTeam>(),
        "",
        mapTileRegion,
        ArrayList<Breadcrumb>(),
        UUID.randomUUID().toString()
    )

    fun pack(password: String): String {
        try {
            // step 1: create the json string

            val jsonString = json.encodeToString(this)

            // step 2: compress the json string

            val byteArrayOutputStream = ByteArrayOutputStream(jsonString.length)
            val gzipOutputStream = GZIPOutputStream(byteArrayOutputStream)
            gzipOutputStream.write(jsonString.toByteArray())
            gzipOutputStream.close()
            val byteArray = byteArrayOutputStream.toByteArray()
            byteArrayOutputStream.close()

            val compressedString = Base64.encodeToString(byteArray, Base64.NO_WRAP)

            // step 3: encrypt the json string, if necessary

            return EncryptionUtil.Encrypt(compressedString, password)
        } catch (ex: Exception) {
            Log.d("xxx", ex.stackTraceToString())
        }

        return ""
    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            prettyPrint = false
        }

        fun unpack( jsonString: String, password: String ) : Pair<EnumArea?,ErrorCode>
        {
            var enumArea: EnumArea? = null
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

                    enumArea = json.decodeFromString<EnumArea>( uncompressedString )

                    errorCode = ErrorCode.None
                }
            }
            catch( ex: Exception )
            {
                errorCode = ErrorCode.UnknownError
                Log.d( "xxx", ex.stackTraceToString())
            }

            return Pair(enumArea,errorCode )
        }
    }
}