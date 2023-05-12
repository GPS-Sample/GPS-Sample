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
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.NetworkMode
import edu.gtri.gpssample.constants.NetworkStatus
import edu.gtri.gpssample.network.TCPClient
import edu.gtri.gpssample.network.TCPServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.Thread.sleep
import java.net.InetAddress


class NetworkClientModel : NetworkModel(), TCPClient.TCPCLientDelegate, TCPServer.TCPServerDelegate {
    override val type = NetworkMode.NetworkClient
    private val client : TCPClient = TCPClient()

    private val tcpServer : TCPServer = TCPServer()


    var _networkConnected : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    var networkConnected : LiveData<NetworkStatus> = _networkConnected

    // test
    private var _clientConnectMessage : MutableLiveData<String> = MutableLiveData("")
    private var _clientDataMessage : MutableLiveData<String> = MutableLiveData("")
    var clientConnectMessage : LiveData<String> = _clientConnectMessage
    var clientDataMessage : LiveData<String> = _clientDataMessage


    var _configurationReceived : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    var configurationReceived : LiveData<NetworkStatus> = _configurationReceived

    val destination = R.id.action_FirstFragment_to_ClientFragment

    override var Activity : Activity?
        get() = _activity
        set(value)
        {
            _activity = value
        }

    override fun initializeNetwork()
    {

    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun startNetworking(networkInfo: NetworkInfo?) : Boolean
    {
        // connect to wifi

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
       // if  (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
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
            Log.d("here", "trying to connect ${networkInfo.ssid} ${networkInfo.password}")


//            val suggestion1 = WifiNetworkSuggestion.Builder()
//                .setSsid("test111111")
//                .setIsAppInteractionRequired(true) // Optional (Needs location permission)
//                .build();
//
//            val suggestionsList = listOf(suggestion1)
//
//            var wifiManager = Activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
//            val status = wifiManager.addNetworkSuggestions(suggestionsList);
//            if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
//                // do error handling here
//            }
//
//// Optional (Wait for post connection broadcast to one of your suggestions)
//            val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);
//
//            val broadcastReceiver = object : BroadcastReceiver() {
//                override fun onReceive(context: Context, intent: Intent) {
//                    if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
//                        return;
//                    }
//                    // do post connect processing here
//                }
//            };
//            context.registerReceiver(broadcastReceiver, intentFilter);
            var wifiManager = Activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager?
            val wifiInfo: WifiInfo = wifiManager!!.getConnectionInfo()
            Log.d("xxx", "wifiinfo ${wifiInfo}")

            val builder = WifiNetworkSpecifier.Builder()
            //builder.setSsid( networkInfo.ssid );
            builder.setSsid( "Cypress Guest Wifi" )
            builder.setWpa2Passphrase("cypresslovesyou")
            //builder.setWpa2Passphrase( networkInfo.password )
            //builder.setWpa2Passphrase("")
            val wifiNetworkSpecifier = builder.build()


            val networkRequestBuilder = NetworkRequest.Builder()

            networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            //networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)
//            networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_TRUSTED)
            networkRequestBuilder.setNetworkSpecifier(wifiNetworkSpecifier)

            val networkRequest = networkRequestBuilder.build()

            val connectivityManager = Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.requestNetwork( networkRequest, networkCallback )

//            val job = GlobalScope.launch(Dispatchers.Default) {
//                client.write("192.168.1.100", "TEST", this@NetworkClientModel)
//            }
        }
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback()
    {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            val connectivityManager = Activity!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.bindProcessToNetwork(network)
            val job = GlobalScope.launch(Dispatchers.Default) {
                client.write("192.168.1.100", "TEST", this@NetworkClientModel)
            }

        }
        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d("xxxx", "FAILED!!!!! HERE" )
        }
        override fun onUnavailable() {
            super.onUnavailable()
            Log.d("xxxx", "FAILED!!!!! NO NETWORK" )
            _networkConnected.postValue(NetworkStatus.NetworkError)
        }
        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities)
        {
            Log.d("xxxxx", "connected 1")
            super.onCapabilitiesChanged(network, networkCapabilities)

            val not_suspended = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_NOT_SUSPENDED)
            val validated = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val trusted = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_TRUSTED)
            val not_restricted = networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)

//            if (udpBroadcaster == null && networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_VALIDATED))
//            {
//                Log.d( "xxx", "beginTransmittingHeartbeat" )
//                udpBroadcaster = UDPBroadcaster()
//                beginReceiving()
//                beginTransmittingHeartbeat()
//            }
        }
        var trycon = true
        @RequiresApi(Build.VERSION_CODES.R)
        override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties)
        {
            Log.d("xxxxx", "connected 2")
            super.onLinkPropertiesChanged(network, linkProperties)
            try {


                viewModelScope?.let { viewModelScope ->
                    viewModelScope.launch(Dispatchers.IO) {
                        sleep(1000)
                        if(trycon)
                        {
                            val wifiManager =
                                Activity!!.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

                            wifiManager?.let{wifiManager ->
                                val myAddress =
                                    intToInetAddress(wifiManager.dhcpInfo.ipAddress)!!.toString()
                                        .substring(1)

                                val myInetAddress = InetAddress.getByName(myAddress)
                               // tcpServer.beginListening(myInetAddress, this@NetworkClientModel)
                            }

                            //client.write("192.168.48.183", "TEST", this@NetworkClientModel)
                            //client.write("142.250.189.110", "TEST", this@NetworkClientModel)
                            //client.write("192.168.43.1", "TEST", this@NetworkClientModel)
//                            val job = GlobalScope.launch(Dispatchers.Default) {
//                                client.write("192.168.1.100", "TEST", this@NetworkClientModel)
//                            }
                            //client.write("192.168.1.100", "TEST", this@NetworkClientModel)
                            trycon = false
                        }

                    }

//                    val serverAddress = linkProperties.dhcpServerAddress.toString().substring(1)
//                    // linkProperties.linkAddresses[0] is the IPV6 address
//                    val myAddress = linkProperties.linkAddresses[1].toString()
//                        .substring(0, linkProperties.linkAddresses[1].toString().length - 3)
//
//                    val components = serverAddress.split(".")
//                    val server_udp_address =
//                        components[0] + "." + components[1] + "." + components[2] + ".255"
//
//                    val myInetAddress = InetAddress.getByName(myAddress)
//
//                    Log.d(
//                        "NETWORK CONNECT ",
//                        "connected to network!@!!!!!!!!!! ${myInetAddress.hostAddress}"
//                    )
                }
            }catch(e : Exception)
            {

            }

//            broadcastInetAddress = InetAddress.getByName( server_udp_address )
        }
    }

    override fun sentData(data: String) {
        Log.d("SERVER CONNECT", data)
        _clientDataMessage.postValue(data)
    }

    override fun connectionString(connection: String) {
        _clientConnectMessage.postValue(connection)
    }

    override fun didReceiveTCPMessage(message: String) {
        TODO("Not yet implemented")
    }
}