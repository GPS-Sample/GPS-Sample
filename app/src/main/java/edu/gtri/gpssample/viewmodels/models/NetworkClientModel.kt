package edu.gtri.gpssample.viewmodels.models


import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
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
import edu.gtri.gpssample.database.models.Study
import edu.gtri.gpssample.network.*
import edu.gtri.gpssample.network.models.NetworkCommand
import edu.gtri.gpssample.network.models.TCPMessage
import kotlinx.coroutines.*
import java.lang.Thread.sleep
import java.net.InetAddress


private const val kDialogTimeout: Long = 400

class NetworkClientModel : NetworkModel(), TCPClient.TCPClientDelegate
{
    interface ConfigurationDelegate
    {
        fun configurationReceived(config: Config)
    }

    interface NetworkConnectDelegate
    {
        fun didReceiveConfiguration(error: Boolean)
        fun didSendData(complete: Boolean)
    }

    override val type = NetworkMode.NetworkClient
    private val client: TCPClient = TCPClient()
    private var heartbeatBroadcasting: Boolean = false
    private var connectWaiting : Boolean = true
    private val kNetworkTimeout = 20

    var clientStarted = false
    var encryptionPassword = ""
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

    override fun initializeNetwork()
    {
    }

    fun setClientMode(mode: ClientMode)
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
                networkInfo?.let { networkInfo ->
                    val response = client.sendMessage(networkInfo.serverIP, message, this@NetworkClientModel)
                    // validate response
                    response?.let { response ->
                        if (response.command == NetworkCommand.NetworkDeviceRegistrationResponse)
                        {
                            sleep(kDialogTimeout)
                            _clientRegistered.postValue(NetworkStatus.ClientRegistered)

                            when (clientMode.value)
                            {
                                ClientMode.Configuration -> {
                                    sendConfigurationCommand()
                                }
                                ClientMode.EnumerationTeam -> {
                                    sendEnumerationData()
                                }
                                ClientMode.CollectionTeam -> {
                                    sendCollectionData()
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendConfigurationCommand()
    {
        val message = TCPMessage(NetworkCommand.NetworkConfigRequest, "")
        networkInfo?.let { networkInfo ->
            val response = client.sendMessage(networkInfo.serverIP, message, this)
            response?.let {

                _commandSent.postValue(NetworkStatus.CommandSent)
                if (response.command == NetworkCommand.NetworkConfigResponse) {
                    response.payload?.let { payload ->
                        Config.unpack(payload, encryptionPassword)?.let { config ->
                            configurationDelegate?.configurationReceived(config)
                            _dataReceived.postValue(NetworkStatus.DataReceived)

                            sleep(kDialogTimeout)
                            connectDelegate?.didReceiveConfiguration(false)
                        } ?: run {
                            _dataReceived.postValue(NetworkStatus.DataReceivedError)
                            connectDelegate?.didReceiveConfiguration(true)
                        }
                    }
                }
            }
        }
    }

    fun sendEnumerationData()
    {
        currentEnumArea?.let { enumArea ->
            networkInfo?.let { networkInfo ->
                val payload = enumArea.pack(encryptionPassword)

                val message = TCPMessage(NetworkCommand.NetworkEnumAreaExport, payload)
                val response = client.sendMessage(networkInfo.serverIP, message, this)

                _commandSent.postValue(NetworkStatus.CommandSent)
                // quick sleep to make the UI look better
                sleep(kDialogTimeout)
                connectDelegate?.didSendData(true)
            }
        }
    }

    fun sendCollectionData()
    {
        currentEnumArea?.let { enumArea ->
            networkInfo?.let { networkInfo ->
                val payload = enumArea.pack(encryptionPassword)

                val message = TCPMessage(NetworkCommand.NetworkSampleAreaExport, payload)
                val response = client.sendMessage(networkInfo.serverIP, message, this)

                _commandSent.postValue(NetworkStatus.CommandSent)
                // quick sleep to make the UI look better
                sleep(kDialogTimeout)
                connectDelegate?.didSendData(true)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun startNetworking(networkInfo: NetworkInfo?): Boolean
    {
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

        try {
            return InetAddress.getByAddress(addressBytes)
        } catch (e: Exception) {
        }

        return null
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToWifi(networkInfo: NetworkInfo)
    {
        connectWaiting = true
        clientStarted = true
        this.networkInfo = networkInfo
        // if  (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
        if (client.socket == null) {
            //if(false)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
            {
                try
                {
                    val connectivityManager =
                        Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

                    val wifiConfig = WifiConfiguration()

                    wifiConfig.SSID = "\"" + networkInfo.ssid + "\""
                    wifiConfig.preSharedKey = "\"" + networkInfo.password + "\""

                    val wifiManager = Activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
                    wifiManager!!.setWifiEnabled(true)

                    if (wifiManager!!.isWifiEnabled)
                    {
                        var netId = wifiManager!!.addNetwork(wifiConfig)
                        if (netId == -1)
                        {
                            //Try it again with no quotes in case of hex password
                            wifiConfig.wepKeys[0] = networkInfo.password;
                            netId = wifiManager.addNetwork(wifiConfig);
                        }

                        runBlocking(Dispatchers.Main) {

                            val disconnect = wifiManager.disconnect()
                            if (disconnect)
                            {
                                wifiManager.enableNetwork(netId, true)

                                val reconnect = wifiManager.reconnect()
                                if (reconnect)
                                {
                                    Handler().postDelayed({

                                        val connectivityManager = Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                                        connectivityManager.registerDefaultNetworkCallback(networkCallback)

                                        CoroutineScope(Dispatchers.IO +  SupervisorJob()).launch {
                                            networkErrorCheck()
                                        }
                                    }, 100)
                                }

                            }
                        }
                    }
                }
                catch (e: Exception)
                {
                    Log.d("error", e.stackTraceToString())
                }
            }
            else
            {
                val wifiManager = Activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
                val wifiInfo: WifiInfo = wifiManager!!.getConnectionInfo()

                val builder = WifiNetworkSpecifier.Builder()
                builder.setSsid(networkInfo.ssid);
                builder.setWpa2Passphrase(networkInfo.password)
                val wifiNetworkSpecifier = builder.build()
                val networkRequestBuilder = NetworkRequest.Builder()

                networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                //networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
                //networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
                networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier)

                val networkRequest = networkRequestBuilder.build()

                val connectivityManager = Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.requestNetwork(networkRequest, networkCallback)
                CoroutineScope(Dispatchers.IO +  SupervisorJob()).launch {
                    networkErrorCheck()
                }
            }
        }
        else
        {
            _networkConnected.postValue(NetworkStatus.NetworkConnected)
            sendRegistration()
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback()
    {
        override fun onAvailable(network: Network)
        {
            super.onAvailable(network)

            val connectivityManager = Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.bindProcessToNetwork(network)
            sleep(1000)

            val job = GlobalScope.launch(Dispatchers.Default) {

                networkInfo?.let { networkInfo ->
                    if (client.connect(networkInfo.serverIP, this@NetworkClientModel))
                    {
                        _networkConnected.postValue(NetworkStatus.NetworkConnected)
                        sendRegistration()
                        connectWaiting = false
                    }
                    else
                    {
                        runError()
                    }
                }?: run{
                    runError()
                }
            }
        }

        override fun onLost(network: Network)
        {
            super.onLost(network)
            runError()
        }

        override fun onUnavailable()
        {
            super.onUnavailable()
            runError()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities)
        {
            super.onCapabilitiesChanged(network, networkCapabilities)
        }

        @RequiresApi(Build.VERSION_CODES.R)
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties)
        {
            super.onLinkPropertiesChanged(network, linkProperties)
        }
    }

    override fun sentData(data: String)
    {
        _clientDataMessage.postValue(data)
    }

    override fun connectionString(connection: String)
    {
        _clientConnectMessage.postValue(connection)
    }

    fun resetState()
    {
        _networkConnected.postValue(NetworkStatus.None)
        _clientRegistered.postValue(NetworkStatus.None)
        _commandSent.postValue(NetworkStatus.None)
        _dataReceived.postValue(NetworkStatus.None)
    }

    fun shutdown()
    {
        try
        {
            if (clientStarted)
            {
                resetState()
                client.shutdown()
                heartbeatBroadcasting = false

                val connectivityManager = Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                connectivityManager.bindProcessToNetwork(null)

                try
                {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
                catch (e: Exception)
                {
                }

                clientStarted = false
            }
        } catch (ex: Exception) {
            Log.d("shutdown Exception ", ex.stackTraceToString())
        }
    }

    private fun networkErrorCheck()
    {
//        var count = 0
//        var timeout = false
//        while(connectWaiting)
//        {
//            sleep(1000)
//            count += 1
//
//            if(count > kNetworkTimeout)
//            {
//                timeout = true
//                break
//            }
//        }
//
//        if(timeout)
//        {
//            GlobalScope.launch(Dispatchers.Default) {
//                runError()
//            }
//        }
    }

    fun runError()
    {
        _networkConnected.postValue(NetworkStatus.NetworkError)
        _clientRegistered.postValue(NetworkStatus.ClientRegisterError)
        _commandSent.postValue(NetworkStatus.CommandError)
        _dataReceived.postValue(NetworkStatus.DataReceivedError)
        sleep(kDialogTimeout)
        connectDelegate?.didReceiveConfiguration(true)
    }
}