package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Rectangle (
    var uuid: String,
    var topLeft_lat: Double,
    var topLeft_lon: Double,
    var topRight_lat: Double,
    var topRight_lon: Double,
    var botRight_lat: Double,
    var botRight_lon: Double,
    var botLeft_lat: Double,
    var botLeft_lon: Double)
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : Rectangle
        {
            return Json.decodeFromString<Rectangle>( message )
        }
    }
}