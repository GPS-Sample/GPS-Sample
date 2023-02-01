package edu.gtri.gpssample.network.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class NetworkCommand( var command: Int, var uuid: String, var message: String )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object 
    {
        const val NetworkUserCommand                = 1001
        const val NetworkRequestConfigCommand       = 1002
        const val NetworkRequestConfigResponse      = 1003
        const val NetworkRequestStudyCommand        = 1004
        const val NetworkRequestStudyResponse       = 1005
        const val NetworkRequestFieldsCommand       = 1006
        const val NetworkRequestFieldsResponse      = 1007
        const val NetworkRequestShapeFileCommand    = 1008
        const val NetworkRequestShapeFileResponse   = 1009

        fun unpack( message: String ) : NetworkCommand
        {
            return Json.decodeFromString<NetworkCommand>( message )
        }

        fun unpack( byteArray: ByteArray, length: Int ) : NetworkCommand
        {
            return unpack( String( byteArray, 0, length ))
        }
    }
}