package edu.gtri.gpssample.database.models

import edu.gtri.gpssample.network.models.NetworkCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class User(
    var id : Int? = null,
    var uuid: String,
    var name: String,
    var pin: Int,
    var role: String,
    var recoveryQuestion: String,
    var recoveryAnswer: String,
    var isOnline: Boolean )
{
    constructor(uuid: String, name: String, pin: Int, role: String,
                recoveryQuestion: String, recoveryAnswer: String, isOnline: Boolean) :
                this(null,uuid, name, pin, role, recoveryQuestion, recoveryAnswer, isOnline)
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