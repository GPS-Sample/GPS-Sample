/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

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
    var name: String,
    var samplingType : SampleType,
    var sampleSize: Int,
    var rule : Rule?)
{
    constructor(name: String) : this(UUID.randomUUID().toString(), name, SampleType.None, 0, null)
    constructor(uuid: String, name: String, samplingType : SampleType, sampleSize: Int) : this(uuid, name, samplingType, sampleSize, null)
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