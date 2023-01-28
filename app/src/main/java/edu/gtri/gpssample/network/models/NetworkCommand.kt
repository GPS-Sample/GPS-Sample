package edu.gtri.gpssample.network.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkCommand( var command: Int, var message: String )
{
    companion object 
    {
        const val NetworkUserCommand = 1001
        const val NetworkRequestConfigCommand = 1002
        const val NetworkRequestStudyCommand = 1003
        const val NetworkRequestStudyFieldsCommand = 1004
        const val NetworkRequestShapeFilesCommand = 1005
    }
}