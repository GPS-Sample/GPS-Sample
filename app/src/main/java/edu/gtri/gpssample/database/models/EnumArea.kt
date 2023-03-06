package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class EnumArea (
    var uuid: String,
    var config_uuid: String,
    var name: String,
    var shape: String,
    var shape_uuid: String)
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : EnumArea
        {
            return Json.decodeFromString<EnumArea>( message )
        }
    }
}