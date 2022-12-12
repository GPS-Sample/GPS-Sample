package edu.gtri.gpssample.network

import android.app.Activity
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UDPBroadcastReceiver
{
    interface UDPBroadcastReceiverDelegate
    {
        fun didReceiveDatagramPacket( datagramPacket: DatagramPacket )
    }

    private var port = 61234
    private var enabled = true
    private lateinit var datagramSocket: DatagramSocket
    private lateinit var delegate: UDPBroadcastReceiverDelegate

    fun stopReceiving()
    {
        enabled = false
        datagramSocket.close()
    }

    fun isEnabled() : Boolean
    {
        return enabled
    }

    suspend fun beginListening( inetAddress: InetAddress, delegate: UDPBroadcastReceiverDelegate )
    {
        this.delegate = delegate

        val backgroundResult = withContext(Dispatchers.Default)
        {
            datagramSocket = DatagramSocket(port)
            datagramSocket.broadcast = true
            datagramSocket.reuseAddress = true

            Log.d( "xxx", "waiting for data on " + inetAddress.hostAddress )

            while (enabled)
            {
                try {
                    val buf = ByteArray(4096)
                    val datagramPacket = DatagramPacket(buf, buf.size)
                    datagramSocket.receive(datagramPacket)

                    delegate.didReceiveDatagramPacket( datagramPacket )
                }
                catch (ex: Exception)
                {
                    ex.printStackTrace().toString()
                    enabled = false
                }
            }

            Log.d( "xxx", "stopped waiting for data" )
        }
        withContext(Dispatchers.Main) {
        }
    }
}