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

    suspend fun transmit( myInetAddress: InetAddress, serverInetAddress: InetAddress, bytes: ByteArray )
    {
        Log.d( "xxx", "begin transmitting data on " + myInetAddress + " to " + serverInetAddress )

        val backgroundResult = withContext(Dispatchers.Default)
        {
            if (datagramSocket == null)
            {
                datagramSocket = DatagramSocket(port,myInetAddress)
                datagramSocket!!.broadcast = true
                datagramSocket!!.reuseAddress = true
            }

            val datagramPacket = DatagramPacket(bytes, bytes.size, serverInetAddress, port)
            datagramSocket!!.send( datagramPacket )
        }
        withContext(Dispatchers.Main) {
            Log.d( "xxx", "finished transmitting data" )
        }
    }
}