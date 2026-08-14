/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
import edu.gtri.gpssample.constants.ReviewStatus
import edu.gtri.gpssample.constants.SamplingState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*
import kotlin.collections.ArrayList

@Serializable
@SerialName("EnumerationItem")
data class EnumerationItem(
    var uuid : String,
    var creationDate: Long,
    var distance : Double,      // not stored in DB!
    var distanceUnits: String,  // not stored in DB!
    var isVisible : Boolean,    // not stored in DB!
    var subAddress : String,
    var enumeratorName : String,
    var enumerationState : EnumerationState,
    var enumerationDate: Long,
    var enumerationIncompleteReason : String,
    var enumerationNotes : String,
    var enumerationEligibleForSampling : Boolean,
    var enumerationEligibleForSubsetSampling : Boolean = false,
    var samplingState : SamplingState,
    var subsetSamplingState : SamplingState = SamplingState.None,
    var collectorName : String,
    var collectionState : CollectionState,
    var collectionDate: Long,
    var collectionIncompleteReason : String,
    var collectionNotes : String,
    var reviewStatus : ReviewStatus,
    var exclusionReason : String,
    var exclusionNotes : String,
    var fieldDataList : ArrayList<FieldData>,
    var locationUuid : String,
    var odkRecordUri : String,
    var version : String)
{
    constructor() : this(
        UUID.randomUUID().toString(),
        Date().time,
        0.0,
        "",
        true,
        "",
        "",
        EnumerationState.Undefined,
        0,
        "",
        "",
        false,
        false,
        SamplingState.None,
        SamplingState.None,
        "",
        CollectionState.Undefined,
        0,
        "",
        "",
        ReviewStatus.Ignore,
        "",
        "",
        ArrayList<FieldData>(),
        "",
        "",
        "")

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : EnumerationItem
        {
            return Json.decodeFromString<EnumerationItem>( string )
        }
    }
}
