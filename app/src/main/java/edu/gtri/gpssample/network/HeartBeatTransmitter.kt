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
    var datagramSocket: DatagramSocket? = null;

    private var enabled = false

    fun stopTransmitting()
    {
        enabled = false
        datagramSocket?.close()
    }

    fun isEnabled() : Boolean
    {
        return enabled
    }

    suspend fun beginTransmitting( myInetAddress: InetAddress, serverInetAddress: InetAddress, bytes: ByteArray )
    {
        Log.d( "xxx", "begin transmitting heartbeat on $myInetAddress:$port to ${serverInetAddress}:$port" )

        val backgroundResult = withContext(Dispatchers.IO)
        {
            if (datagramSocket == null)
            {
                datagramSocket = DatagramSocket(port,myInetAddress)
                datagramSocket!!.broadcast = true
                datagramSocket!!.reuseAddress = true
            }

            val datagramPacket = DatagramPacket(bytes, bytes.size, serverInetAddress, port)

            enabled = true

            delay(1000)

            while( enabled )
            {
                try {
                    datagramSocket!!.send( datagramPacket )
                    delay(1000)
                }
                catch( ex: Exception )
                {
                    Log.d( "xxx", ex.stackTraceToString())
                    stopTransmitting()
                }
            }

            Log.d( "xxx", "finished transmitting data" )
        }
    }
}