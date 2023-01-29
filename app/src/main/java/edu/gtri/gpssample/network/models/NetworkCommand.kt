package edu.gtri.gpssample.network.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkCommand( var command: Int, var uuid: String, var message: String )
{
    companion object 
    {
        const val NetworkUserCommand                = 1001
        const val NetworkRequestConfigCommand       = 1002
        const val NetworkRequestConfigResponse      = 1003
        const val NetworkRequestStudyCommand        = 1004
        const val NetworkRequestStudyResponse       = 1005
        const val NetworkRequestFieldCommand        = 1006
        const val NetworkRequestFieldResponse       = 1007
        const val NetworkRequestShapeFileCommand    = 1008
        const val NetworkRequestShapeFileResponse   = 1009
    }
}