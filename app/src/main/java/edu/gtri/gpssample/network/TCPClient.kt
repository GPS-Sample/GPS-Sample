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

    suspend fun write( inetAddress: String, message: String )
    {
        withContext(Dispatchers.IO)
        {
            try
            {
                Log.d( "xxx", "open TCP connection to $inetAddress:$port...")

                val socket = Socket( inetAddress, port )

                Log.d( "xxx", "write TCP message: $message to $inetAddress" )

                socket.outputStream.write( message.toByteArray())

                socket.close()
            }
            catch (ex: Exception)
            {
                Log.d( "xxx", ex.stackTraceToString())
            }
        }
    }
}