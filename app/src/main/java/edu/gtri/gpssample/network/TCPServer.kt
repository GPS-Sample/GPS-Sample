/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.network

import android.util.Log
import edu.gtri.gpssample.network.TCPClient.TCPClientDelegate
import edu.gtri.gpssample.network.models.TCPHeader
import edu.gtri.gpssample.network.models.TCPMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataInputStream
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
        try
        {
            serverSocket = ServerSocket(port)

            serverSocket?.let {
                socketStarted = true
            } ?: run{
                socketStarted = false
            }
        }
        catch (ex : Exception)
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
                try
                {
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
//                    Log.d( "xxx", ex.stackTraceToString())
                    _serverListening = false
                }

                Log.d( "xxx", "Server: stopped waiting for TCP connections")
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
            delegate.clientConnected(socket)

            val headerArray = ByteArray(TCPHeader.SIZE)

            while(socket.isConnected)
            {
                val numRead = NetworkUtils.readFully( headerArray, TCPHeader.SIZE, socket, "Server" )

                if (numRead != TCPHeader.SIZE)
                {
                    break
                }

                val header = TCPHeader.fromByteArray(headerArray)

                header?.let { header ->
                    val payloadArray = ByteArray(header.payloadSize.toInt())

                    if(header.payloadSize > 0)
                    {
                        NetworkUtils.readFully( payloadArray, header.payloadSize.toInt(), socket, "Server" )
                    }

                    val tcpMessage = TCPMessage(header, payloadArray)
                    delegate.didReceiveTCPMessage( tcpMessage, socket )
                }
            }

            socket.close()
            delegate.didDisconnect(socket)
        }
        catch( ex: Exception )
        {
            Log.d( "xxx", ex.stackTraceToString())
        }

        Log.d( "xxx", "Server: stopped waiting for TCP messages")
    }


    fun sendDataRequestMessage( socket: Socket, message : TCPMessage ) : TCPHeader?
    {
        try
        {
            socket.outputStream.write( message.toByteArray())
            socket.outputStream.flush()

            val headerArray = ByteArray(TCPHeader.SIZE )

            if (NetworkUtils.readFully( headerArray, TCPHeader.SIZE, socket, "Client" ) == TCPHeader.SIZE)
            {
                return TCPHeader.fromByteArray(headerArray)
            }
        }
        catch (ex: Exception)
        {
            Log.d( "xxx", ex.stackTraceToString())
        }

        return null
    }
}