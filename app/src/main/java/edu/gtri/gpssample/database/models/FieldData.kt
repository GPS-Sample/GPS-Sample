package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FieldData (
    var id : Int? = null,
    var fieldId : Int,
    var enumDataId : Int,
    var response1 : String,
    var response2 : String,
    var response3 : String,
    var response4 : String)
{
    constructor() : this( null, 0, 0, "", "", "", "" )

    constructor( fieldId: Int, enumDataId: Int, response1: String, response2: String, response3: String, response4: String ) :
            this( null, fieldId, enumDataId, response1, response2, response3, response4 )

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