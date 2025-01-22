/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.CollectionState
import edu.gtri.gpssample.constants.EnumerationState
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
    var syncCode : Int,
    var distance : Double,      // not stored in DB!
    var distanceUnits: String,  // not stored in DB!
    var subAddress : String,
    var enumeratorName : String,
    var enumerationState : EnumerationState,
    var enumerationDate: Long,
    var enumerationIncompleteReason : String,
    var enumerationNotes : String,
    var enumerationEligibleForSampling : Boolean,
    var samplingState : SamplingState,
    var collectorName : String,
    var collectionState : CollectionState,
    var collectionDate: Long,
    var collectionIncompleteReason : String,
    var collectionNotes : String,
    var fieldDataList : ArrayList<FieldData>,
    var locationUuid : String,
    var odkRecordUri : String )
{
    constructor() : this(
        "",
        Date().time,
        0,
        0.0,
        "",
        "",
        "",
        EnumerationState.Undefined,
        0,
        "",
        "",
        false,
        SamplingState.None,
        "",
        CollectionState.Undefined,
        0,
        "",
        "",
        ArrayList<FieldData>(),
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