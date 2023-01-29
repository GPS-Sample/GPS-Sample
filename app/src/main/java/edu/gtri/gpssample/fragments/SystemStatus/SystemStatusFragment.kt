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
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.Key
import edu.gtri.gpssample.constants.ResultCode
import edu.gtri.gpssample.constants.Role
import edu.gtri.gpssample.databinding.FragmentSystemStatusBinding
import edu.gtri.gpssample.network.models.NetworkCommand
import edu.gtri.gpssample.network.models.NetworkUser
import edu.gtri.gpssample.network.UDPBroadcaster
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import kotlin.collections.ArrayList

class SystemStatusFragment : Fragment(), UDPBroadcaster.UDPBroadcasterDelegate
{
    private lateinit var broadcastInetAddress: InetAddress
    private var _binding: FragmentSystemStatusBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SystemStatusViewModel
    private lateinit var myInetAddress: InetAddress
    private val udpBroadcaster = UDPBroadcaster()

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

        binding.wifiImageButton.setOnClickListener {
            val intent = Intent(activity!!, CameraXLivePreviewActivity::class.java)
            startActivityForResult( intent, 0 )
        }

        binding.configImageButton.setOnClickListener {
            val user = (activity!!.application as? MainApplication)?.user
            val networkCommand = NetworkCommand( NetworkCommand.NetworkRequestConfigCommand, user!!.uuid, "" )
            val networkCommandMessage = Json.encodeToString( networkCommand )

            binding.configCheckBox.isChecked = false

            lifecycleScope.launch {
                udpBroadcaster.transmit( myInetAddress, broadcastInetAddress, networkCommandMessage )
            }
        }

        binding.studyImageButton.setOnClickListener {
            val user = (activity!!.application as? MainApplication)?.user
            val networkCommand = NetworkCommand( NetworkCommand.NetworkRequestStudyCommand, user!!.uuid, "" )
            val networkCommandMessage = Json.encodeToString( networkCommand )

            binding.studyCheckBox.isChecked = false

            lifecycleScope.launch {
                udpBroadcaster.transmit( myInetAddress, broadcastInetAddress, networkCommandMessage )
            }
        }

        binding.fieldsImageButton.setOnClickListener {
            val user = (activity!!.application as? MainApplication)?.user
            val networkCommand = NetworkCommand( NetworkCommand.NetworkRequestFieldCommand, user!!.uuid, "" )
            val networkCommandMessage = Json.encodeToString( networkCommand )

            binding.fieldsCheckBox.isChecked = false

            lifecycleScope.launch {
                udpBroadcaster.transmit( myInetAddress, broadcastInetAddress, networkCommandMessage )
            }
        }

        binding.shapeFilesImageButton.setOnClickListener {
            val user = (activity!!.application as? MainApplication)?.user
            val networkCommand = NetworkCommand( NetworkCommand.NetworkRequestShapeFileCommand, user!!.uuid, "" )
            val networkCommandMessage = Json.encodeToString( networkCommand )

            binding.shapeFilesCheckBox.isChecked = false

            lifecycleScope.launch {
                udpBroadcaster.transmit( myInetAddress, broadcastInetAddress, networkCommandMessage )
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

                        val serverAddress = intToInetAddress(wifiManager!!.dhcpInfo.serverAddress)!!.toString().substring(1 )
                        val myAddress = intToInetAddress(wifiManager!!.dhcpInfo.ipAddress)!!.toString().substring(1 )
                        val components = serverAddress.split(".")
                        val broadcast_address = components[0] + "." + components[1] + "." + components[2] + ".255"

                        myInetAddress = InetAddress.getByName( myAddress )
                        broadcastInetAddress = InetAddress.getByName( broadcast_address )

                        beginReceiving()
                        beginTransmittingHeartbeat()
                    }, 1000)
                } catch (e: Exception) {
                    Log.d( "xxx", e.stackTraceToString())
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

    private fun beginReceiving()
    {
        if (!udpBroadcaster.receiverIsEnabled())
        {
            lifecycleScope.launch {
                udpBroadcaster.beginReceiving( myInetAddress, this@SystemStatusFragment )
            }
        }
    }

    private fun beginTransmittingHeartbeat()
    {
        if (!udpBroadcaster.transmitterIsEnabled())
        {
            lifecycleScope.launch {
                binding.wifiCheckBox.isChecked = true

                val user = (activity!!.application as? MainApplication)?.user

                val networkUser = NetworkUser( user!!.name, user!!.uuid, true )
                val networkUserMessage = Json.encodeToString( networkUser )
                val networkCommand = NetworkCommand( NetworkCommand.NetworkUserCommand, user!!.uuid, networkUserMessage )
                val networkCommandMessage = Json.encodeToString( networkCommand )

                udpBroadcaster.beginTransmitting( myInetAddress, broadcastInetAddress, networkCommandMessage.toByteArray())
            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback()
    {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities)
        {
            super.onCapabilitiesChanged(network, networkCapabilities)

            if (networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            {
                beginReceiving()
                beginTransmittingHeartbeat()
            }
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties)
        {
            super.onLinkPropertiesChanged(network, linkProperties)

            val serverAddress = linkProperties.dhcpServerAddress.toString().substring(1)
            // linkProperties.linkAddresses[0] is the IPV6 address
            val myAddress = linkProperties.linkAddresses[1].toString().substring(0, linkProperties.linkAddresses[1].toString().length-3)

            val components = serverAddress.split(".")
            val server_udp_address = components[0] + "." + components[1] + "." + components[2] + ".255"

            myInetAddress = InetAddress.getByName( myAddress )
            broadcastInetAddress = InetAddress.getByName( server_udp_address )
        }
    }

    override fun didReceiveDatagramPacket( datagramPacket: DatagramPacket )
    {
        val message = String( datagramPacket.data, 0, datagramPacket.length )

        val networkCommand = Json.decodeFromString<NetworkCommand>( message )

        when( networkCommand.command )
        {
            NetworkCommand.NetworkRequestConfigResponse ->
            {
                activity!!.runOnUiThread {
                    binding.configCheckBox.isChecked = true
                }
            }
            NetworkCommand.NetworkRequestStudyResponse ->
            {
                activity!!.runOnUiThread{
                    binding.studyCheckBox.isChecked = true
                }
            }
            NetworkCommand.NetworkRequestFieldResponse ->
            {
                activity!!.runOnUiThread{
                    binding.fieldsCheckBox.isChecked = true
                }
            }
            NetworkCommand.NetworkRequestShapeFileResponse ->
            {
                activity!!.runOnUiThread{
                    binding.shapeFilesCheckBox.isChecked = true
                }
            }
        }
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}