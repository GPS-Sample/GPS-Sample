/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.constants.FieldType
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

// NOTE!!
// This class defines the Field answers and is a part of an EnumerationItem

@Serializable
data class FieldData (
    var uuid : String,
    var creationDate : Long,
    var field : Field?,
    var name : String,
    var type : FieldType,
    var textValue : String,
    var numberValue : Double?,
    var dateValue : Long?,
    var dropdownIndex : Int?,
    var blockNumber : Int?,
    var fieldDataOptions : ArrayList<FieldDataOption>)
{
    constructor( creationDate: Long, field: Field ) : this( UUID.randomUUID().toString(), creationDate, field, "", FieldType.None,
        "", null, null, null, null, ArrayList<FieldDataOption>())

    constructor( field: Field, blockNumber: Int ) : this( UUID.randomUUID().toString(), Date().time, field, "", FieldType.None,
        "", null, null, null, blockNumber, ArrayList<FieldDataOption>())

    constructor( creationDate: Long, field: Field, blockNumber: Int ) : this( UUID.randomUUID().toString(), creationDate, field, "", FieldType.None,
        "", null, null, null, blockNumber, ArrayList<FieldDataOption>())

    constructor( field: Field,  name : String, type : FieldType, textValue: String,
                 numberValue: Double, dateValue: Long, dropdownIndex: Int, blockNumber: Int ) :
            this( UUID.randomUUID().toString(), Date().time, field, name, type,  textValue,
                numberValue, dateValue, dropdownIndex, blockNumber, ArrayList<FieldDataOption>())

    fun equals( other: FieldData ) : Boolean
    {
        if (this.uuid == other.uuid &&
            this.name == other.name &&
            this.type == other.type &&
            this.textValue == other.textValue &&
            this.numberValue == other.numberValue &&
            this.dateValue == other.dateValue &&
            this.dropdownIndex == other.dropdownIndex &&
            this.blockNumber == other.blockNumber)
        {
            return true
        }

        return false
    }

    fun doesNotEqual( fieldData: FieldData ): Boolean
    {
        return !this.equals( fieldData )
    }

    fun copy() : FieldData
    {
        return unpack(pack())
    }

    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( string: String ) : FieldData
        {
            return Json.decodeFromString<FieldData>( string )
        }
    }
}