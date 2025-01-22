/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class User(
    var uuid : String,
    var name: String,
    var role: String,
    var recoveryQuestion: String,
    var recoveryAnswer: String,
    var isOnline: Boolean )
{
    constructor(name: String, role: String, recoveryQuestion: String, recoveryAnswer: String, isOnline: Boolean) :
                this( UUID.randomUUID().toString(), name, role, recoveryQuestion, recoveryAnswer, isOnline)

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : User
        {
            return Json.decodeFromString<User>( message )
        }
    }
}