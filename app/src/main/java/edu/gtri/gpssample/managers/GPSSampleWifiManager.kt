package edu.gtri.gpssample.managers

import android.net.wifi.WifiManager
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import edu.gtri.gpssample.network.UDPBroadcaster
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList

class GPSSampleWifiManager( val fragment: Fragment )
{
    interface GPSSampleWifiManagerDelegate
    {
        fun didStartHotspot( ssid: String, pass: String )
    }

    private var serverInetAddress: InetAddress? = null
    private var broadcastInetAddress: InetAddress? = null
    private val udpBroadcaster: UDPBroadcaster = UDPBroadcaster()
    private var localOnlyHotspotReservation: WifiManager.LocalOnlyHotspotReservation? = null

    fun stopHotSpot()
    {
        udpBroadcaster.closeSocket()

        localOnlyHotspotReservation?.close()
    }

    fun startHotSpot()
    {
        try
        {
            Log.d( "xxx", "startLocalOnlyHotspot" )

            val oldWifiAdresses = getWifiApIpAddresses()

            val wifiManager = fragment.activity!!.applicationContext.getSystemService(AppCompatActivity.WIFI_SERVICE) as WifiManager

            wifiManager.startLocalOnlyHotspot(object : WifiManager.LocalOnlyHotspotCallback()
            {
                override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation)
                {
                    super.onStarted(reservation)

                    localOnlyHotspotReservation = reservation

                    val wifiConfiguration = reservation.wifiConfiguration

                    val ssid = wifiConfiguration!!.SSID //reservation.softApConfiguration.ssid
                    val pass = wifiConfiguration!!.preSharedKey //reservation.softApConfiguration.passphrase

                    Log.d( "xxx", "ssid = " + ssid );
                    Log.d( "xxx", "pass = " + pass );

                    val delegate = fragment as GPSSampleWifiManagerDelegate
                    delegate.didStartHotspot( ssid, pass )

                    val newWifiAddresses = getWifiApIpAddresses()

                    if (oldWifiAdresses.isEmpty())
                    {
                        if (!newWifiAddresses.isEmpty())
                        {
                            serverInetAddress = newWifiAddresses[0]
                        }
                    }
                    else
                    {
                        for (oldAddr in oldWifiAdresses)
                        {
                            for (newAddr in newWifiAddresses)
                            {
                                if (newAddr.hostAddress.equals( oldAddr.hostAddress ))
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

                    if (serverInetAddress != null)
                    {
                        val components = serverInetAddress!!.hostAddress.split(".")
                        val broadcast_address = components[0] + "." + components[1] + "." + components[2] + ".255"
                        broadcastInetAddress = InetAddress.getByName( broadcast_address )

                        Log.d( "xxx", broadcast_address )

                        fragment.lifecycleScope.launch {
                            val delegate = fragment as UDPBroadcaster.UDPBroadcasterDelegate
                            udpBroadcaster.beginReceiving( serverInetAddress!!, broadcastInetAddress!!, delegate )
                        }
                    }
                }
            }, Handler())
        } catch(e: Exception) {
            Log.d( "xxx", e.stackTraceToString())
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
}