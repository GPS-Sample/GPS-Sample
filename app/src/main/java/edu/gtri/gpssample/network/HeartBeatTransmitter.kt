package edu.gtri.gpssample.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class HeartBeatTransmitter
{
    var port = 61234
    var enabled = true

    suspend fun transmit( myInetAddress: InetAddress, serverInetAddress: InetAddress, bytes: ByteArray )
    {
        Log.d( "xxx", "begin transmitting heartbeat on " + myInetAddress + " to " + serverInetAddress )

        val backgroundResult = withContext(Dispatchers.Default)
        {
            val datagramSocket = DatagramSocket(port,myInetAddress)
            datagramSocket!!.broadcast = true
            datagramSocket!!.reuseAddress = true
            val datagramPacket = DatagramPacket(bytes, bytes.size, serverInetAddress, port)

            while (enabled)
            {
                datagramSocket!!.send( datagramPacket )
                delay(1000)
            }
        }
        withContext(Dispatchers.Main) {
            Log.d( "xxx", "finished transmitting data" )
        }
    }
}