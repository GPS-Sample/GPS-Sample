/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class MapTileRegion (
    var uuid: String,
    var creationDate: Long,
    var version: String,
    var northEast: LatLon,
    var southWest: LatLon,
    var enumAreaUuid: String)
{
    constructor( northEast: LatLon, southWest: LatLon, enumAreaUuid: String ) : this( UUID.randomUUID().toString(), Date().time, UUID.randomUUID().toString(), northEast, southWest, enumAreaUuid )
}