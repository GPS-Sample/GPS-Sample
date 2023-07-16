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
    var id : Int? = null,
    var uuid : String,
    var fieldId : Int,
    var enumerationItemId : Int,
    var name : String,
    var type : FieldType,
    var textValue : String,
    var numberValue : Double?,
    var dateValue : Long?,
    var dropdownIndex : Int?,
    var checkbox1 : Boolean,
    var checkbox2 : Boolean,
    var checkbox3 : Boolean,
    var checkbox4 : Boolean)
{
    constructor( fieldId: Int, enumerationItemId: Int ) : this( null, UUID.randomUUID().toString(),
        fieldId,enumerationItemId, "", FieldType.None,  "", null, null,
        null, false, false, false, false )

    constructor( fieldId: Int, enumerationItemId: Int, name : String, type : FieldType, textValue: String,
                 numberValue: Double?, dateValue: Long?, dropdownIndex: Int?, checkbox1: Boolean, checkbox2: Boolean,
                 checkbox3: Boolean, checkbox4: Boolean ) :
            this( null, UUID.randomUUID().toString(), fieldId, enumerationItemId, name, type,  textValue,
                numberValue, dateValue, dropdownIndex, checkbox1, checkbox2, checkbox3, checkbox4 )

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