package edu.gtri.gpssample.viewmodels.models

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.RecyclerView
import edu.gtri.gpssample.R
import edu.gtri.gpssample.constants.HotspotMode
import edu.gtri.gpssample.constants.Keys
import edu.gtri.gpssample.constants.NetworkMode
import edu.gtri.gpssample.constants.NetworkStatus
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.models.*
import edu.gtri.gpssample.managers.GPSSampleWifiManager
import edu.gtri.gpssample.network.TCPServer
import edu.gtri.gpssample.network.models.NetworkCommand
import edu.gtri.gpssample.network.models.TCPMessage
import edu.gtri.gpssample.viewmodels.ConfigurationViewModel
import edu.gtri.gpssample.viewmodels.NetworkConnectionViewModel
import edu.gtri.gpssample.viewmodels.SamplingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.net.InetAddress
import java.net.Socket

const val hotspotMessageTemplate = "SSID:"
const val kNetworkTimeout = 5 //seconds
class NetworkHotspotModel : NetworkModel(), TCPServer.TCPServerDelegate, GPSSampleWifiManager.HotspotDelegate
{
    enum class MessageType()
    {
        ImportFailed,
        ImportRequestFailed,
        ExportRequestFailed,
    }

    companion object
    {
    }

    interface NetworkHotspotDelegate
    {
        fun didStartImport()
        fun didFinishImport()
        fun importFailed( messageType: MessageType )
    }

    var encryptionPassword = ""
    var delegate : NetworkHotspotDelegate? = null

    override val type = NetworkMode.NetworkHotspot

    interface NetworkCreationDelegate
    {
        fun didComplete(complete: Boolean)
    }

    var config : Config? = null

    var hotspotStarted = false

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

    private var _title : MutableLiveData<String> = MutableLiveData("")
    var title : LiveData<String> = _title

    fun setTitle( t: String)
    {
        _title = MutableLiveData(t)
        title = _title
    }

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

    var sharedViewModel : ConfigurationViewModel? = null

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

    @RequiresApi(Build.VERSION_CODES.O)
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
                status = NetworkStatus.NetworkError
            }
        }
        hotspotStarted = true
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

                    val v1 = NetworkConnectionViewModel(socket)

                    v1.updateName(payload)

                    _activity?.let{activity ->
                        v1.updateConnection(activity.getString(R.string.connected))
                    }?: run{
                        v1.updateConnection("Connected")
                    }

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
                if (hotspotMode.value == HotspotMode.Import)
                {
                    delegate?.importFailed( MessageType.ImportRequestFailed )
                }
                else
                {
                    config?.let { config->
                        val packedConfig = config.packMinimal()
                        val tcpMessage = TCPMessage(NetworkCommand.NetworkConfigResponse, packedConfig )
                        val byteArray = tcpMessage.toByteArray()
                        socket.outputStream.write(byteArray)
                        socket.outputStream.flush()
                        Log.d( "xxx", "Server: wrote ${byteArray!!.size}")
                    }
                }
            }

            NetworkCommand.NetworkEnumAreaExport,
            NetworkCommand.NetworkSampleAreaExport ->
            {
                if (hotspotMode.value == HotspotMode.Export)
                {
                    delegate?.importFailed( MessageType.ExportRequestFailed )
                    return
                }

                message.payload?.let { payload ->
                    val config = Config.unpack( payload, encryptionPassword )

                    if (config == null)
                    {
                        delegate?.importFailed( MessageType.ImportFailed )
                    }
                    else
                    {
                        delegate?.didStartImport()

                        DAO.instance().writableDatabase.beginTransaction()

                        DAO.configDAO.createOrUpdateConfig( config )

                        DAO.instance().writableDatabase.setTransactionSuccessful()
                        DAO.instance().writableDatabase.endTransaction()

                        DAO.configDAO.getConfig( config.uuid )?.let {
                            sharedViewModel?.setCurrentConfig(it)
                        }

                        delegate?.didFinishImport()
                    }
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

                // debug message
                val message = "${hotspotMessageTemplate} ${hotspot.hotspotSSID.value}"
                _message.postValue(message)

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
        }
        else
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
        while(!tcpServer.serverListening && timeout != kNetworkTimeout)
        {
            Thread.sleep(1000)
            timeout += 1
        }

        // if the server isn't listening, kill everything, throw errors
        if(!tcpServer.serverListening)
        {
            shutdown()
            return
        }

        _serverCreated.postValue(NetworkStatus.ServerCreated)

        // if the qrCode isn't generated
        if(generatedQRCode == null)
        {
            val jsonObject = JSONObject()
            jsonObject.put(Keys.kSSID.value, hotspot.hotspotSSID.value)
            jsonObject.put(Keys.kPass.value, hotspot.hotspotSSIDPassword.value)
            jsonObject.put(Keys.kIpAddress.value, hotspot.hotspotIP.value)


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
        }
        else
        {
            _creationDelegate?.didComplete(false)
        }
    }

    fun shutdown()
    {
        try{
            if(hotspotStarted)
            {
                tcpServer.shutdown()
                hotspot.stopHotSpot()

                _serverCreated.value = NetworkStatus.None
                _networkCreated.value = NetworkStatus.None
                _qrCreated.value = NetworkStatus.None
                hotspotStarted = false

                generatedQRCode = null

                clientConenctions.clear()
                _connections.postValue(clientConenctions)
            }
        }
        catch (exception : Exception)
        {
            Log.d("Shutdown Exception", exception.stackTraceToString())
        }
    }
}

@BindingAdapter("android:layout_height")
fun setLayoutHeight(view: View, height: Float)
{
    val layoutParams = view.layoutParams
    layoutParams.height = height.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("android:layout_width")
fun setLayoutWidth(view: View, width: Float)
{
    val layoutParams = view.layoutParams
    layoutParams.width = width.toInt()
    view.layoutParams = layoutParams
}

@BindingAdapter("bind:imageBitmap")
fun loadImage(iv: ImageView, bitmap: Bitmap?)
{
    iv.setImageBitmap(bitmap)
}


