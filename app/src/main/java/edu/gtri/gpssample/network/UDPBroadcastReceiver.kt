package edu.gtri.gpssample.network

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class UDPBroadcastReceiver {

    var port = 61234
    var enabled = true

    suspend fun beginListening( inetAddress: InetAddress )
    {
        val backgroundResult = withContext(Dispatchers.Default)
        {
            val datagramSocket = DatagramSocket(port)
            datagramSocket.broadcast = true
            datagramSocket.reuseAddress = true

            while (enabled)
            {
                var bytes: ByteArray? = null
                val buf = ByteArray(4096)
                val datagramPacket = DatagramPacket(buf, buf.size)

                Log.d( "xxx", "waiting for data on " + inetAddress.hostAddress )

                datagramSocket.receive(datagramPacket)

                Log.d( "xxx", "received: " + datagramPacket.length )
            }
        }
        withContext(Dispatchers.Main) {
            Log.d( "xxx", "stopped waiting for data" )
        }
    }
}