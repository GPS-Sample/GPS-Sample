package edu.gtri.gpssample.fragments.SystemStatus

import android.content.Context
import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.net.*
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.whenStarted
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.constants.ResultCode
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.FragmentSystemStatusBinding
import edu.gtri.gpssample.network.models.NetworkCommand
import edu.gtri.gpssample.network.models.NetworkUser
import edu.gtri.gpssample.network.HeartBeatTransmitter
import edu.gtri.gpssample.network.TCPClient
import edu.gtri.gpssample.network.UDPBroadcastTransmitter
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList

class SystemStatusFragment : Fragment()
{
    private lateinit var serverTCPAddress: String
    private lateinit var serverUDPInetAddress: InetAddress
    private var _binding: FragmentSystemStatusBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SystemStatusViewModel
    private lateinit var myInetAddress: InetAddress
    private val heartBeatTransmitter = HeartBeatTransmitter()
    private val udpBroadcastTransmitter = UDPBroadcastTransmitter()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(SystemStatusViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        _binding = FragmentSystemStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val role_arg = getArguments()?.getString(Key.kRole.toString());

        val role = Role.valueOf(role_arg!!)

        binding.titleTextView.text = role.toString()

        binding.fragmentRootLayout.setOnClickListener {
            if (BuildConfig.DEBUG) {
                Toast.makeText(activity!!.applicationContext, this.javaClass.simpleName, Toast.LENGTH_SHORT).show()
            }
        }

        val oldWifiAdresses = getWifiApIpAddresses()

        Log.d( "xxx", "searching for old WiFi addresses..." )
        for (address in oldWifiAdresses)
        {
            myInetAddress = address
            Log.d( "xxx", myInetAddress!!.hostAddress )
        }

        binding.wifiImageButton.setOnClickListener {
//            serverTCPAddress = "172.20.10.13"
//            serverUDPInetAddress = InetAddress.getByName("172.20.10.13")
//            beginTransmittingHeartbeat()

            val intent = Intent(activity!!, CameraXLivePreviewActivity::class.java)
            startActivityForResult( intent, 0 )
        }

        binding.configImageButton.setOnClickListener {
            if (::serverTCPAddress.isInitialized)
            {
                val networkCommand = NetworkCommand( NetworkCommand.NetworkRequestConfigCommand, "" )
                val networkCommandMessage = Json.encodeToString( networkCommand )
                lifecycleScope.launch {
                    TCPClient().write( serverTCPAddress, networkCommandMessage )
                }
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == ResultCode.BarcodeScanned.value)
        {
            val payload = data!!.getStringExtra( Key.kPayload.toString())

            val jsonObject = JSONObject( payload );

            Log.d( "xxx", jsonObject.toString(2))

            val ssid = jsonObject.getString( Key.kSSID.toString() )
            val pass = jsonObject.getString( Key.kPass.toString() )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                try {
                    val wifiConfig = WifiConfiguration()
                    wifiConfig.SSID = "\"" + ssid + "\""
                    wifiConfig.preSharedKey = "\"" + pass + "\""

                    var wifiManager = activity!!.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager?

                    val netId = wifiManager!!.addNetwork(wifiConfig)
                    wifiManager.disconnect()
                    wifiManager.enableNetwork(netId, true)
                    wifiManager.reconnect()

                    Handler().postDelayed({
                        wifiManager = activity!!.applicationContext.getSystemService(WIFI_SERVICE) as WifiManager?

                        serverTCPAddress = intToInetAddress(wifiManager!!.dhcpInfo.serverAddress)!!.toString().substring(1 )
                        val myAddress = intToInetAddress(wifiManager!!.dhcpInfo.ipAddress)!!.toString().substring(1 )
                        val components = serverTCPAddress.split(".")
                        val server_udp_address = components[0] + "." + components[1] + "." + components[2] + ".255"

                        myInetAddress = InetAddress.getByName( myAddress )
                        serverUDPInetAddress = InetAddress.getByName( server_udp_address )

                        if (!heartBeatTransmitter.isEnabled())
                        {
                            beginTransmittingHeartbeat()
                        }
                    }, 1000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            else
            {
                val builder = WifiNetworkSpecifier.Builder()
                builder.setSsid( ssid );
                builder.setWpa2Passphrase( pass )
                val wifiNetworkSpecifier = builder.build()

                val networkRequestBuilder = NetworkRequest.Builder()
                networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier)

                val networkRequest = networkRequestBuilder.build()

                val connectivityManager = activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.requestNetwork( networkRequest, networkCallback )
            }
        }
    }

    fun intToInetAddress(address: Int): InetAddress?
    {
        val addressBytes = byteArrayOf(
            (0xff and address).toByte(),
            (0xff and (address shr 8)).toByte(),
            (0xff and (address shr 16)).toByte(),
            (0xff and (address shr 24)).toByte()
        )

        try
        {
            return InetAddress.getByAddress(addressBytes)
        }
        catch (e: Exception) {}

        return null
    }

    private fun beginTransmittingHeartbeat()
    {
        lifecycleScope.launchWhenStarted {
            whenStarted {
                binding.wifiCheckBox.isChecked = true

                val sharedPreferences = activity!!.application.getSharedPreferences( "default", 0 )
                val userName = sharedPreferences.getString( Key.kUserName.toString(), "" )

                val networkUser = NetworkUser( userName!!, UUID.randomUUID().toString(), true )
                val networkUserMessage = Json.encodeToString( networkUser )
                val networkCommand = NetworkCommand( NetworkCommand.NetworkUserCommand, networkUserMessage )
                val networkCommandMessage = Json.encodeToString( networkCommand )

                heartBeatTransmitter.beginTransmitting( myInetAddress, serverUDPInetAddress, networkCommandMessage.toByteArray())
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback()
    {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities)
        {
            super.onCapabilitiesChanged(network, networkCapabilities)

            if (networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_VALIDATED) && !heartBeatTransmitter.isEnabled())
            {
                beginTransmittingHeartbeat()
            }
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties)
        {
            super.onLinkPropertiesChanged(network, linkProperties)

            serverTCPAddress = linkProperties.dhcpServerAddress.toString().substring(1)
            // linkProperties.linkAddresses[0] is the IPV6 address
            val myAddress = linkProperties.linkAddresses[1].toString().substring(0, linkProperties.linkAddresses[1].toString().length-3)

            val components = serverTCPAddress.split(".")
            val server_udp_address = components[0] + "." + components[1] + "." + components[2] + ".255"

            myInetAddress = InetAddress.getByName( myAddress )
            serverUDPInetAddress = InetAddress.getByName( server_udp_address )
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}