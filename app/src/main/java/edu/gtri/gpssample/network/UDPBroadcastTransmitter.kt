package edu.gtri.gpssample.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UDPBroadcastTransmitter {

    var port = 61234
    private var datagramSocket: DatagramSocket? = null

    fun transmit( myInetAddress: InetAddress, serverInetAddress: InetAddress, bytes: ByteArray )
    {
        if (datagramSocket == null)
        {
            datagramSocket = DatagramSocket(port,myInetAddress)
            datagramSocket!!.broadcast = true
            datagramSocket!!.reuseAddress = true
        }

        datagramSocket!!.send( DatagramPacket(bytes, bytes.size, serverInetAddress, port ))
    }
}