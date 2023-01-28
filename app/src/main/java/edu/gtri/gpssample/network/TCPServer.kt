package edu.gtri.gpssample.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class TCPServer
{
    interface TCPServerDelegate
    {
        fun didReceiveMessage( message: String )
    }

    var port = 51234
    private var enabled = true
    private var serverSocket: ServerSocket? = null

    fun stopReceiving()
    {
        enabled = false

        if (serverSocket != null)
        {
            serverSocket!!.close()
        }
    }

    suspend fun beginListening( inetAddress: InetAddress, delegate: TCPServerDelegate )
    {
        withContext(Dispatchers.IO)
        {
            try {
                serverSocket = ServerSocket( port )

                Log.d( "xxx", "waiting for TCP connections on $inetAddress:$port...")

                while( enabled )
                {
                    val socket = serverSocket!!.accept()

                    Log.d( "xxx", "accepted connection from ${socket.inetAddress.toString()}" )

                    Thread {
                        handleClient(socket,delegate)
                    }.start()
                }
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
            }

            Log.d( "xxx", "stopped waiting for TCP connections")
        }
    }

    private fun handleClient(socket: Socket, delegate: TCPServerDelegate)
    {
        try
        {
            Log.d( "xxx", "waiting for TCP messages on ${socket.inetAddress}:$port...")

            val message = BufferedReader(InputStreamReader(socket.inputStream)).readLine()

            Log.d( "xxx", "handleClient.received: ${message}")

            delegate.didReceiveMessage( message )
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
            Log.d( "xxx", ex.localizedMessage!! )
        }

        Log.d( "xxx", "stopped waiting for TCP messages")
    }
}