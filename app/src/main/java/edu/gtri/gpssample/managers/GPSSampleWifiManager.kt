/*
 * Copyright (C) 2022-2025 Georgia Tech Research Institute
 * SPDX-License-Identifier: GPL-3.0-or-later
 *
 * See the LICENSE file for the full license text.
*/

package edu.gtri.gpssample.managers

import android.annotation.SuppressLint
import android.app.Activity
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.network.NetworkUtils
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList

class GPSSampleWifiManager//( val fragment: Fragment )
{
    interface GPSSampleWifiManagerDelegate
    {
        fun didCreateHotspot(success : Boolean, serverIp : InetAddress?)
        fun didStartHotspot( ssid: String, pass: String )
    }
    interface HotspotDelegate
    {
        fun didCreateHotspot(success : Boolean, serverIp : InetAddress?)

    }

    private var serverInetAddress: InetAddress? = null
    private var broadcastInetAddress: InetAddress? = null
//    private val udpBroadcaster: UDPBroadcaster = UDPBroadcaster()
    private var localOnlyHotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null
    private var _hotspotSSID : MutableLiveData<String> = MutableLiveData("TEST")
    private var _hotspotSSIDPassword : MutableLiveData<String> = MutableLiveData("")
    private var _hotspotIP : MutableLiveData<String> = MutableLiveData("")

    var hotspotSSID : LiveData<String> = _hotspotSSID
    var hotspotSSIDPassword : LiveData<String> = _hotspotSSIDPassword
    var hotspotIP : LiveData<String> = _hotspotIP

    private var _activity : Activity? = null
    private var _hotspotStarted : Boolean = false

    var activity : Activity?
    get() = _activity
    set(value)
    {
         _activity = value
    }

    fun stopHotSpot()
    {
       // udpBroadcaster.closeSocket()

        localOnlyHotspotReservation?.let {localOnlyHotspotReservation ->
            localOnlyHotspotReservation.close()

        }
        _hotspotStarted = false
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.O)
    fun startHotSpot(delegate: HotspotDelegate)
    {
        if(!_hotspotStarted)
        {
            try
            {
                val oldInetAdresses = NetworkUtils.getInetAddresses()
                val wifiManager = activity!!.applicationContext.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager

                wifiManager.startLocalOnlyHotspot( object : WifiManager.LocalOnlyHotspotCallback()
                {
                    override fun onFailed(reason: Int)
                    {
                        super.onFailed(reason)
                    }

                    override fun onStopped()
                    {
                        super.onStopped()
                    }

                    override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation)
                    {
                        super.onStarted(reservation)
                        localOnlyHotspotReservation = reservation
                        val wifiConfiguration = reservation.wifiConfiguration

                        val ssid = wifiConfiguration!!.SSID //reservation.softApConfiguration.ssid
                        val pass = wifiConfiguration!!.preSharedKey //reservation.softApConfiguration.passphrase

                        _hotspotSSID.value = ssid //.postValue(ssid)
                        _hotspotSSIDPassword.value = pass
                        val newInetAddresses = NetworkUtils.getInetAddresses()

                        if (oldInetAdresses.isEmpty())
                        {
                            if (!newInetAddresses.isEmpty())
                            {
                                serverInetAddress = newInetAddresses[0]
                            }
                        }
                        else
                        {
                            for (oldAddr in oldInetAdresses)
                            {
                                for (newAddr in newInetAddresses)
                                {
                                    if (newAddr.hostAddress!!.equals( oldAddr.hostAddress ))
                                    {
                                        continue
                                    }
                                    else
                                    {
                                        serverInetAddress = newAddr
                                        break;
                                    }
                                }
                            }
                        }

                        serverInetAddress?.let { serverInetAddress ->
                            serverInetAddress.hostAddress?.let { hostAddress ->
                                val components = hostAddress.split(".")
                                val broadcast_address = components[0] + "." + components[1] + "." + components[2] + ".255"
                                broadcastInetAddress = InetAddress.getByName( broadcast_address )
                                _hotspotIP.value = hostAddress
                                _hotspotIP.postValue(hostAddress)
                                _hotspotStarted = true
                                delegate.didCreateHotspot(true, serverInetAddress )
                            }
                        }
                    }
                }, Handler())
            }
            catch(e: Exception) {
                Log.d( "xxx", e.stackTraceToString())
                delegate.didCreateHotspot(false, null)
                throw(e)
            }

        }
        else // the hotspot is up
        {
            serverInetAddress?.let {serverInetAddress ->
                delegate.didCreateHotspot(true, serverInetAddress)
            }?: run {
                delegate.didCreateHotspot(false, null)
            }
        }
    }

    fun getWifiApIpAddresses2(): ArrayList<InetAddress> {
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
}