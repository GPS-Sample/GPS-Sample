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
        fun didReceiveTCPMessage( message: String )
        fun clientConnected(socket: Socket)

    }

    val serverListening : Boolean
        get() = _serverListening
    private var port = 51234

    private var enabled = true
    private var serverSocket: ServerSocket? = null
    private var socketStarted : Boolean = false
    private var _serverListening : Boolean = false
    fun stopReceiving()
    {
        enabled = false

        if (serverSocket != null)
        {
            serverSocket!!.close()
        }
    }

    fun  createSocket() : Boolean
    {
        try {
            serverSocket = ServerSocket(port)
            serverSocket?.let {
                socketStarted = true
            } ?: run{
                socketStarted = false
            }
        }catch (ex : Exception)
        {
            socketStarted = false
        }
        return socketStarted
    }
    suspend fun beginListening( inetAddress: InetAddress, delegate: TCPServerDelegate )
    {
        if(socketStarted && serverSocket != null)
        {
            withContext(Dispatchers.IO)
            {
                try {
                    Log.d( "xxx", "waiting for TCP connections on $inetAddress:$port...")
                    _serverListening = true
                    while( enabled )
                    {
                        val socket = serverSocket!!.accept()

                        Log.d( "xxx", "accepted connection from ${socket.inetAddress.toString()}" )

                        Thread {

                            handleClient(socket,delegate)
                        }.start()
                    }
                    _serverListening = false
                }
                catch( ex: Exception )
                {
                    Log.d( "xxx", ex.stackTraceToString())
                    _serverListening = false
                }

                Log.d( "xxx", "stopped waiting for TCP connections")
            }

        }

    }

    fun shutdown()
    {
        enabled = false

        if (serverSocket != null)
        {
            serverSocket!!.close()
        }
    }
    private fun handleClient(socket: Socket, delegate: TCPServerDelegate)
    {
        try
        {

            delegate.clientConnected(socket)
            val bytes : ByteArray = ByteArray(6)

            while(socket.isConnected)
            {
                socket.inputStream.read(bytes)
                Log.d("xxxxxx", "the bytes ${String(bytes)}")
            //    val message = BufferedReader(InputStreamReader(socket.inputStream)).readLine()

                delegate.didReceiveTCPMessage( String(bytes) )
            }

        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
            Log.d( "xxx", ex.localizedMessage!! )
        }

        Log.d( "xxx", "stopped waiting for TCP messages")
    }
}