package edu.gtri.gpssample.database.models

import android.util.Log
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
    var id : Int? = null,
    var uuid : String,
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
    constructor( field: Field ) : this( null, UUID.randomUUID().toString(), field, "", FieldType.None,
        "", null, null, null, null, ArrayList<FieldDataOption>())

    constructor( field: Field, blockNumber: Int ) : this( null, UUID.randomUUID().toString(), field, "", FieldType.None,
        "", null, null, null, blockNumber, ArrayList<FieldDataOption>())

    constructor( field: Field,  name : String, type : FieldType, textValue: String,
                 numberValue: Double, dateValue: Long, dropdownIndex: Int, blockNumber: Int ) :
            this( null, UUID.randomUUID().toString(), field, name, type,  textValue,
                numberValue, dateValue, dropdownIndex, blockNumber, ArrayList<FieldDataOption>())

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