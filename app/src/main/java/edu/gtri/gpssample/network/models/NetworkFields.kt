package edu.gtri.gpssample.network.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkFields( var fields: List<NetworkField> )
{
}