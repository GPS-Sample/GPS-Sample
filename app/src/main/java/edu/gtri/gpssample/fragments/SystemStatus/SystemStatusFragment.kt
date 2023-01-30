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
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import edu.gtri.gpssample.BuildConfig
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Field
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.databinding.FragmentSystemStatusBinding
import edu.gtri.gpssample.network.UDPBroadcaster
import edu.gtri.gpssample.network.models.*
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
    private var studyId = 0
    private var configId = 0
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
        setHasOptionsMenu( true )

        _binding = FragmentSystemStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val role_arg = getArguments()?.getString(Key.kRole.toString());

        val role = Role.valueOf(role_arg!!)

        binding.titleTextView.text = role.toString()

        val configs = DAO.configDAO.getConfigs()
        if (configs.isNotEmpty())
        {
            binding.configCheckBox.isChecked = true

            val studies = DAO.studyDAO.getStudies()
            if (studies.isNotEmpty())
            {
                binding.studyCheckBox.isChecked = true

                val fields = DAO.fieldDAO.getFields(studies[0].id)
                if (fields.isNotEmpty())
                {
                    binding.fieldsCheckBox.isChecked = true
                }
            }
        }

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

            DAO.configDAO.deleteAllConfigs()

            binding.configCheckBox.isChecked = false
            binding.studyCheckBox.isChecked = false
            binding.fieldsCheckBox.isChecked = false
            binding.shapeFilesCheckBox.isChecked = false

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
            val networkCommand = NetworkCommand( NetworkCommand.NetworkRequestFieldsCommand, user!!.uuid, "" )
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
            studyId = jsonObject.getInt( Key.kStudyId.toString() )
            configId = jsonObject.getInt( Key.kConfigId.toString() )

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

                udpBroadcaster.beginTransmitting( myInetAddress, broadcastInetAddress, networkCommandMessage)
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
        val user = (activity!!.application as? MainApplication)?.user

        if (networkCommand.uuid == user!!.uuid)
        {
            when( networkCommand.command )
            {
                NetworkCommand.NetworkRequestConfigResponse ->
                {
                    val networkConfig = Json.decodeFromString<NetworkConfig>(networkCommand.message)

                    val config = Config()
                    config.name = networkConfig.name
                    config.dateFormat = DateFormat.valueOf( networkConfig.dateFormat )
                    config.timeFormat = TimeFormat.valueOf( networkConfig.timeFormat )
                    config.distanceFormat = DistanceFormat.valueOf( networkConfig.distanceFormat )
                    config.minGpsPrecision = networkConfig.minGspPrecision
                    config.id = DAO.configDAO.createConfig( config )

                    activity!!.runOnUiThread {
                        binding.configCheckBox.isChecked = true
                    }
                }

                NetworkCommand.NetworkRequestStudyResponse ->
                {
                    val networkStudy = Json.decodeFromString<NetworkStudy>(networkCommand.message)

                    val study = Study()
                    study.name = networkStudy.name
                    study.isValid = networkStudy.isValid
                    study.configId = networkStudy.configId
                    study.id = DAO.studyDAO.createStudy( study )

                    activity!!.runOnUiThread {
                        binding.studyCheckBox.isChecked = true
                    }
                }

                NetworkCommand.NetworkRequestFieldsResponse ->
                {
                    val networkFields = Json.decodeFromString<NetworkFields>(networkCommand.message)

                    for (networkField in networkFields.fields)
                    {
                        val field = Field()
                        field.studyId = networkField.studyId
                        field.name = networkField.name
                        field.type = FieldType.valueOf( networkField.type )
                        field.pii = networkField.pii
                        field.required = networkField.required
                        field.integerOnly = networkField.integerOnly
                        field.date = networkField.date
                        field.time = networkField.time
                        field.option1 = networkField.option1
                        field.option2 = networkField.option2
                        field.option3 = networkField.option3
                        field.option4 = networkField.option4
                        field.id = DAO.fieldDAO.createField( field )
                    }

                    activity!!.runOnUiThread {
                        binding.fieldsCheckBox.isChecked = true
                    }
                }

                NetworkCommand.NetworkRequestShapeFileResponse ->
                {
                    activity!!.runOnUiThread {
                        binding.shapeFilesCheckBox.isChecked = true
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        super.onCreateOptionsMenu(menu, inflater)

        inflater.inflate(R.menu.menu_system_status, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.action_manage_configurations ->
            {
                val bundle = Bundle()
                bundle.putString( Key.kRole.toString(), Role.Admin.toString())
                findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment, bundle)
            }
        }

        return false
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        _binding = null
    }
}