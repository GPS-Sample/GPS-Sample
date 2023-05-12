package edu.gtri.gpssample.viewmodels.models

import android.app.Activity
import android.net.Network
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.NetworkMode
import edu.gtri.gpssample.constants.NetworkStatus
import edu.gtri.gpssample.managers.GPSSampleWifiManager
import edu.gtri.gpssample.network.TCPServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.net.InetAddress

class NetworkHotspotModel : NetworkModel(), TCPServer.TCPServerDelegate,
    GPSSampleWifiManager.GPSSampleWifiManagerDelegate {



    override val type = NetworkMode.NetworkHotspot
    private val tcpServer : TCPServer = TCPServer()

    private var _networkCreated : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    var networkCreated : LiveData<NetworkStatus> = _networkCreated

    private var _message : MutableLiveData<String> = MutableLiveData("")
    var message : LiveData<String> = _message

    val destination = R.id.action_FirstFragment_to_HotspotFragment

    val hotspot : GPSSampleWifiManager = GPSSampleWifiManager()
    override var Activity : Activity?
        get() = _activity
        set(value)
        {
            _activity = value
            hotspot.activity = value
        }

    init
    {

    }

    override fun initializeNetwork()
    {
    }

    override fun startNetworking(networkInfo: NetworkInfo?) : Boolean
    {
        var status : NetworkStatus = NetworkStatus.None
        _networkCreated.postValue(NetworkStatus.None)
        Thread.sleep(1000L)
        runBlocking(Dispatchers.Main) {
            try {

                hotspot?.let { hotspot ->
                    hotspot.startHotSpot(this@NetworkHotspotModel)

                }
                status = NetworkStatus.NetworkCreated

            }catch (ex : Exception)
            {
                Log.d("ERROR", " ${ex.toString()}")
                status = NetworkStatus.NetworkError
            // _networkCreated.postValue(NetworkStatus.NetworkError)

            }
        }

        _networkCreated.postValue(status)
        Thread.sleep(1000L)
        return (status == NetworkStatus.NetworkCreated)
    }

    fun closeHotspot()
    {
        hotspot.stopHotSpot()
    }

    override fun didReceiveTCPMessage(message: String) {
        _message.postValue(message)
    }
    override fun didCreateHotspot(success : Boolean, serverIp : InetAddress?)
    {
        Log.d("xxx", "WE CREATE HOTSPOT")
        viewModelScope?.let{ viewModelScope ->
            viewModelScope.launch(Dispatchers.IO) {
                serverIp?.let{serverIp ->
                    tcpServer.beginListening(serverIp, this@NetworkHotspotModel)
                }

            }
        }


    }

    override fun didStartHotspot(ssid: String, pass: String) {
        TODO("Not yet implemented")
    }
}