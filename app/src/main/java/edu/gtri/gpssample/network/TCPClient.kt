package edu.gtri.gpssample.network

import android.util.Log
import edu.gtri.gpssample.network.models.TCPHeader
import edu.gtri.gpssample.network.models.TCPMessage
import java.io.DataInputStream
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

    var socket : Socket? = null

    fun connect(inetAddress: String, delegate : TCPClientDelegate) : Boolean
    {
        try
        {
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
        }
        catch (ex: Exception)
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

                val headerArray = ByteArray(TCPHeader.size )

                val dataInputStream = DataInputStream( socket.inputStream )
                dataInputStream.readFully( headerArray, 0, TCPHeader.size )

                val header = TCPHeader.fromByteArray(headerArray)
                header?.let { header ->
                    val payloadArray = ByteArray(header.payloadSize)

                    if(header.payloadSize > 0)
                    {
                        dataInputStream.readFully( payloadArray, 0, header.payloadSize )
                    }

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

    fun shutdown()
    {
        socket?.let {
            it.close()

        }
        socket =  null
    }
}