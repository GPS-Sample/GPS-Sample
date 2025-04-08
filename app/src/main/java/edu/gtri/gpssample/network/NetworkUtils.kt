/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.network

import android.util.Log
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList

object NetworkUtils {
    const val kHeartbeatInterval : Long = 1000
    const val kNumRetries = 200

    fun readFully( byteArray: ByteArray, totalNeeded: Int, socket: Socket, tag: String ) : Int
    {
        var totalRead = 0

        while( totalRead < totalNeeded)
        {
//            Log.d( "xxx", "${tag}: waiting on read..." )
            val numRead = socket.inputStream.read( byteArray, totalRead, totalNeeded - totalRead )
//            Log.d( "xxx", "${tag}: read ${numRead}")
            if (numRead < 0)
            {
                break
            }

            totalRead += numRead
//            Log.d( "xxx", "${tag}: needed/read = ${totalNeeded} / ${totalRead}")
        }

//        Log.d( "xxx", "${tag}: done reading.")

        return totalRead
    }

    fun readToEnd(fullSize : Int, actualRead : Int, socket : Socket, payloadArray : ByteArray ) : Boolean
    {
        var retryCounter : Int = 0
        if(actualRead < fullSize)
        {
            var left = fullSize - actualRead
            val leftover = ByteArray(left)
            // maybe read one byte at a time?
            var offset = 0
            var read = 0
            var leftAfterReading = left
            
            while(left - offset != 0)
            {
                if(retryCounter < kNumRetries)
                {
                    read = socket.inputStream.read(leftover, offset,leftAfterReading)
                    offset += read
                    leftAfterReading -= read
                    retryCounter += 1
                }
                else
                {
                    return false
                }
            }

            for ( i in (fullSize - left) until fullSize)
            {
                val test = i - (fullSize - left)
                if(test > -1)
                {
                    payloadArray[i] = leftover[i - (fullSize - left)]
                }
            }
        }

        return true
    }

    fun getInetAddresses(): ArrayList<InetAddress>
    {
        val list = ArrayList<InetAddress>()

        try
        {
            val networkInterfaces: Enumeration<NetworkInterface> = NetworkInterface.getNetworkInterfaces()

            while (networkInterfaces.hasMoreElements())
            {
                val networkInterface: NetworkInterface = networkInterfaces.nextElement()
                val inetAddresses: Enumeration<InetAddress> = networkInterface.getInetAddresses()

                while (inetAddresses.hasMoreElements())
                {
                    val inetAddress: InetAddress = inetAddresses.nextElement()

                    if (!inetAddress.isLoopbackAddress() && !inetAddress.hostAddress!!.contains(":"))
                    {
                        list.add( inetAddress )
                    }
                }
            }
        }
        catch (ex: Exception)
        {
            Log.d("xxx", ex.stackTraceToString())
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