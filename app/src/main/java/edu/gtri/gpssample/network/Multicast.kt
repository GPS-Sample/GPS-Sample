/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.network

import android.util.Log
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

const val kMulticastAddress = "228.5.6.7"
const val kMulticastPort = 6789

object Multicast {

    private var mcastSocket : MulticastSocket? = null
    private var mcastGroup : InetAddress? = null

    private var mcastJoined = false
    fun join(networkInterface: NetworkInterface)
    {
        try {
            mcastSocket?.close()
            mcastGroup = InetAddress.getByName(kMulticastAddress)
            mcastSocket = MulticastSocket(kMulticastPort)
            mcastSocket?.networkInterface = networkInterface
            mcastSocket?.joinGroup(InetSocketAddress(mcastGroup, kMulticastPort),networkInterface)
            mcastJoined = true
        }catch(e : Exception)
        {
            Log.d("Exception", e.stackTraceToString())
        }

    }

    fun broadcast(message : ByteArray)
    {
        if(mcastJoined)
        {
            mcastGroup?.let{mcastGroup ->
                val datagram : DatagramPacket = DatagramPacket(message, message.size,
                    mcastGroup, kMulticastPort)
                mcastSocket?.let{socket ->
                    socket.send(datagram)
                }
            }
        }

    }

    fun receive(receiver : DatagramPacket)
    {
        mcastSocket?.receive(receiver)
    }

    fun disconnect()
    {
        mcastGroup?.let{
            mcastSocket?.leaveGroup(it)
            mcastSocket?.close()
            mcastSocket = null
            mcastJoined = false
        }

    }
}