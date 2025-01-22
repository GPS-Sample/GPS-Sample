/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database.models

import com.google.android.gms.maps.model.LatLng
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class LatLon (
    var uuid : String,
    var creationDate : Long,
    var latitude: Double,
    var longitude: Double)
{
    constructor( creationDate: Long, latitude: Double, longitude: Double ) : this(UUID.randomUUID().toString(), creationDate, latitude, longitude)

    fun toLatLng() : LatLng
    {
        return LatLng( latitude, longitude )
    }

    fun equals( other: LatLon ) : Boolean
    {
        if (this.uuid == other.uuid &&
            this.creationDate == other.creationDate &&
            this.latitude == other.latitude &&
            this.longitude == other.longitude)
        {
            return true
        }

        return false
    }

    fun doesNotEqual( latLon: LatLon ): Boolean
    {
        return !this.equals( latLon )
    }
}