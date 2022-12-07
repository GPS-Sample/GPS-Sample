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

    var port = 61234
    var enabled = true
    lateinit var delegate: UDPBroadcastReceiverDelegate
//    var datagramPacketLiveData: MutableLiveData<DatagramPacket> = MutableLiveData<DatagramPacket>()

    suspend fun beginListening( inetAddress: InetAddress, delegate: UDPBroadcastReceiverDelegate )
    {
        val backgroundResult = withContext(Dispatchers.Default)
        {
            val datagramSocket = DatagramSocket(port)
            datagramSocket.broadcast = true
            datagramSocket.reuseAddress = true

            Log.d( "xxx", "waiting for data on " + inetAddress.hostAddress )

            while (enabled)
            {
                var bytes: ByteArray? = null
                val buf = ByteArray(4096)
                val datagramPacket = DatagramPacket(buf, buf.size)

                datagramSocket.receive(datagramPacket)

                delegate.didReceiveDatagramPacket( datagramPacket )

//                this.launch {
//                    datagramPacketLiveData.value = datagramPacket
//                }
            }
        }
        withContext(Dispatchers.Main) {
            Log.d( "xxx", "stopped waiting for data" )
        }
    }
}