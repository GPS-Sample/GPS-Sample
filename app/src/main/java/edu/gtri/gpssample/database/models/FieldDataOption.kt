package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

@Serializable
data class FieldDataOption(
    var id : Int? = null,
    var uuid : String,
    var name : String,
    var value: Boolean )
{
    constructor(name: String, value: Boolean) : this( null, UUID.randomUUID().toString(), name, value )

    fun equals( fieldDataOption: FieldDataOption ) : Boolean
    {
        if (this.id == fieldDataOption.id &&
            this.uuid == fieldDataOption.uuid &&
            this.name == fieldDataOption.name &&
            this.value == fieldDataOption.value )
        {
            return true
        }

        return false
    }

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