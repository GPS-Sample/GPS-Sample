package edu.gtri.gpssample.network.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

// uuid is either the source or destination, based on whether the command is a command or response

@Serializable
data class NetworkCommand( var command: Int, var id: Int, val parm1: String, val parm2: String, var message: String )
{
    fun pack() : String
    {
        return Json.encodeToString( this )
    }

    companion object 
    {
        const val NetworkDeviceRegistrationRequest  = 1000
        const val NetworkDeviceRegistrationResponse = 2000
        const val NetworkUserRequest                = 1001
        const val NetworkUserResponse               = 2001

        const val NetworkConfigRequest              = 1002
        const val NetworkConfigResponse             = 2002

        const val NetworkStudyRequest               = 1003
        const val NetworkStudyResponse              = 2003

        const val NetworkFieldsRequest              = 1004
        const val NetworkFieldsResponse             = 2004

        const val NetworkRulesRequest               = 1005
        const val NetworkRulesResponse              = 2005

        const val NetworkFiltersRequest             = 1006
        const val NetworkFiltersResponse            = 2006

        const val NetworkFilterRulesRequest         = 1007
        const val NetworkFilterRulesResponse        = 2007

        const val NetworkSampleRequest              = 1008
        const val NetworkSampleResponse             = 2008

        const val NetworkNavPlansRequest        = 1009
        const val NetworkNavPlansResponse       = 2009

        const val NetworkEnumAreaRequest        = 1010
        const val NetworkEnumAreaResponse       = 2010
        const val NetworkEnumAreaExport         = 3010
        const val NetworkSampleAreaExport       = 4010

        const val NetworkRectangleRequest       = 1011
        const val NetworkRectangleResponse      = 2011

        const val NetworkTeamRequest            = 1012
        const val NetworkTeamResponse           = 2012

        const val NetworkEnumerationDataResponse = 2013

        fun unpack( byteArray: ByteArray, length: Int ) : NetworkCommand
        {
            return Json.decodeFromString<NetworkCommand>( String( byteArray, 0, length ) )
        }
    }
}