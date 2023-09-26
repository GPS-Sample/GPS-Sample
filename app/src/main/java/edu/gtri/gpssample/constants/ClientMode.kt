package edu.gtri.gpssample.constants

enum class ClientMode(val format : String) {
    None("None"),
    Configuration("Configuration Client"),
    EnumerationTeam("Enumeration Team Client"),
    CollectionTeam( "Connection Team Client")

}

object ClientModeConverter {

}