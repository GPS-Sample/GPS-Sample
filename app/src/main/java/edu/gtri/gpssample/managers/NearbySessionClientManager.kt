package edu.gtri.gpssample.managers

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Image
import edu.gtri.gpssample.managers.NearbySessionCore.Companion.SERVICE_ID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.*

class NearbySessionClientManager(private val context: Context)
{
    // -------------------------------------------------------------------------
    // Google Nearby
    // -------------------------------------------------------------------------

    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private val _state = MutableStateFlow<NearbySessionState>(NearbySessionState.Idle)

    val state: StateFlow<NearbySessionState> = _state.asStateFlow()

    // -------------------------------------------------------------------------
    // Scope
    // -------------------------------------------------------------------------

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var clientJob: Job? = null

    // -------------------------------------------------------------------------
    // Connection tracking
    // -------------------------------------------------------------------------

    private var connectedEndpointId: String? = null

    private var connectDeferred: CompletableDeferred<Unit>? = null

    // -------------------------------------------------------------------------
    // Request tracking
    // -------------------------------------------------------------------------

    private enum class PendingRequest {
        CONFIG,
        IMAGE
    }

    private var pendingRequest: PendingRequest? = null
    private var configDeferred: CompletableDeferred<Config>? = null
    private var imageDeferred: CompletableDeferred<Image>? = null

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun connect(sessionId: String, completion: ((config: Config)->Unit))
    {
        if (clientJob != null) return

        clientJob = scope.launch {
            try {
                runClient( sessionId, completion )
            } catch (ce: CancellationException) {
                // expected
            } catch (e: Exception) {
                _state.value = NearbySessionState.Error(e.message ?: "Client error", e)
            } finally {
                cleanup()
            }
        }
    }

    fun cancel()
    {
        clientJob?.cancel()
        clientJob = null

        cleanup()
    }

    fun shutdown()
    {
        cancel()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Client workflow
    // -------------------------------------------------------------------------

    private suspend fun runClient(sessionId: String, completion: ((config: Config)->Unit))
    {
        _state.value = NearbySessionState.Connecting

        startDiscovery(sessionId)

        waitForConnection()

        _state.value = NearbySessionState.Connected

        val config = requestConfig()

        downloadImages(config)

        completion( config )

        sendDone()

        disconnect()

        _state.value = NearbySessionState.Idle
    }

    // -------------------------------------------------------------------------
    // Discovery
    // -------------------------------------------------------------------------

    private fun startDiscovery(sessionId: String)
    {
        val options = DiscoveryOptions.Builder().setStrategy(Strategy.P2P_STAR).build()

        client.startDiscovery(SERVICE_ID, discoveryCallback(sessionId), options)
    }

    private fun stopDiscovery()
    {
        client.stopDiscovery()
    }

    private fun discoveryCallback(sessionId: String) = object : EndpointDiscoveryCallback()
    {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo)
        {
            if (info.endpointName != sessionId) return

            stopDiscovery()

            client.requestConnection("Client", endpointId, connectionCallback)
        }

        override fun onEndpointLost(endpointId: String) {}
    }

    // -------------------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------------------

    private val connectionCallback = object : ConnectionLifecycleCallback()
    {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo)
        {
            client.acceptConnection(endpointId, payloadCallback) }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution)
            {
                if (result.status.isSuccess)
                {
                    connectedEndpointId = endpointId
                    connectDeferred?.complete(Unit)
                }
            }

            override fun onDisconnected(endpointId: String)
            {
                connectedEndpointId = null
                _state.value = NearbySessionState.Idle
            }
        }

    private suspend fun waitForConnection()
    {
        connectDeferred = CompletableDeferred()
        connectDeferred!!.await()
    }

    // -------------------------------------------------------------------------
    // Payload handling
    // -------------------------------------------------------------------------

    private val payloadCallback = object : PayloadCallback()
    {
            override fun onPayloadReceived(endpointId: String, payload: Payload)
            {
                if (payload.type != Payload.Type.STREAM) return

                val input = payload.asStream()?.asInputStream() ?: return

                when (pendingRequest)
                {
                    PendingRequest.CONFIG ->
                        receiveConfig(input)

                    PendingRequest.IMAGE ->
                        receiveImage(input)

                    null ->
                        Log.d("xxx", "Unexpected payload")
                }
            }

            override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
        }

    // -------------------------------------------------------------------------
    // Config request
    // -------------------------------------------------------------------------

    private suspend fun requestConfig(): Config
    {
        pendingRequest = PendingRequest.CONFIG

        val deferred = CompletableDeferred<Config>()

        configDeferred = deferred

        sendRequest(Request(Command.GET_CONFIG))

        _state.value = NearbySessionState.ReceivingConfig

        return try {
            deferred.await()
        } finally {
            pendingRequest = null
            configDeferred = null
        }
    }

    // -------------------------------------------------------------------------
    // Image request loop
    // -------------------------------------------------------------------------

    private suspend fun downloadImages(config: Config)
    {
        for (enumArea in config.enumAreas)
        {
            for (location in enumArea.locations)
            {
                val id = location.imageUuid

                if (id.isEmpty()) continue

                if (ImageDAO.instance().doesNotExist(id))
                {
                    pendingRequest = PendingRequest.IMAGE

                    val deferred = CompletableDeferred<Image>()

                    imageDeferred = deferred

                    sendRequest(Request(Command.GET_IMAGE, id))

                    _state.value = NearbySessionState.ReceivingImages

                    val image = try {
                        deferred.await()
                    } finally {
                        pendingRequest = null
                        imageDeferred = null
                    }

                    ImageDAO.instance().createImage(image)
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Receive handlers
    // -------------------------------------------------------------------------

    @OptIn(ExperimentalSerializationApi::class)
    private fun receiveConfig(input: InputStream)
    {
        scope.launch(Dispatchers.IO) {
            try {
                val config = json.decodeFromStream(Config.serializer(), input)
                configDeferred?.complete(config)
            } catch( ex: Exception ) {
                Log.d( "xxx", ex.stackTraceToString())
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun receiveImage(input: InputStream)
    {
        scope.launch(Dispatchers.IO) {
            try {
                val image = json.decodeFromStream(Image.serializer(), input)
                imageDeferred?.complete(image)
            } catch( ex: Exception ) {
                Log.d( "xxx", ex.stackTraceToString())
            }
        }
    }

    // -------------------------------------------------------------------------
    // Send helpers
    // -------------------------------------------------------------------------

    private fun sendRequest(request: Request)
    {
        val endpoint = connectedEndpointId ?: return

        try {
            val bytes = json.encodeToString(Request.serializer(), request).toByteArray()
            client.sendPayload(endpoint, Payload.fromBytes(bytes))
        } catch( ex: Exception ) {
            Log.d( "xxx", ex.stackTraceToString())
        }
    }

    private fun sendDone()
    {
        sendRequest(Request(Command.DONE))
    }

    private fun disconnect()
    {
        connectedEndpointId?.let {
            client.disconnectFromEndpoint(it)
        }

        connectedEndpointId = null
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    private fun cleanup()
    {
        stopDiscovery()

        disconnect()

        connectDeferred?.cancel()
        configDeferred?.cancel()
        imageDeferred?.cancel()

        pendingRequest = null

        _state.value = NearbySessionState.Idle
    }
}