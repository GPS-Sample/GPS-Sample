package edu.gtri.gpssample.network

import android.util.Log
import edu.gtri.gpssample.constants.NetworkStatus
import edu.gtri.gpssample.network.models.NetworkCommand
import edu.gtri.gpssample.network.models.TCPMessage
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

    fun connect(inetAddress: String, delegate : TCPCLientDelegate) : Boolean
    {
        try {
            // if you call connect and we're connected, we kill the socket and start over!
            socket?.let{socket ->
                socket.close()
            }
            socket = null
            socket = Socket( inetAddress, 51234 )
            if(socket != null)
            {
                return true
            }
        }catch (ex: Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
            delegate.connectionString("Connection failed to ${inetAddress}")
        }
        return false
    }

    fun sendMessage(inetAddress: String, message : TCPMessage, delegate: TCPCLientDelegate) : NetworkStatus
    {
        try
        {
            socket?.let {socket ->
                socket.outputStream.write( message.toByteArray())
                delegate.sentData("TCP message: $message to $inetAddress")
                socket.outputStream.flush()
                return NetworkStatus.CommandSent
            }
        }
        catch (ex: Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
            delegate.connectionString("Connection failed to ${inetAddress}")

        }
        return NetworkStatus.CommandError
    }

    fun write( inetAddress: String, message: String, delegate : TCPCLientDelegate )
    {
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
                socket.outputStream.flush()
                //socket.close()
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