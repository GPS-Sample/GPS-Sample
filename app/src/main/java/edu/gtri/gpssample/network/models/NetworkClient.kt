package edu.gtri.gpssample.network.models
import java.net.Socket

data class NetworkClient(val dataSocket : Socket, val id : String, val name : String ) {
    var timer : Int = 0
}