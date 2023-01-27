package edu.gtri.gpssample.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkCommand( var command: Int, var message: String )
{
}