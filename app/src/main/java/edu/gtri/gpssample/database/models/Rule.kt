/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.Operator
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class Rule(
    var uuid : String,
    var creationDate: Long,
    var fieldUuid: String,
    var name: String,
    var value: String,
    var isSubsetRule: Boolean = false,
    var operator : Operator?,
    var filterOperator: FilterOperator?,
    var fieldDataOptions : ArrayList<FieldDataOption>,
    var version: String)
{
    constructor() : this(UUID.randomUUID().toString(), Date().time, "", "", "", false, Operator.Equal, null, ArrayList<FieldDataOption>(), UUID.randomUUID().toString())

    constructor( fieldUuid: String, name: String, value: String, operator: Operator)
            : this(UUID.randomUUID().toString(), Date().time, fieldUuid, name, value, false, operator, null, ArrayList<FieldDataOption>(), UUID.randomUUID().toString())

    constructor( fieldUuid: String, name: String, value: String)
            : this(UUID.randomUUID().toString(), Date().time, fieldUuid, name, value, false, null, null, ArrayList<FieldDataOption>(), UUID.randomUUID().toString())

    constructor(uuid : String, creationDate: Long, fieldUuid: String, name: String, value: String, isSubsetRule: Boolean, operator: Operator, filterOperator: FilterOperator?, version: String)
            : this(uuid, creationDate, fieldUuid, name, value, isSubsetRule, operator, filterOperator, ArrayList<FieldDataOption>(), version)

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    fun copy() : Rule?
    {
        val _copy = unpack(pack())

        _copy?.let { _copy ->
            return _copy
        }
        return null
    }
    override fun toString() : String
    {
        return this.name
    }
    
    companion object
    {
        fun unpack( message: String ) : Rule
        {
            return Json.decodeFromString<Rule>( message )
        }
    }
}