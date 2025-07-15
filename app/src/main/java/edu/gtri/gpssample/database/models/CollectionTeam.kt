/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class CollectionTeam(
    var uuid : String,
    var creationDate: Long,
    var enumAreaUuid: String,
    var name: String,
    var polygon: ArrayList<LatLon>,
    var locationUuids: ArrayList<String> // the EnumArea is the single point keeper of locations, we don't want a copy of it here!
)
{
    constructor( enumAreaUuid: String,  name: String, polygon: ArrayList<LatLon>, locationUuids: ArrayList<String>)
            : this(UUID.randomUUID().toString(), Date().time, enumAreaUuid, name, polygon, locationUuids )

    fun equals( other: CollectionTeam ): Boolean
    {
        if (this.uuid == other.uuid &&
            this.creationDate == other.creationDate &&
            this.enumAreaUuid == other.enumAreaUuid &&
            this.name == other.name)
        {
            return true
        }

        return false
    }

    fun doesNotEqual( collectionTeam: CollectionTeam ): Boolean
    {
        return !this.equals( collectionTeam )
    }

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : EnumerationTeam
        {
            return Json.decodeFromString<EnumerationTeam>( message )
        }
    }
}