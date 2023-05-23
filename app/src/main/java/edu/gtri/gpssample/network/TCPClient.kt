package edu.gtri.gpssample.network

import android.util.Log
import edu.gtri.gpssample.network.models.TCPHeader
import edu.gtri.gpssample.network.models.TCPMessage
import java.net.Socket
const val kSocketTimeout = 5000
const val kTCPPort = 51234
class TCPClient
{
    interface TCPClientDelegate
    {
        fun sentData(data : String)
        fun connectionString(connection : String)
    }
    //var port = kTCPPort
    // var port = 80
    var socket : Socket? = null

    fun connect(inetAddress: String, delegate : TCPClientDelegate) : Boolean
    {
        try {
            // if you call connect and we're connected, we kill the socket and start over!
            socket?.let{socket ->
                socket.close()
            }
            socket = null
            socket = Socket( inetAddress, kTCPPort )
            if(socket != null)
            {
                socket!!.soTimeout = kSocketTimeout
                return true
            }
        }catch (ex: Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
            delegate.connectionString("Connection failed to ${inetAddress}")
        }
        return false
    }

    fun sendMessage(inetAddress: String, message : TCPMessage, delegate: TCPClientDelegate) : TCPMessage?
    {
        try
        {
            socket?.let {socket ->
                socket.outputStream.write( message.toByteArray())
                delegate.sentData("TCP message: $message to $inetAddress")
                socket.outputStream.flush()

                // build the response from the server

                val headerArray : ByteArray = ByteArray(TCPHeader.size)
                socket.inputStream.read(headerArray)
                val header = TCPHeader.fromByteArray(headerArray)
                header?.let { header ->
                    // if we get here, the key is valid
                    val payloadArray = ByteArray(header.payloadSize)
                    socket.inputStream.read(payloadArray)

                    val payload = String(payloadArray)

                    return TCPMessage(header, payload)
                }
                return null
            }
        }
        catch (ex: Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
            delegate.connectionString("Connection failed to ${inetAddress}")
        }
        return null
    }

    fun write( inetAddress: String, message: String, delegate : TCPClientDelegate )
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

    fun shutdown()
    {
        socket?.let {
            it.close()
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