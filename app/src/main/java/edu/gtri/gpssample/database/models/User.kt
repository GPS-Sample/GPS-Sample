package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class User(
    var id: Int,
    var uuid: String,
    var name: String,
    var pin: Int,
    var role: String,
    var recoveryQuestion: String,
    var recoveryAnswer: String,
    var isOnline: Boolean )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object
    {
        fun unpack( message: String ) : User
        {
            return Json.decodeFromString<User>( message )
        }
    }
}