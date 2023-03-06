package edu.gtri.gpssample.database.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Rectangle (
    var id : Int? = null,
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
    constructor(uuid: String, topLeft_lat: Double, topLeft_lon: Double, topRight_lat: Double,
                topRight_lon: Double, botRight_lat: Double, botRight_lon: Double, botLeft_lat: Double,
                botLeft_lon: Double) : this(null, uuid, topLeft_lat, topLeft_lon, topRight_lat,
                topRight_lon, botRight_lat, botRight_lon, botLeft_lat, botLeft_lon)
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