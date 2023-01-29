package edu.gtri.gpssample.network.models

import kotlinx.serialization.Serializable

@Serializable
data class NetworkConfig( var name: String, var dateFormat: String, var timeFormat: String, var distanceFormat: String, var minGspPrecision: Int )
{
}