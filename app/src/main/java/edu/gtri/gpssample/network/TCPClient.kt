package edu.gtri.gpssample.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class TCPClient
{
    interface TCPCLientDelegate
    {
        fun sentData(data : String)
        fun connectionString(connection : String)
    }
    var port = 51234
    // var port = 80
    var socket : Socket? = null
    fun write( inetAddress: String, message: String, delegate : TCPCLientDelegate )
    {
//        val testIp = "192.168.43.1"
//        val testPort = 51234

        // val testIp = "192.168.1.100"
        //val testIp = "www.google.com"
        //val testPort = 80
       // val testIp = "192.168.5.169"

        //val testIp = "gtri.gatech.edu"
     //   val testPort = 8080
        // withContext(Dispatchers.IO)
        //{
        try
        {
            Log.d( "xxx", "open TCP connection to $inetAddress:51234...")
            if (socket == null)
            {
                socket = Socket( inetAddress, 51234 )
                //socket = Socket(InetAddress.getByName(testIp),testPort)
                Log.d("xx","did we ")
            }
            socket?.let {socket ->
                delegate.connectionString("Connected to ${inetAddress}")
                Log.d( "xxx", "write TCP message: $message to $inetAddress" )

                socket.outputStream.write( message.toByteArray())

                delegate.sentData("TCP message: $message to $inetAddress")

                socket.close()
            }

        }
        catch (ex: Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
            delegate.connectionString("Connection failed to ${inetAddress}")
        }
    }
//    suspend fun write( inetAddress: String, message: String )
//    {
//        withContext(Dispatchers.IO)
//        {
//            try
//            {
//                Log.d( "xxx", "open TCP connection to $inetAddress:$port...")
//
//                val socket = Socket( inetAddress, port )
//
//                Log.d( "xxx", "write TCP message: $message to $inetAddress" )
//
//                socket.outputStream.write( message.toByteArray())
//
//                socket.close()
//            }
//            catch (ex: Exception)
//            {
//                Log.d( "xxx", ex.stackTraceToString())
//            }
//        }
//    }
}