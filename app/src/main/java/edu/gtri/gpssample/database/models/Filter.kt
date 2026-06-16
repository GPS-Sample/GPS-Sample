/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.SampleType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class Filter(
    var uuid : String,
    var creationDate: Long,
    var name: String,
    var samplingType : SampleType,
    var sampleSize: Int,
    var rule : Rule?,
    var studyUuid : String,
    var version: String)
{
    constructor(name: String, studyUuid: String) : this( UUID.randomUUID().toString(), Date().time, name, SampleType.None, 0, null, studyUuid, UUID.randomUUID().toString())
    constructor(uuid: String, creationDate: Long, name: String, samplingType : SampleType, sampleSize: Int, studyUuid: String, version: String) : this(uuid, creationDate, name, samplingType, sampleSize, null, studyUuid, version )

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : Filter
        {
            return Json.decodeFromString<Filter>( message )
        }
    }
}