/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class LatLon (
    var uuid : String,
    var creationDate : Long,
    var latitude: Double,
    var longitude: Double,
    var version: String)
{
    constructor( creationDate: Long, latitude: Double, longitude: Double ) : this(UUID.randomUUID().toString(), creationDate, latitude, longitude, UUID.randomUUID().toString())

    fun toLatLng() : LatLng
    {
        return LatLng( latitude, longitude )
    }
}