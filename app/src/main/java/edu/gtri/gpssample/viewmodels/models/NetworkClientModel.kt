package edu.gtri.gpssample.viewmodels.models


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


private const val kDialogTimeout : Long = 400
class NetworkClientModel : NetworkModel(), TCPClient.TCPClientDelegate {
    override val type = NetworkMode.NetworkClient
    private val client : TCPClient = TCPClient()
    private var heartbeatBroadcasting : Boolean = false


    interface ConfigurationDelegate
    {
        fun configurationReceived(config : Config)
    }

    interface NetworkConnectDelegate
    {
        fun didConnect(complete: Boolean)
        fun didSendData(complete: Boolean)
    }

    var clientStarted = false
    // maybe a better way
    var currentEnumArea : EnumArea? = null

    var configurationDelegate : ConfigurationDelegate? = null

    private var _connectDelegate : NetworkConnectDelegate? = null
    var connectDelegate : NetworkConnectDelegate?
        get() = _connectDelegate
        set(value) {
            _connectDelegate = value
        }

    var _networkConnected : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val networkConnected : LiveData<NetworkStatus>
        get() = _networkConnected

    var _clientRegistered : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val clientRegistered : LiveData<NetworkStatus>
        get() = _clientRegistered

    var _commandSent : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val commandSent : LiveData<NetworkStatus>
        get() = _commandSent

    var _dataReceived : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val dataReceived : LiveData<NetworkStatus>
        get() = _dataReceived

    private var _clientMode : MutableLiveData<ClientMode> = MutableLiveData(ClientMode.None)
    val clientMode : LiveData<ClientMode>
        get() = _clientMode


    private var networkInfo : NetworkInfo? = null

    private var _clientConnectMessage : MutableLiveData<String> = MutableLiveData("")
    private var _clientDataMessage : MutableLiveData<String> = MutableLiveData("")
    var clientConnectMessage : LiveData<String> = _clientConnectMessage
    var clientDataMessage : LiveData<String> = _clientDataMessage


    var _configurationReceived : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    var configurationReceived : LiveData<NetworkStatus> = _configurationReceived


    override var Activity : Activity?
        get() = _activity
        set(value)
        {
            _activity = value
        }

    override fun initializeNetwork()
    {

    }

    fun setClientMode(mode : ClientMode)
    {
        _clientMode.value = mode
        _clientMode.postValue(mode)
    }

    fun sendRegistration()
    {
        _activity?.let { activity ->
            val app = (activity.application as MainApplication?)
            app?.user?.let { user ->

                val payload = user.name
                val message = TCPMessage(NetworkCommand.NetworkDeviceRegistrationRequest, payload)
                networkInfo?.let{networkInfo ->
                    val response = client.sendMessage(networkInfo.serverIP, message, this@NetworkClientModel)
                    // validate response
                    response?.let{response ->
                        if(response.command == NetworkCommand.NetworkDeviceRegistrationResponse)
                        {
                            sleep(kDialogTimeout)
                            _clientRegistered.postValue(NetworkStatus.ClientRegistered)

                            when (clientMode.value)
                            {
                                ClientMode.Configuration ->
                                {
                                    sendConfigurationCommand()
                                }
                                ClientMode.EnumerationTeam ->
                                {
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

    private fun sendConfigurationCommand()
    {
        val message = TCPMessage(NetworkCommand.NetworkConfigRequest, "")
        networkInfo?.let{networkInfo ->
            val response = client.sendMessage(networkInfo.serverIP, message, this)
            response?.let {

                _commandSent.postValue(NetworkStatus.CommandSent)
                if(response.command == NetworkCommand.NetworkConfigResponse)
                {
                    response.payload?.let {payload ->
                        val config = Config.unpack(payload)

                        // TODO: put the config in the list of current configs.....
                        config?.let{config ->
                            configurationDelegate?.configurationReceived(config)
                            _dataReceived.postValue(NetworkStatus.DataReceived)

                            sleep(kDialogTimeout)
                            connectDelegate?.didConnect(true)


                        }?:run {
                            _dataReceived.postValue(NetworkStatus.DataReceivedError)
                            connectDelegate?.didConnect(false)
                        }

                    }

                }
            }
        }
    }

    fun sendEnumerationData()
    {
        currentEnumArea?.let{enumArea ->
            networkInfo?.let{networkInfo ->
                val payload = enumArea.pack()
                val message = TCPMessage(NetworkCommand.NetworkEnumAreaExport, payload)
                val response = client.sendMessage(networkInfo.serverIP, message, this)
                connectDelegate?.didSendData(true)
            }

        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun startNetworking(networkInfo: NetworkInfo?) : Boolean
    {
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

    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToWifi(networkInfo: NetworkInfo)
    {
        clientStarted = true
        this.networkInfo = networkInfo
       // if  (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        if(client.socket == null)
        {
            if(false)
            {
                try {
                    val wifiConfig = WifiConfiguration()

                    wifiConfig.SSID = "\"" + networkInfo.ssid + "\""
                    wifiConfig.preSharedKey = "\"" + networkInfo.password + "\""
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.AuthAlgorithm.OPEN);
                    wifiConfig.allowedGroupCiphers.set(WifiConfiguration.AuthAlgorithm.SHARED);

                    var wifiManager = Activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
                    wifiManager!!.setWifiEnabled(true)

                    val suggestionWpa2 = WifiNetworkSuggestion.Builder()
                        .setSsid("Cypress Guest Wifi") //SSID name
                        .setWpa2Passphrase("cypresslovesyou") //password
                        .build()
                    val networkSuggestions: ArrayList<WifiNetworkSuggestion> = ArrayList()
                    networkSuggestions.add(suggestionWpa2)

                    wifiManager!!.startScan()


                    if(wifiManager!!.isWifiEnabled)
                    {
                        Log.d("xxxxxx", "WIFI ENABLED")
                    }
                    var netId = wifiManager!! .addNetwork(wifiConfig)
                    if (netId == -1){
                        //Try it again with no quotes in case of hex password
                        wifiConfig.wepKeys[0] = networkInfo.password;
                        netId = wifiManager.addNetwork(wifiConfig);
                    }

                    if(netId == -1)
                    {

                        netId = wifiManager!!.addNetworkSuggestions(networkSuggestions)
                    }
                    val disconnect = wifiManager.disconnect()
                    wifiManager.enableNetwork(netId, true)
                    val a = wifiManager.reconnect()
                    runBlocking(Dispatchers.Main) {
                        Handler().postDelayed({
                            wifiManager =
                                Activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?

                            val serverAddress =
                                intToInetAddress(wifiManager!!.dhcpInfo.serverAddress)!!.toString()
                                    .substring(1)
                            val myAddress =
                                intToInetAddress(wifiManager!!.dhcpInfo.ipAddress)!!.toString()
                                    .substring(1)
                            val components = serverAddress.split(".")
                            val broadcast_address =
                                components[0] + "." + components[1] + "." + components[2] + ".255"

                            val myInetAddress = InetAddress.getByName(myAddress)
                            val broadcastInetAddress = InetAddress.getByName(broadcast_address)
                            Log.d("here", "xxx  IP ADDRESS ${myInetAddress.hostAddress}")

                            // tryto connect to server
                            viewModelScope?.let { viewModelScope ->
                                viewModelScope.launch(Dispatchers.IO) {
                                    sleep(5000)
                                    client.write("192.168.227.232", "TEST", this@NetworkClientModel)
                                }
                            }
                        }, 1000)
                    }
                } catch (e: Exception) {
                    Log.d( "xxx", e.stackTraceToString())
                }
            }
            else
            {
                var wifiManager = Activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
                val wifiInfo: WifiInfo = wifiManager!!.getConnectionInfo()
                Log.d("xxx", "wifiinfo ${wifiInfo}")

                val builder = WifiNetworkSpecifier.Builder()
                builder.setSsid( networkInfo.ssid );
                builder.setWpa2Passphrase( networkInfo.password )
                val wifiNetworkSpecifier = builder.build()
                val networkRequestBuilder = NetworkRequest.Builder()

                networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                //networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier)

                val networkRequest = networkRequestBuilder.build()

                val connectivityManager = Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.requestNetwork( networkRequest, networkCallback )

            }
        }else
        {
            _networkConnected.postValue(NetworkStatus.NetworkConnected)
            sendRegistration()
        }

    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback()
    {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val connectivityManager = Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.bindProcessToNetwork(network)
            val job = GlobalScope.launch(Dispatchers.Default) {

                networkInfo?.let {networkInfo ->
                    if(client.connect(networkInfo.serverIP, this@NetworkClientModel))
                    {
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
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities)
        {
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
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties)
        {
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

    private fun startHeartbeat()
    {
        viewModelScope?.let { viewModelScope ->
            viewModelScope.launch(Dispatchers.IO) {
                //join multicast
                val newWifiInterfaces = NetworkUtils.getWifiApInterfaces()
                if(newWifiInterfaces.size > 0)
                {
                    /* Acquire MultiCast Lock */
//                    val wifi = Activity!!.getSystemService(Context.WIFI_SERVICE) as WifiManager
//                    val multicastLock = wifi.createMulticastLock("multicastLock")
//                    multicastLock.setReferenceCounted(true)
//                    multicastLock.acquire()
                    Multicast.join(newWifiInterfaces[0])
                }
                heartbeatBroadcasting = true
                while(heartbeatBroadcasting)
                {
                    val heartbeat = "HEARTBEAT"
                    Multicast.broadcast(heartbeat.toByteArray())
                    sleep(NetworkUtils.kHeartbeatInterval)
                }
            }

        }
    }
    fun resetState()
    {
        _networkConnected.postValue( NetworkStatus.None )
        _clientRegistered.postValue( NetworkStatus.None )
        _commandSent.postValue( NetworkStatus.None )
        _dataReceived.postValue( NetworkStatus.None )
    }

    fun shutdown()
    {
        try {
            if(clientStarted)
            {
                resetState()
                client.shutdown()
                heartbeatBroadcasting = false

                val connectivityManager = Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.bindProcessToNetwork(null)
                connectivityManager.unregisterNetworkCallback(networkCallback )

                clientStarted = false
            }

        }catch (ex : Exception)
        {
            Log.d("shutdown Exception " ,ex.stackTraceToString())
        }

    }
}