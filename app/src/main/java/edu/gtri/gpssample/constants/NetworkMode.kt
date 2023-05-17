package edu.gtri.gpssample.constants

enum class NetworkMode(val format : String) {
    None("None"),
    NetworkHotspot("Network Hotspot"),
    NetworkClient("Network CLient"),
    NetworkServer("Network Server"),

}

object NetworkModeConverter {

}