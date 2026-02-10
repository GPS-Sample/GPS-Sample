/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
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
    var sampleType: SampleType)
{
    constructor( studyUuid: String, name: String, sampleSize: Int, sampleType: SampleType )
            : this( UUID.randomUUID().toString(), Date().time, studyUuid, name, sampleSize, sampleType )

    fun equals( other: Strata ) : Boolean
    {
        if (this.uuid == other.uuid &&
            this.creationDate == other.creationDate &&
            this.name == other.name &&
            this.sampleSize == other.sampleSize &&
            this.sampleType == other.sampleType)
        {
            return true
        }

        return false
    }

    fun doesNotEqual( strata: Strata ): Boolean
    {
        return !this.equals( strata )
    }
}