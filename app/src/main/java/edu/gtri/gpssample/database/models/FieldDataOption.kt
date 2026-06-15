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
data class FieldDataOption(
    var uuid : String,
    var creationDate: Long,
    var name : String,
    var value: Boolean,
    var version: String)
{
    constructor(name: String, value: Boolean) : this(UUID.randomUUID().toString(), Date().time, name, value, UUID.randomUUID().toString())

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : FieldDataOption
        {
            return Json.decodeFromString<FieldDataOption>( string )
        }
    }
}