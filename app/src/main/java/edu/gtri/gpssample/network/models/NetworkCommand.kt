package edu.gtri.gpssample.network.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// uuid is either the source or destination, based on whether the command is a command or response

@Serializable
data class NetworkCommand( var command: Int, var uuid: String, val parm1: String, val parm2: String, var message: String )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object 
    {
        const val NetworkUserCommand                    = 1001
        const val NetworkUserCommandResponse            = 2001

        const val NetworkRequestConfigCommand           = 1002
        const val NetworkRequestConfigResponse          = 2002

        const val NetworkRequestStudyCommand            = 1003
        const val NetworkRequestStudyResponse           = 2003

        const val NetworkRequestFieldsCommand           = 1004
        const val NetworkRequestFieldsResponse          = 2004

        const val NetworkRequestRulesCommand            = 1005
        const val NetworkRequestRulesResponse           = 2005

        const val NetworkRequestFiltersCommand          = 1006
        const val NetworkRequestFiltersResponse         = 2006

        const val NetworkRequestFilterRulesCommand      = 1007
        const val NetworkRequestFilterRulesResponse     = 2007

        fun unpack( byteArray: ByteArray, length: Int ) : NetworkCommand
        {
            return Json.decodeFromString<NetworkCommand>( String( byteArray, 0, length ) )
        }
    }
}