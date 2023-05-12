package edu.gtri.gpssample.constants

enum class NetworkStatus(val format : String) {
    None("None"),
    NetworkConnected("Network Connected"),
    NetworkCreated("Network Created"),
    NetworkError("Network Error"),
    DataReceived("Data Received"),

}

object NetworkStatusConverter {

}