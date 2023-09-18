package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class FieldDataOption(
    var id : Int? = null,
    var value: Boolean )
{
    constructor(value: Boolean) : this( null, value )

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