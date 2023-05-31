package edu.gtri.gpssample.network

import android.util.Log
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList

object NetworkUtils {
    const val kHeartbeatInterval : Long = 1000
    fun readToEnd(fullSize : Int, actualRead : Int, socket : Socket, payloadArray : ByteArray )
    {
        if(actualRead < fullSize)
        {
            val left = fullSize - actualRead
            val leftover = ByteArray(left)
            // maybe read one byte at a time?
            val read = socket.inputStream.read(leftover, 0,left)
            if(read > 0)
            {
                for ( i in (fullSize - left) until fullSize)
                {
                    payloadArray[i] = leftover[i - (fullSize - left)]
                }
            }else
            {
                // throw read error, give up
            }
        }
    }

    fun getWifiApIpAddresses(): ArrayList<InetAddress> {
        val list = ArrayList<InetAddress>()
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                if (intf.getName().contains("wlan")) {
                    val enumIpAddr: Enumeration<InetAddress> = intf.getInetAddresses()
                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress: InetAddress = enumIpAddr.nextElement()
                        if (!inetAddress.isLoopbackAddress()) {
                            val inetAddr = inetAddress.hostAddress!!
                            if (!inetAddr.contains(":")) {
                                list.add( inetAddress)
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("xxx", ex.toString())
        }
        return list
    }

    fun getWifiApInterfaces(): ArrayList<NetworkInterface> {
        val list = ArrayList<NetworkInterface>()
        try {
            val en: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf: NetworkInterface = en.nextElement()
                if (intf.getName().contains("wlan")) {
                    list.add(intf)
//                    val enumIpAddr: Enumeration<InetAddress> = intf.getInetAddresses()
//                    while (enumIpAddr.hasMoreElements()) {
//                        val inetAddress: InetAddress = enumIpAddr.nextElement()
//                        if (!inetAddress.isLoopbackAddress()) {
//                            val inetAddr = inetAddress.hostAddress!!
//                            if (!inetAddr.contains(":")) {
//                                list.add( inetAddress)
//                            }
//                        }
//                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("xxx", ex.toString())
        }
        return list
    }

}