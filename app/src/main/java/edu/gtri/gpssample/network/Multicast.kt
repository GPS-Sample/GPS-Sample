package edu.gtri.gpssample.network

import android.util.Log
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket

const val kMulticastAddress = "228.5.6.7"
const val kMulticastPort = 6789

object Multicast {

    private var mcastSocket : MulticastSocket? = null
    private var mcastGroup : InetAddress? = null

    fun join()
    {
        try {
            mcastGroup = InetAddress.getByName(kMulticastAddress)
            mcastSocket = MulticastSocket(kMulticastPort)
            mcastSocket?.joinGroup(mcastGroup);
        }catch(e : Exception)
        {
            Log.d("Exception", e.toString())
        }

    }

    fun broadcast(message : ByteArray)
    {
        mcastGroup?.let{mcastGroup ->
            val datagram : DatagramPacket = DatagramPacket(message, message.size,
                mcastGroup, kMulticastPort)
            mcastSocket?.let{socket ->
                socket.send(datagram)
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
        }

    }
}