/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.network

import android.util.Log
import edu.gtri.gpssample.network.models.TCPHeader
import edu.gtri.gpssample.network.models.TCPMessage
import java.io.DataInputStream
import java.net.Socket

const val kSocketTimeout = 10000
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
            socket?.close()

            socket = Socket( inetAddress, kTCPPort )

            socket?.let {
                it.soTimeout = kSocketTimeout
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

    fun sendMessage(inetAddress: String, message : TCPMessage, delegate: TCPClientDelegate, waitForResponse: Boolean = true) : TCPMessage?
    {
        try
        {
            socket?.let {socket ->
                socket.outputStream.write( message.toByteArray())
                delegate.sentData("TCP message: $message to $inetAddress")
                socket.outputStream.flush()

                Log.d( "xxx", "Client: wrote ${message.toByteArray()!!.size}")

                if (waitForResponse)
                {
                    val headerArray = ByteArray(TCPHeader.size )

                    if (NetworkUtils.readFully( headerArray, TCPHeader.size, socket, "Client" ) == TCPHeader.size)
                    {
                        val header = TCPHeader.fromByteArray(headerArray)
                        if (header != null)
                        {
                            val payloadArray = ByteArray(header.payloadSize)

                            if(header.payloadSize > 0)
                            {
                                NetworkUtils.readFully( payloadArray, header.payloadSize, socket, "Client" )
                            }

                            val payload = String(payloadArray)

                            return TCPMessage(header, payload)
                        }
                    }
                }
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