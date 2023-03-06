package edu.gtri.gpssample.database.models

import android.graphics.Point
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class Circle (
    var uuid: String,
    var lat: Double,
    var lon: Double,
    var radius: Double)
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : Circle
        {
            return Json.decodeFromString<Circle>( message )
        }
    }
}