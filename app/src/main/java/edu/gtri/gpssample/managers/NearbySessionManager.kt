package edu.gtri.gpssample.managers

import android.content.Context
import android.graphics.Color
import android.util.Log
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Image
import edu.gtri.gpssample.dialogs.ConfirmationDialog.ButtonPress
import edu.gtri.gpssample.dialogs.NearbySessionStatusDialog
import edu.gtri.gpssample.extensions.getSimpleUuid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.*
import java.lang.Thread.sleep
import java.util.UUID

/**
 * ============================================================================
 * Models
 * ============================================================================
 */

@Serializable
data class Request(
    val command: Command,
    val imageUuid: String? = null
)

@Serializable
enum class Command
{
    GET_CONFIG,
    GET_IMAGE,
    DONE
}

/**
 * ============================================================================
 * State
 * ============================================================================
 */

sealed interface NearbySessionState
{
    object Idle : NearbySessionState

    data class Advertising(
        val sessionId: String
    ) : NearbySessionState

    object Connecting : NearbySessionState

    object Connected : NearbySessionState

    object Closed : NearbySessionState

    data class Error(
        val message: String,
        val throwable: Throwable? = null
    ) : NearbySessionState
}

/**
 * ============================================================================
 * NearbySessionManager
 * ============================================================================
 */

class NearbySessionManager( private val context: Context, private val lifecycleOwner: LifecycleOwner, private val config: Config? )
{
    companion object
    {
        private const val TAG = "NearbySession"
        private const val SERVICE_ID = "com.example.gpssample.transfer"
    }

    private val client: ConnectionsClient = Nearby.getConnectionsClient( context )

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _nearbySessionState = MutableStateFlow<NearbySessionState>(NearbySessionState.Idle)

    val nearbySessionState: StateFlow<NearbySessionState> = _nearbySessionState

    // =========================================================================
    // Session
    // =========================================================================

    private var connectedEndpointId: String? = null
    private var sessionId: String? = null

    // =========================================================================
    // Request tracking (ONLY ONE at a time)
    // =========================================================================

    private enum class PendingRequest
    {
        CONFIG,
        IMAGE
    }

    private var pendingRequest: PendingRequest? = null
    private var configDeferred: CompletableDeferred<Config>? = null
    private var imageDeferred: CompletableDeferred<Image>? = null
    private var nearbySessionStatusDialog: NearbySessionStatusDialog? = null

    // =========================================================================
    // HOST API
    // =========================================================================

    fun startHosting(): String
    {
        val id = UUID.randomUUID().toString()
        sessionId = id

        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        Log.d( "xxx", "Advertising Started" )

        client.startAdvertising(
            id,
            SERVICE_ID,
            hostConnectionCallback,
            options
        ).addOnSuccessListener {
            _nearbySessionState.value = NearbySessionState.Advertising(id)
        }.addOnFailureListener {
            _nearbySessionState.value = NearbySessionState.Error("Advertising failed", it)
        }

        return id
    }

    fun stopHosting()
    {
        Log.d( "xxx", "Stop Advertising" )
        client.stopAdvertising()
        connectedEndpointId = null
        _nearbySessionState.value = NearbySessionState.Idle
    }

    fun handleNearbySessionStatusForHost( nearbySessionStatusDialog: NearbySessionStatusDialog )
    {
        this.nearbySessionStatusDialog = nearbySessionStatusDialog

        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED )
            {
                nearbySessionState.collect { state ->

                    when (state) {
                        is NearbySessionState.Advertising -> {
                            val qrgEncoder = QRGEncoder(state.sessionId, null, QRGContents.Type.TEXT, 500 )
                            qrgEncoder.colorBlack = Color.WHITE;
                            qrgEncoder.colorWhite = Color.BLACK;
                            nearbySessionStatusDialog.showQrCode( qrgEncoder.bitmap )
                            nearbySessionStatusDialog.showDoneButton()
                        }

                        NearbySessionState.Connecting -> {
                        }

                        NearbySessionState.Connected -> {
                            nearbySessionStatusDialog.setStatus( "Connected." )
                            nearbySessionStatusDialog.showCancelButton()
                        }

                        NearbySessionState.Idle -> {
                        }

                        is NearbySessionState.Error -> {
                            nearbySessionStatusDialog.setStatus( state.message )
                        }

                        NearbySessionState.Closed -> {
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // CLIENT API
    // =========================================================================

    fun clientConnect(sessionId: String)
    {
        _nearbySessionState.value = NearbySessionState.Connecting

        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        Log.d("xxx", "Discovery Started" )

        client.startDiscovery(
            SERVICE_ID,
            object : EndpointDiscoveryCallback()
            {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo)
                {
                    Log.d( "xxx", "EndPoint Found" )

                    if (info.endpointName != sessionId) return

                    client.stopDiscovery()

                    Log.d( "xxx", "Connection Requested" )
                    client.requestConnection("Client", endpointId, clientConnectionCallback )
                }

                override fun onEndpointLost(endpointId: String) {}
            },
            options
        )
    }

    fun handleNearbySessionStatusForClient( nearbySessionStatusDialog: NearbySessionStatusDialog, completion: (( config: Config )->Unit))
    {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED)
            {
                nearbySessionState.collect { state ->

                    when (state) {
                        is NearbySessionState.Advertising -> {
                        }

                        NearbySessionState.Connecting -> {
                            nearbySessionStatusDialog.setStatus( "Connecting..." )
                        }

                        NearbySessionState.Connected -> {
                            nearbySessionStatusDialog.showCancelButton()
                            nearbySessionStatusDialog.setStatus( "Connected." )
                            sleep(1000 )
                            nearbySessionStatusDialog.setStatus( "Requesting Configuration..." )

                            val config = requestConfig()

                            nearbySessionStatusDialog.setStatus( "Received Configuration." )

                            for (enumArea in config.enumAreas)
                            {
                                for (location in enumArea.locations)
                                {
                                    if (location.imageUuid.isNotEmpty())
                                    {
                                        if (ImageDAO.instance().doesNotExist( location.imageUuid))
                                        {
                                            nearbySessionStatusDialog.setStatus( "Requesting Image ${location.imageUuid.getSimpleUuid()}..." )
                                            val image = requestImage( location.imageUuid )
                                            ImageDAO.instance().createImage( image )
                                        }
                                    }
                                }
                            }

                            completion( config )
                        }

                        NearbySessionState.Idle -> {
                        }

                        is NearbySessionState.Error -> {
                            nearbySessionStatusDialog.setStatus( state.message )
                        }

                        NearbySessionState.Closed -> {
                            nearbySessionStatusDialog.setStatus( "Closed." )
                        }
                    }
                }
            }
        }
    }

    suspend fun requestConfig(): Config
    {
        checkConnected()

        check(pendingRequest == null)
        {
            "Request already in progress"
        }

        pendingRequest = PendingRequest.CONFIG

        val deferred = CompletableDeferred<Config>()
        configDeferred = deferred

        sendRequest( Request(Command.GET_CONFIG ))

        return try {
            deferred.await()
        } finally {
            pendingRequest = null
            configDeferred = null
        }
    }

    suspend fun requestImage(imageId: String): Image
    {
        checkConnected()

        check(pendingRequest == null)
        {
            "Request already in progress"
        }

        pendingRequest = PendingRequest.IMAGE

        val deferred = CompletableDeferred<Image>()
        imageDeferred = deferred

        sendRequest( Request( Command.GET_IMAGE, imageId ))

        return try {
            deferred.await()
        } finally {
            pendingRequest = null
            imageDeferred = null
        }
    }

    fun clientClose()
    {
        sendRequest(Request(Command.DONE))

        client.stopDiscovery()

        connectedEndpointId?.let {
            client.disconnectFromEndpoint(it)
        }

        connectedEndpointId = null
        pendingRequest = null

        _nearbySessionState.value = NearbySessionState.Idle
    }

    // =========================================================================
    // HOST CALLBACK
    // =========================================================================

    private val hostConnectionCallback = object : ConnectionLifecycleCallback()
    {
        override fun onConnectionInitiated( endpointId: String, connectionInfo: ConnectionInfo)
        {
            if (connectedEndpointId != null)
            {
                client.rejectConnection(endpointId)
                return
            }

            Log.d( "xxx", "Connection Accepted" )

            client.acceptConnection(endpointId, hostPayloadCallback )
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution)
        {
            Log.d( "xxx", "Connection Result Received" )
            if (result.status.isSuccess)
            {
                connectedEndpointId = endpointId
                _nearbySessionState.value = NearbySessionState.Connected
            }
        }

        override fun onDisconnected(endpointId: String)
        {
            Log.d( "xxx", "hostConnectionCallback: Connection Disconnected" )

            stopHosting()
            startHosting()
        }
    }

    // =========================================================================
    // CLIENT CALLBACK
    // =========================================================================

    private val clientConnectionCallback = object : ConnectionLifecycleCallback()
    {
        override fun onConnectionInitiated( endpointId: String, connectionInfo: ConnectionInfo )
        {
            Log.d( "xxx", "clientConnectionCallback: Connection Accepted" )
            client.acceptConnection(endpointId, clientPayloadCallback )
        }

        override fun onConnectionResult( endpointId: String, result: ConnectionResolution )
        {
            Log.d( "xxx", "clientConnectionCallback: Connection Result Received" )
            if (result.status.isSuccess)
            {
                connectedEndpointId = endpointId
                _nearbySessionState.value = NearbySessionState.Connected
            }
        }

        override fun onDisconnected(endpointId: String)
        {
            Log.d( "xxx", "clientConnectionCallback: Connection Disconnected" )
            connectedEndpointId = null
            _nearbySessionState.value = NearbySessionState.Idle
        }
    }

    // =========================================================================
    // HOST PAYLOAD HANDLER
    // =========================================================================

    private val hostPayloadCallback = object : PayloadCallback()
    {
        override fun onPayloadReceived( endpointId: String, payload: Payload )
        {
            if (payload.type != Payload.Type.BYTES) return

            val request = json.decodeFromString(Request.serializer(), String(payload.asBytes()!!))

            when (request.command)
            {
                Command.GET_CONFIG -> {
                    nearbySessionStatusDialog?.setStatus( "Sending Configuration..." )
                    sendConfig()
                }
                Command.GET_IMAGE -> {
                    request.imageUuid?.let { imageUuid ->
                        nearbySessionStatusDialog?.setStatus( "Sending Image ${imageUuid.getSimpleUuid()}..." )
                        sendImage(imageUuid )
                    }
                }
                Command.DONE -> {
                    client.disconnectFromEndpoint(endpointId)
                }
            }
        }

        override fun onPayloadTransferUpdate( endpointId: String, update: PayloadTransferUpdate)
        {
        }
    }

    // =========================================================================
    // CLIENT PAYLOAD HANDLER
    // =========================================================================

    private val clientPayloadCallback = object : PayloadCallback()
    {
        override fun onPayloadReceived( endpointId: String, payload: Payload )
        {
            if (payload.type != Payload.Type.STREAM) return

            val input = payload.asStream()?.asInputStream() ?: return

            when (pendingRequest)
            {
                PendingRequest.CONFIG -> receiveConfig(input)
                PendingRequest.IMAGE -> receiveImage(input)
                null -> Log.w(TAG, "Unexpected payload")
            }
        }

        override fun onPayloadTransferUpdate( endpointId: String, update: PayloadTransferUpdate)
        {
        }
    }

    // =========================================================================
    // HOST SENDERS
    // =========================================================================

    @OptIn(ExperimentalSerializationApi::class)
    private fun sendConfig()
    {
        val endpoint = connectedEndpointId ?: return

        val output = PipedOutputStream()
        val input = PipedInputStream(output, 64 * 1024)

        client.sendPayload(endpoint, Payload.fromStream(input))

        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                json.encodeToStream(Config.serializer(), config!!, output)
            } catch( ex: Exception ) {
                Log.d( "xxx", ex.stackTraceToString())
            } finally {
                output.close()
            }
        }
    }

    private fun sendImage( imageId: String )
    {
        ImageDAO.instance().getImage(imageId )?.let { image ->
            val endpoint = connectedEndpointId ?: return

            val output = PipedOutputStream()
            val input = PipedInputStream(output, 64 * 1024)

            client.sendPayload(endpoint, Payload.fromStream(input))

            lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                try {
                    json.encodeToStream(Image.serializer(), image, output )
                } finally {
                    output.close()
                }
            }
        }
    }

    // =========================================================================
    // CLIENT RECEIVERS
    // =========================================================================

    @OptIn(ExperimentalSerializationApi::class)
    private fun receiveConfig(input: InputStream)
    {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val config = json.decodeFromStream(Config.serializer(),input )
                configDeferred?.complete(config )
            }
            catch( ex: Exception )
            {
                Log.d( "xxx", ex.stackTraceToString())
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun receiveImage(input: InputStream)
    {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val image = json.decodeFromStream(
                Image.serializer(),
                input
            )

            imageDeferred?.complete(image )
        }
    }

    // =========================================================================
    // COMMON
    // =========================================================================

    private fun sendRequest(request: Request)
    {
        val endpoint = connectedEndpointId ?: return

        val bytes = json.encodeToString(Request.serializer(), request).toByteArray()

        client.sendPayload(endpoint, Payload.fromBytes(bytes))
    }

    private fun checkConnected()
    {
        check(connectedEndpointId != null)
        {
            "Not connected"
        }
    }
}