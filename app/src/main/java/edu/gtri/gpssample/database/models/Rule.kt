/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

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
    var fieldUuid: String,
    var name: String,
    var value: String,
    var operator : Operator?,
    var filterOperator: FilterOperator?,
    var fieldDataOptions : ArrayList<FieldDataOption>
    )
{
    constructor() : this(UUID.randomUUID().toString(), "", "", "", Operator.Equal, null, ArrayList<FieldDataOption>())

    constructor( fieldUuid: String, name: String, value: String, operator: Operator)
            : this(UUID.randomUUID().toString(), fieldUuid, name, value, operator, null, ArrayList<FieldDataOption>())

    constructor( fieldUuid: String, name: String, value: String)
            : this(UUID.randomUUID().toString(), fieldUuid, name, value, null, null, ArrayList<FieldDataOption>())

    constructor(uuid : String, fieldUuid: String, name: String, value: String, operator: Operator, filterOperator: FilterOperator?)
            : this(uuid, fieldUuid, name, value, operator, filterOperator, ArrayList<FieldDataOption>())

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