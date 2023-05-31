package edu.gtri.gpssample.network

import android.util.Log
import edu.gtri.gpssample.network.models.TCPHeader
import edu.gtri.gpssample.network.models.TCPMessage
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
        fun didReceiveTCPMessage( message: TCPMessage, socket: Socket )

        fun didDisconnect(socket : Socket)
        fun clientConnected(socket: Socket)

    }
    private var clientSockets : ArrayList<Socket> = ArrayList()
    val serverListening : Boolean
        get() = _serverListening
    private var port = 51234

    private var enabled = true
    private var serverSocket: ServerSocket? = null
    private var socketStarted : Boolean = false
    private var _serverListening : Boolean = false
    private var _sockets : ArrayList<Socket> = ArrayList()

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
                    enabled = true
                    while( enabled )
                    {
                        val socket = serverSocket!!.accept()
                        _sockets.add(socket)
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
            for(socket in _sockets)
            {
                socket.close()
            }
            _sockets.clear()
            serverSocket!!.close()
            serverSocket = null
            _serverListening = false

        }
    }
    private fun handleClient(socket: Socket, delegate: TCPServerDelegate)
    {
        try
        {
           // clientSockets.add(socket)
            delegate.clientConnected(socket)
            val headerArray : ByteArray = ByteArray(TCPHeader.size)

            while(socket.isConnected)
            {
                val success = socket.inputStream.read(headerArray,0, TCPHeader.size )
                if(success == -1)
                {
                    break
                }else if(success < TCPHeader.size)
                {
                    NetworkUtils.readToEnd(TCPHeader.size, success, socket, headerArray)
                }
                val header = TCPHeader.fromByteArray(headerArray)
                header?.let {header ->

                    // if we get here, the key is valid
                    val payloadArray = ByteArray(header.payloadSize)
                    val read = socket.inputStream.read(payloadArray, 0, header.payloadSize)

                    // TCP read may not read the requested bytes.
                    // handle if not
                    if(read < header.payloadSize)
                    {
                        NetworkUtils.readToEnd(header.payloadSize, read, socket, payloadArray)

                    }
                    val payload = String(payloadArray)
                    val tcpMessage = TCPMessage(header, payload)
                    delegate.didReceiveTCPMessage( tcpMessage, socket )
                }
            }
            socket.close()
            delegate.didDisconnect(socket)

        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
            Log.d( "xxx", ex.localizedMessage!! )
        }

        Log.d( "xxx", "stopped waiting for TCP messages")
    }


}