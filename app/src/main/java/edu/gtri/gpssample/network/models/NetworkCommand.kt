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
        const val NetworkRequestRulesCommand        = 1008
        const val NetworkRequestRulesResponse       = 1009
        const val NetworkRequestFiltersCommand      = 1010
        const val NetworkRequestFiltersResponse     = 1011

        fun unpack( byteArray: ByteArray, length: Int ) : NetworkCommand
        {
            return Json.decodeFromString<NetworkCommand>( String( byteArray, 0, length ) )
        }
    }
}