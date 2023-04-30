package edu.gtri.gpssample.fragments.SystemStatusNotUsed

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
import edu.gtri.gpssample.R
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.barcode_scanner.CameraXLivePreviewActivity
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.databinding.FragmentSystemStatusBinding
import edu.gtri.gpssample.network.UDPBroadcaster
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.InetAddress

class SystemStatusFragment : Fragment(), UDPBroadcaster.UDPBroadcasterDelegate
{
    private lateinit var user: User
    private lateinit var role: String
    private lateinit var broadcastInetAddress: InetAddress
    private lateinit var myInetAddress: InetAddress

    private var enum_area_id = -1
    private var _binding: FragmentSystemStatusBinding? = null
    private val binding get() = _binding!!
    private var udpBroadcaster: UDPBroadcaster? = null

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle? ): View?
    {
        setHasOptionsMenu( true )

        _binding = FragmentSystemStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?)
    {
        super.onViewCreated(view, savedInstanceState)

        binding.wifiView.setOnClickListener {}
        binding.configView.setOnClickListener {}
        binding.studyView.setOnClickListener {}
        binding.fieldsView.setOnClickListener {}
        binding.rulesView.setOnClickListener {}
        binding.filtersView.setOnClickListener {}

        (activity!!.application as? MainApplication)?.user?.let { user ->
            this.user = user
        }

        if (!this::user.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "User is undefined", Toast.LENGTH_SHORT).show()
            return
        }

        arguments?.getString(Keys.kRole.toString())?.let { role ->
            this.role = role
        }

        if (!this::role.isInitialized)
        {
            Toast.makeText(activity!!.applicationContext, "Fatal! Missing required parameter: role.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.titleTextView.text = role.toString()

        binding.requestWifiButton.setOnClickListener {

            if (!connected())
            {
                val intent = Intent(activity!!, CameraXLivePreviewActivity::class.java)
                startActivityForResult( intent, 0 )
            }
        }

        binding.requestConfigButton.setOnClickListener {
        }

        binding.requestStudyButton.setOnClickListener {
        }

        binding.requestFieldsButton.setOnClickListener {
        }

        binding.requestRulesButton.setOnClickListener {
        }

        binding.requestFiltersButton.setOnClickListener {
        }

        binding.requestEnumAreaButton.setOnClickListener {
        }

        binding.requestTeamButton.setOnClickListener {
        }

        binding.nextButton.setOnClickListener {

            udpBroadcaster?.closeSocket()
            binding.wifiCheckBox.isChecked = false

//            if (team_uuid.isNotEmpty())
//            {
//                val bundle = Bundle()
//                bundle.putString( Keys.kTeam_uuid.toString(), team_uuid )
//                bundle.putString( Keys.kStudy_uuid.toString(), study_uuid )
//                bundle.putInt( Keys.kEnumArea_id.toString(), enum_area_id )
//                findNavController().navigate(R.id.action_navigate_to_PerformEnumerationFragment, bundle)
//            }
//            else if (enum_area_id > 0)
//            {
//                val bundle = Bundle()
//                bundle.putString( Keys.kStudy_uuid.toString(), study_uuid )
//                bundle.putInt( Keys.kEnumArea_id.toString(), enum_area_id )
//                findNavController().navigate(R.id.action_navigate_to_ManageEnumerationTeamsFragment, bundle)
//            }
        }
    }

    override fun onResume()
    {
        super.onResume()

        (activity!!.application as? MainApplication)?.currentFragment = FragmentNumber.SystemStatusFragment.value.toString() + ": " + this.javaClass.simpleName

        binding.configCheckBox.isChecked = false
        binding.studyCheckBox.isChecked = false
        binding.fieldsCheckBox.isChecked = false
        binding.rulesCheckBox.isChecked = false
        binding.filtersCheckBox.isChecked = false
        binding.enumAreaCheckBox.isChecked = false

        //val configs = DAO.configDAO.getConfigs()

//        if (configs.isNotEmpty())
//        {
//            binding.configCheckBox.isChecked = true
//
//            val config = configs[0]
//            config_uuid = config.uuid
//
//           // val studies = DAO.studyDAO.getStudies()
//
////            if (studies.isNotEmpty())
////            {
////                binding.studyCheckBox.isChecked = true
////
////                val study = studies[0]
////                study_uuid = study.uuid
////
//////                val fields = DAO.fieldDAO.getFields(study.uuid)
//////                binding.fieldsCheckBox.isChecked = fields.isNotEmpty()
//////
//////                val rules = DAO.ruleDAO.getRules(study.uuid)
//////                binding.rulesCheckBox.isChecked = rules.isNotEmpty()
////
////            //    val filters = DAO.filterDAO.getFilters(study.uuid)
////             //   binding.filtersCheckBox.isChecked = filters.isNotEmpty()
////
////                val enumAreas = DAO.enumAreaDAO.getEnumAreas(config.uuid)
////
////                if (enumAreas.isNotEmpty())
////                {
////                    binding.enumAreaCheckBox.isChecked = true
////
////                    val enumArea = enumAreas[0]
////                    enum_area_uuid = enumArea.uuid
////                }
////            }
//        }
    }

    private fun connected() : Boolean
    {
        return udpBroadcaster != null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == ResultCode.BarcodeScanned.value)
        {
            val payload = data!!.getStringExtra( Keys.kPayload.toString())

            val jsonObject = JSONObject( payload );

            Log.d( "xxx", jsonObject.toString(2))

            val ssid = jsonObject.getString( Keys.kSSID.toString() )
            val pass = jsonObject.getString( Keys.kPass.toString() )

//            val team_uuid = jsonObject.opt( Keys.kTeam_uuid.toString())

//            study_uuid = jsonObject.getString( Keys.kStudy_uuid.toString() )
//            config_uuid = jsonObject.getString( Keys.kConfig_uuid.toString() )
//            enum_area_uuid = jsonObject.getString( Keys.kEnumArea_uuid.toString() )

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
        lifecycleScope.launch {
            udpBroadcaster?.beginReceiving( myInetAddress, broadcastInetAddress, this@SystemStatusFragment )
        }
    }

    private fun beginTransmittingHeartbeat()
    {
        lifecycleScope.launch {
            binding.wifiCheckBox.isChecked = true

            user.isOnline = true
//            val networkCommand = NetworkCommand( NetworkCommand.NetworkUserRequest, user.uuid, "", "", user.pack() )
//
//            udpBroadcaster?.beginTransmitting( myInetAddress, broadcastInetAddress, networkCommand.pack())
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback()
    {
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities)
        {
            super.onCapabilitiesChanged(network, networkCapabilities)

//            val not_suspended = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
//            val validated = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_VALIDATED)
//            val trusted = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_TRUSTED)
//            val not_restricted = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
//
//            Log.d( "xxx", "not_suspended = ${not_suspended}")
//            Log.d( "xxx", "validated = ${validated}")
//            Log.d( "xxx", "trusted = ${trusted}")
//            Log.d( "xxx", "not_restricted = ${not_restricted}")

            if (udpBroadcaster == null && networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_VALIDATED))
            {
                Log.d( "xxx", "beginTransmittingHeartbeat" )
                udpBroadcaster = UDPBroadcaster()
                beginReceiving()
                beginTransmittingHeartbeat()
            }
        }

        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties)
        {
            super.onLinkPropertiesChanged(network, linkProperties)

//            val serverAddress = linkProperties.dhcpServerAddress.toString().substring(1)
            // linkProperties.linkAddresses[0] is the IPV6 address
            val myAddress = linkProperties.linkAddresses[1].toString().substring(0, linkProperties.linkAddresses[1].toString().length-3)

            val components = myAddress.split(".")
            val server_udp_address = components[0] + "." + components[1] + "." + components[2] + ".255"

            myInetAddress = InetAddress.getByName( myAddress )
            broadcastInetAddress = InetAddress.getByName( server_udp_address )
        }
    }

    override fun didReceiveDatagramPacket( datagramPacket: DatagramPacket )
    {
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
                findNavController().navigate(R.id.action_navigate_to_ManageConfigurationsFragment)
            }
        }

        return false
    }

    override fun onDestroyView()
    {
        super.onDestroyView()

        udpBroadcaster?.closeSocket()

        _binding = null
    }
}