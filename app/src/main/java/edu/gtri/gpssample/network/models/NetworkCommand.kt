package edu.gtri.gpssample.network.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkCommand( var command: Int, var uuid: String, var message: String )
{
    companion object 
    {
        const val NetworkUserCommand = 1001
        const val NetworkRequestConfigCommand = 1002
        const val NetworkConfigResponseCommand = 1003
    }
}