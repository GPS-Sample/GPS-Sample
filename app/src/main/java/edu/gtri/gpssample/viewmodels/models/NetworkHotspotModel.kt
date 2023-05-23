package edu.gtri.gpssample.viewmodels.models

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.HotspotMode
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.NetworkMode
import edu.gtri.gpssample.constants.NetworkStatus
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.managers.GPSSampleWifiManager
import edu.gtri.gpssample.network.TCPServer
import edu.gtri.gpssample.network.models.NetworkCommand
import edu.gtri.gpssample.network.models.TCPMessage
import edu.gtri.gpssample.viewmodels.NetworkConnectionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.InetAddress
import java.net.Socket


class NetworkHotspotModel : NetworkModel(), TCPServer.TCPServerDelegate,
    GPSSampleWifiManager.HotspotDelegate {
    override val type = NetworkMode.NetworkHotspot
    private val kNetworkTimeout = 5 //seconds
    interface NetworkCreationDelegate
    {
        fun didComplete(complete: Boolean)
    }

    var config : Config? = null

    private var _creationDelegate : NetworkCreationDelegate? = null
    var creationDelegate : NetworkCreationDelegate?
        get() = _creationDelegate
        set(value) {
            _creationDelegate = value
        }

    private val tcpServer : TCPServer = TCPServer()

    private var _networkCreated : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val networkCreated : LiveData<NetworkStatus>
        get() = _networkCreated

    private var _serverCreated : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val serverCreated : LiveData<NetworkStatus>
        get() = _serverCreated

    private var _qrCreated : MutableLiveData<NetworkStatus> = MutableLiveData(NetworkStatus.None)
    val qrCreated : LiveData<NetworkStatus>
        get() = _qrCreated

    private var _message : MutableLiveData<String> = MutableLiveData("")
    var message : LiveData<String> = _message

    private var _hotspotMode : MutableLiveData<HotspotMode> = MutableLiveData(HotspotMode.None)
    var hotspotMode : LiveData<HotspotMode> = _hotspotMode

    val connections: LiveData<List<NetworkConnectionViewModel>>
        get() = _connections
    private val _connections = MutableLiveData<List<NetworkConnectionViewModel>>(emptyList())

    private val clientConenctions : MutableList<NetworkConnectionViewModel> = mutableListOf()


    val destination = R.id.action_navigate_to_HotspotFragment

    val hotspot : GPSSampleWifiManager = GPSSampleWifiManager()
    val qrCodeHeight = 500.0f
    val qrCodeWidth = 500.0f

    private val _qrCodeBitmap : MutableLiveData<Bitmap> = MutableLiveData()
    val qrCodeBitmap :LiveData<Bitmap> = _qrCodeBitmap

    private var socketCreated = false
    private var hotspotCreated = false
    private var qrCodeCreated = false
    private var generatedQRCode : Bitmap? = null



    override var Activity : Activity?
        get() = _activity
        set(value)
        {
            _activity = value
            hotspot.activity = value
        }

    init
    {

        _connections.value = clientConenctions
        _connections.postValue(clientConenctions)
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

        //_networkCreated.postValue(status)
        //Thread.sleep(5000L)
        return (status == NetworkStatus.NetworkCreated)
    }

    fun closeHotspot()
    {
        hotspot.stopHotSpot()
    }

    fun setHotspotMode(mode : HotspotMode)
    {
        _hotspotMode.postValue(mode)
    }

    override fun didReceiveTCPMessage(message: TCPMessage, socket: Socket) {
        // if we are here the key was authenticated
        when (message.header.command)
        {

            NetworkCommand.NetworkDeviceRegistrationRequest ->
            {
                message.payload?.let { payload ->

                    // send response
                    val retMessage = TCPMessage(NetworkCommand.NetworkDeviceRegistrationResponse, "")

                    socket.outputStream.write( retMessage.toByteArray())
                    socket.outputStream.flush()

                    val v1 : NetworkConnectionViewModel = NetworkConnectionViewModel(socket)

                    v1.updateName(payload)
                    v1.updateConnection("Connected")
                    v1.socket = socket
                    var check : NetworkConnectionViewModel? = null
                    for(client in clientConenctions)
                    {
                        if(client.name.value == v1.name.value)
                        {
                            check = client
                            break
                        }
                    }
                    clientConenctions.remove(check)
                    clientConenctions.add(v1)
                    _connections.postValue(clientConenctions)
                    // create a new entry in the list
                    // send an ack
                }

            }
            NetworkCommand.NetworkConfigRequest ->
            {
                config?.let {
                    // create the config message
                    val response = TCPMessage(NetworkCommand.NetworkConfigResponse, it.pack())
                    socket.outputStream.write(response.toByteArray())
                }
            }
        }
    }

    override fun didDisconnect(socket: Socket) {
        var check : NetworkConnectionViewModel? = null
        for(client in clientConenctions)
        {
            if(client.socket == socket)
            {
                check = client
                break
            }
        }
        clientConenctions.remove(check)
        _connections.postValue(clientConenctions)
    }

    override fun clientConnected(socket: Socket) {


    }

    override fun didCreateHotspot(success : Boolean, serverIp : InetAddress?)
    {
        if(success && serverIp != null)
        {
            _networkCreated.value = NetworkStatus.NetworkCreated
            _networkCreated.postValue(NetworkStatus.NetworkCreated)

            viewModelScope?.let{ viewModelScope ->


                viewModelScope.launch(Dispatchers.IO) {
                    startNetworkServices()
                }

                // if the server isn't started already
                if(!tcpServer.serverListening)
                {
                    viewModelScope.launch(Dispatchers.IO) {
                        serverIp?.let{serverIp ->
                            socketCreated = tcpServer.createSocket()

                            tcpServer.beginListening(serverIp, this@NetworkHotspotModel)
                        }
                    }
                }

            }

        }else
        {
            // TODO:  Handle the error
            _networkCreated.value = NetworkStatus.NetworkError
            _networkCreated.postValue(NetworkStatus.NetworkError)
        }

    }

    private fun startNetworkServices()
    {

        var timeout = 0

        // maybe better way thread safety.  observable?
        while(!socketCreated && timeout != kNetworkTimeout)
        {
            Thread.sleep(1000)
            timeout += 1
        }

        // if the server isn't listening, kill everything, throw errors
        if(!tcpServer.serverListening)
        {
            tcpServer.shutdown()
            _serverCreated.postValue(NetworkStatus.ServerError)
            _qrCreated.postValue(NetworkStatus.QRCodeError)
            _creationDelegate?.didComplete(false)
            return
        }

        _serverCreated.postValue(NetworkStatus.ServerCreated)
        Log.d("XXXXX", hotspot.hotspotIP.value!!)
        Log.d("XXXXX", hotspot.hotspotSSID.value!!)

        // if the qrCode isn't generated
        if(generatedQRCode == null) {
            val jsonObject = JSONObject()
            jsonObject.put(Keys.kSSID.toString(), hotspot.hotspotSSID.value)
            jsonObject.put(Keys.kPass.toString(), hotspot.hotspotSSIDPassword.value)
            jsonObject.put(Keys.kIpAddress.toString(), hotspot.hotspotIP.value)
//        jsonObject.put( Keys.kTeam_id.toString(), team.id )
////        jsonObject.put( Keys.kStudy_uuid.toString(), study.uuid )
//        jsonObject.put( Keys.kEnumArea_id.toString(), enumArea.id )
////        jsonObject.put( Keys.kConfig_uuid.toString(), study.config_uuid )
//
            val qrgEncoder =
                QRGEncoder(jsonObject.toString(2), null, QRGContents.Type.TEXT, qrCodeWidth.toInt())
            qrgEncoder.colorBlack = Color.WHITE;
            qrgEncoder.colorWhite = Color.BLACK;

            generatedQRCode = qrgEncoder.bitmap
        }

        // post the qr code
        generatedQRCode?.let{qrCode ->
            _qrCodeBitmap.postValue(qrCode)
            _qrCreated.postValue(NetworkStatus.QRCodeCreated)
        }

        // slight pause to make the ui look better
        Thread.sleep(200)

        if(generatedQRCode != null && tcpServer.serverListening &&
            _networkCreated.value == NetworkStatus.NetworkCreated)
        {
            _creationDelegate?.didComplete(true)
        }else
        {
            _creationDelegate?.didComplete(false)
        }
    }
    fun shutdown()
    {
        hotspot.stopHotSpot()
        tcpServer.shutdown()
    }

    companion object
    {

    }
}
@BindingAdapter("android:layout_height")
fun setLayoutHeight(view: View, height: Float) {
    val layoutParams = view.layoutParams
    layoutParams.height = height.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("android:layout_width")
fun setLayoutWidth(view: View, width: Float) {
    val layoutParams = view.layoutParams
    layoutParams.width = width.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("bind:imageBitmap")
fun loadImage(iv: ImageView, bitmap: Bitmap?) {
    iv.setImageBitmap(bitmap)
}


