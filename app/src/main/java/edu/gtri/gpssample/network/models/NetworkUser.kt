package edu.gtri.gpssample.network.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkUser( var name: String, var uuid: String, var isOnline: Boolean )
{
}