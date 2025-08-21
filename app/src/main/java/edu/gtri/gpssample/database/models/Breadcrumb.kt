/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

@Serializable
data class Breadcrumb(
    var uuid: String,
    var creationDate: Long,
    var enumAreaUuid: String,
    var latitude: Double,
    var longitude: Double,
    var groupId: String
)
{
    constructor( enumAreaUuid: String, latitude: Double, longitude: Double, groupId: String )
            : this( UUID.randomUUID().toString(), Date().time, enumAreaUuid, latitude, longitude, groupId )
}