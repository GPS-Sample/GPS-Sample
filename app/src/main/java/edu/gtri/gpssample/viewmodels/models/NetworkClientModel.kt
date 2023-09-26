package edu.gtri.gpssample.viewmodels.models


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.*
import android.net.wifi.*
import android.os.Build
import android.os.Handler
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import edu.gtri.gpssample.application.MainApplication
import edu.gtri.gpssample.constants.*
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.network.*
import edu.gtri.gpssample.network.models.NetworkCommand
import edu.gtri.gpssample.network.models.TCPMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import java.net.InetAddress


private const val kDialogTimeout: Long = 400

class NetworkClientModel : NetworkModel(), TCPClient.TCPClientDelegate {
    override val type = NetworkMode.NetworkClient
    private val client: TCPClient = TCPClient()
    private var heartbeatBroadcasting: Boolean = false


    interface ConfigurationDelegate {
        fun configurationReceived(config: Config)
    }

    interface NetworkConnectDelegate {
        fun didConnect(complete: Boolean)
        fun didSendData(complete: Boolean)
    }

    var clientStarted = false

    // maybe a better way
    var currentEnumArea: EnumArea? = null

    var configurationDelegate: ConfigurationDelegate? = null

    private var _connectDelegate: NetworkConnectDelegate? = null
    var connectDelegate: NetworkConnectDelegate?
        get() = _connectDelegate
        set(value) {
            _connectDelegate = value
        }

    var _networkConnected: MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val networkConnected: LiveData<NetworkStatus>
        get() = _networkConnected

    var _clientRegistered: MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val clientRegistered: LiveData<NetworkStatus>
        get() = _clientRegistered

    var _commandSent: MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val commandSent: LiveData<NetworkStatus>
        get() = _commandSent

    var _dataReceived: MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val dataReceived: LiveData<NetworkStatus>
        get() = _dataReceived

    private var _clientMode: MutableLiveData<ClientMode> = MutableLiveData(ClientMode.None)
    val clientMode: LiveData<ClientMode>
        get() = _clientMode


    private var networkInfo: NetworkInfo? = null

    private var _clientConnectMessage: MutableLiveData<String> = MutableLiveData("")
    private var _clientDataMessage: MutableLiveData<String> = MutableLiveData("")
    var clientConnectMessage: LiveData<String> = _clientConnectMessage
    var clientDataMessage: LiveData<String> = _clientDataMessage


    var _configurationReceived: MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    var configurationReceived: LiveData<NetworkStatus> = _configurationReceived


    override var Activity: Activity?
        get() = _activity
        set(value) {
            _activity = value
        }

    override fun initializeNetwork() {

    }

    fun setClientMode(mode: ClientMode) {
        _clientMode.value = mode
        _clientMode.postValue(mode)
    }

    fun sendRegistration() {
        _activity?.let { activity ->
            val app = (activity.application as MainApplication?)
            app?.user?.let { user ->

                val payload = user.name
                val message = TCPMessage(NetworkCommand.NetworkDeviceRegistrationRequest, payload)
                networkInfo?.let { networkInfo ->
                    val response =
                        client.sendMessage(networkInfo.serverIP, message, this@NetworkClientModel)
                    // validate response
                    response?.let { response ->
                        if (response.command == NetworkCommand.NetworkDeviceRegistrationResponse) {
                            sleep(kDialogTimeout)
                            _clientRegistered.postValue(NetworkStatus.ClientRegistered)

                            when (clientMode.value) {
                                ClientMode.Configuration -> {
                                    sendConfigurationCommand()
                                }
                                ClientMode.EnumerationTeam -> {
                                    sendEnumerationData()
                                }
                                else -> {}
                            }
                            // TODO:  change this to be more generic

                        }
                    }
                }
            }

        }

    }

    private fun sendConfigurationCommand() {
        val message = TCPMessage(NetworkCommand.NetworkConfigRequest, "")
        networkInfo?.let { networkInfo ->
            val response = client.sendMessage(networkInfo.serverIP, message, this)
            response?.let {

                _commandSent.postValue(NetworkStatus.CommandSent)
                if (response.command == NetworkCommand.NetworkConfigResponse) {
                    response.payload?.let { payload ->
                        val config = Config.unpack(payload)

                        Log.d("xxx", payload)

                        if (config == null) {
                            Log.d("xxx", "failed to receive config")
                        }

                        // TODO: put the config in the list of current configs.....
                        config?.let { config ->
                            configurationDelegate?.configurationReceived(config)
                            _dataReceived.postValue(NetworkStatus.DataReceived)

                            sleep(kDialogTimeout)
                            connectDelegate?.didConnect(true)
                        } ?: run {
                            _dataReceived.postValue(NetworkStatus.DataReceivedError)
                            connectDelegate?.didConnect(false)
                        }
                    }
                }
            }
        }
    }

    fun sendEnumerationData() {
        currentEnumArea?.let { enumArea ->
            networkInfo?.let { networkInfo ->
                val payload = enumArea.pack()

                Log.d("xxx", payload)

                val message = TCPMessage(NetworkCommand.NetworkEnumAreaExport, payload)
                val response = client.sendMessage(networkInfo.serverIP, message, this)

                _commandSent.postValue(NetworkStatus.CommandSent)
                // quick sleep to make the UI look better
                sleep(kDialogTimeout)
                connectDelegate?.didSendData(true)
            }

        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun startNetworking(networkInfo: NetworkInfo?): Boolean {
        // connect to wifi
        clientStarted = true
        _networkConnected.postValue(NetworkStatus.None)
        _configurationReceived.postValue(NetworkStatus.None)

        sleep(1500L)
        networkInfo?.let {
            //connectToWifi(networkInfo)


            sleep(1500L)
            _configurationReceived.postValue(NetworkStatus.DataReceived)

            sleep(1000L)
            return true
        }

        return false
    }

    fun intToInetAddress(address: Int): InetAddress? {
        val addressBytes = byteArrayOf(
            (0xff and address).toByte(),
            (0xff and (address shr 8)).toByte(),
            (0xff and (address shr 16)).toByte(),
            (0xff and (address shr 24)).toByte()
        )

        try {
            return InetAddress.getByAddress(addressBytes)
        } catch (e: Exception) {
        }

        return null
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToWifi(networkInfo: NetworkInfo) {
        clientStarted = true
        this.networkInfo = networkInfo
        // if  (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        if (client.socket == null) {
            //if(false)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                try {
                    val connectivityManager =
                        Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                    val wifiConfig = WifiConfiguration()

                    wifiConfig.SSID = "\"" + networkInfo.ssid + "\""
                    wifiConfig.preSharedKey = "\"" + networkInfo.password + "\""

//                    wifiConfig.SSID = "\"" + "Cypress Guest Wifi" + "\""
//                    wifiConfig.preSharedKey = "\"" + "cypresslovesyou" + "\""

//                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
//                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.AuthAlgorithm.OPEN);
//                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.AuthAlgorithm.SHARED);
//                    wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
//                    wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//                    wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//                    wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
//                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
//                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
//                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                    var wifiManager =
                        Activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
                    wifiManager!!.setWifiEnabled(true)


                    val wifiList = wifiManager!!.configuredNetworks
                    for (item in wifiList) {
                        Log.d("XXXX wifi ", "ssid ${item.SSID}")
//                        if(item.SSID != null && item.SSID.equals(ssid)){
//                            return item
//                        }
                    }

//                    val suggestionWpa2 = WifiNetworkSuggestion.Builder()
//                        .setSsid("Cypress Guest Wifi") //SSID name
//                        .setWpa2Passphrase("cypresslovesyou") //password
//                        .build()
//                    val networkSuggestions: ArrayList<WifiNetworkSuggestion> = ArrayList()
//                    networkSuggestions.add(suggestionWpa2)

                    //   wifiManager!!.startScan()


                    if (wifiManager!!.isWifiEnabled) {
                        Log.d("xxxxxx", "WIFI ENABLED")

                        var netId = wifiManager!!.addNetwork(wifiConfig)
                        if (netId == -1) {
                            //Try it again with no quotes in case of hex password
                            wifiConfig.wepKeys[0] = networkInfo.password;
                            netId = wifiManager.addNetwork(wifiConfig);
                        }

                        runBlocking(Dispatchers.Main) {

                            val disconnect = wifiManager.disconnect()
                            if (disconnect) {
                                Log.d("xxxxxx", "${wifiManager?.isWifiEnabled}")
                                wifiManager.enableNetwork(netId, true)

                                val reconnect = wifiManager.reconnect()
                                if (reconnect) {
                                    Log.d("XXXXXXXXXXXXX", "THE INITIAL SSID ${wifiManager!!.connectionInfo.ssid}")
                                    Log.d("XXXXXXXXXXXXX", "THE INITIAL RSSI ${wifiManager!!.connectionInfo.rssi}")
                                    Handler().postDelayed({
                                        val wifiInfo = wifiManager!!.connectionInfo
                                        var ssid = wifiInfo.ssid
                                        var serverAddress =
                                            intToInetAddress(wifiManager!!.dhcpInfo.serverAddress)!!.toString()
                                        var myAddress =
                                            intToInetAddress(wifiManager!!.dhcpInfo.ipAddress)!!.toString()
                                                .substring(1)
                                                .substring(1)
                                        var linkSpeed = wifiManager!!.connectionInfo.linkSpeed

                                        Log.d("XXXXXXX", "the server address ${serverAddress}")
                                        Log.d("XXXXXXX", "the link speed ${wifiManager!!.connectionInfo.linkSpeed}")
                                        // wifiManager
                                        while(ssid.contains("unknown ssid") || linkSpeed < 0 )//|| serverAddress.contains("/"))
                                        {
                                            ssid = wifiManager!!.connectionInfo.ssid
                                            linkSpeed = wifiManager!!.connectionInfo.linkSpeed
                                            serverAddress =
                                                intToInetAddress(wifiManager!!.dhcpInfo.serverAddress)!!.toString()
                                            sleep(1000)
                                        }

                                        //sleep(5000)

                                        serverAddress =
                                            intToInetAddress(wifiManager!!.dhcpInfo.serverAddress)!!.toString()
                                                .substring(1)
                                        myAddress =
                                            intToInetAddress(wifiManager!!.dhcpInfo.ipAddress)!!.toString()
                                                .substring(1)
                                        Log.d("XXXXXXX", "the server address ${serverAddress}")
                                        Log.d("XXXXXXX", "the link speed ${wifiManager!!.connectionInfo.linkSpeed}")
                                        val components = serverAddress.split(".")
                                        val broadcast_address =
                                            components[0] + "." + components[1] + "." + components[2] + ".255"
                                        val myInetAddress = InetAddress.getByName(myAddress)
                                        val broadcastInetAddress =
                                            InetAddress.getByName(broadcast_address)

                                        viewModelScope?.let { viewModelScope ->
                                            viewModelScope.launch(Dispatchers.IO) {
                                                if (client.connect(
                                                        networkInfo.serverIP,
                                                        this@NetworkClientModel
                                                    )
                                                ) {
                                                    _networkConnected.postValue(NetworkStatus.NetworkConnected)
                                                    sendRegistration()
                                                }
                                            }
                                        }
                                    }, 100)
                                }

                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d("xxx", e.stackTraceToString())
                }
            } else {
                var wifiManager =
                    Activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
                val wifiInfo: WifiInfo = wifiManager!!.getConnectionInfo()
                Log.d("xxx", "wifiinfo ${wifiInfo}")

                val builder = WifiNetworkSpecifier.Builder()
                builder.setSsid(networkInfo.ssid);
                builder.setWpa2Passphrase(networkInfo.password)
                val wifiNetworkSpecifier = builder.build()
                val networkRequestBuilder = NetworkRequest.Builder()

                networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                //networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier)

                val networkRequest = networkRequestBuilder.build()

                val connectivityManager =
                    Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.requestNetwork(networkRequest, networkCallback)

            }
        } else {
            _networkConnected.postValue(NetworkStatus.NetworkConnected)
            sendRegistration()
        }

    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val connectivityManager =
                Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.bindProcessToNetwork(network)
            val job = GlobalScope.launch(Dispatchers.Default) {

                networkInfo?.let { networkInfo ->
                    if (client.connect(networkInfo.serverIP, this@NetworkClientModel)) {
                        // start heartbeat
                        //startHeartbeat()
                        _networkConnected.postValue(NetworkStatus.NetworkConnected)
                        sendRegistration()
                    }
                }
            }

        }

        override fun onLost(network: Network) {
            super.onLost(network)
            _networkConnected.postValue(NetworkStatus.NetworkError)
            _clientRegistered.postValue(NetworkStatus.ClientRegisterError)
            _commandSent.postValue(NetworkStatus.CommandError)
            _dataReceived.postValue(NetworkStatus.DataReceivedError)
            sleep(kDialogTimeout)
            connectDelegate?.didConnect(false)

        }

        override fun onUnavailable() {
            super.onUnavailable()
            _networkConnected.postValue(NetworkStatus.NetworkError)
            _clientRegistered.postValue(NetworkStatus.ClientRegisterError)
            _commandSent.postValue(NetworkStatus.CommandError)
            _dataReceived.postValue(NetworkStatus.DataReceivedError)
            sleep(kDialogTimeout)
            connectDelegate?.didConnect(false)

        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            super.onCapabilitiesChanged(network, networkCapabilities)

//            val not_suspended = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
//            val validated = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_VALIDATED)
//            val trusted = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_TRUSTED)
//            val not_restricted = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)

//            if (udpBroadcaster == null && networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_VALIDATED))
//            {
//                Log.d( "xxx", "beginTransmittingHeartbeat" )
//                udpBroadcaster = UDPBroadcaster()
//                beginReceiving()
//                beginTransmittingHeartbeat()
//            }
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties)

        }
    }

    override fun sentData(data: String) {
        Log.d("SERVER CONNECT", data)
        _clientDataMessage.postValue(data)
    }

    override fun connectionString(connection: String) {
        _clientConnectMessage.postValue(connection)
    }

    private fun startHeartbeat() {
        viewModelScope?.let { viewModelScope ->
            viewModelScope.launch(Dispatchers.IO) {
                //join multicast
                val newWifiInterfaces = NetworkUtils.getWifiApInterfaces()
                if (newWifiInterfaces.size > 0) {
                    /* Acquire MultiCast Lock */
//                    val wifi = Activity!!.getSystemService(Context.WIFI_SERVICE) as WifiManager
//                    val multicastLock = wifi.createMulticastLock("multicastLock")
//                    multicastLock.setReferenceCounted(true)
//                    multicastLock.acquire()
                    Multicast.join(newWifiInterfaces[0])
                }
                heartbeatBroadcasting = true
                while (heartbeatBroadcasting) {
                    val heartbeat = "HEARTBEAT"
                    Multicast.broadcast(heartbeat.toByteArray())
                    sleep(NetworkUtils.kHeartbeatInterval)
                }
            }

        }
    }

    fun resetState() {
        _networkConnected.postValue(NetworkStatus.None)
        _clientRegistered.postValue(NetworkStatus.None)
        _commandSent.postValue(NetworkStatus.None)
        _dataReceived.postValue(NetworkStatus.None)
    }

    fun shutdown() {
        try {
            if (clientStarted) {
                resetState()
                client.shutdown()
                heartbeatBroadcasting = false

                val connectivityManager =
                    Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.bindProcessToNetwork(null)
                try {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                } catch (e: Exception) {

                }


                clientStarted = false
            }

        } catch (ex: Exception) {
            Log.d("shutdown Exception ", ex.stackTraceToString())
        }

    }
}