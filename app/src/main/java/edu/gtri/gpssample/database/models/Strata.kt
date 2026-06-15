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
import java.util.*

@Serializable
data class Strata (
    var uuid: String,
    var creationDate: Long,
    var studyUuid: String,
    var name: String,
    var sampleSize: Int,
    var sampleType: SampleType,
    var version: String )
{
    constructor( studyUuid: String, name: String, sampleSize: Int, sampleType: SampleType )
            : this( UUID.randomUUID().toString(), Date().time, studyUuid, name, sampleSize, sampleType, UUID.randomUUID().toString())
}