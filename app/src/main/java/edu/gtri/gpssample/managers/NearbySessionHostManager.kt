package edu.gtri.gpssample.managers

import android.content.Context
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.*
import java.util.UUID

class NearbySessionHostManager( private val context: Context, private val config: Config )
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
    // Scope / lifecycle
    // -------------------------------------------------------------------------

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var hostJob: Job? = null

    // -------------------------------------------------------------------------
    // Session tracking
    // -------------------------------------------------------------------------

    private var sessionId: String? = null
    private var connectedEndpointId: String? = null
    private var disconnectDeferred: CompletableDeferred<Unit>? = null

    // -------------------------------------------------------------------------
    // Start / Stop Hosting
    // -------------------------------------------------------------------------

    fun startHosting()
    {
        if (hostJob != null) return

        hostJob = scope.launch {
            try {
                runHostLoop()
            } catch (e: CancellationException) {
                // normal shutdown
            } catch (e: Exception) {
                _state.value = NearbySessionState.Error(message = e.message ?: "Host error", throwable = e)
            } finally {
                cleanup()
            }
        }
    }

    fun stopHosting()
    {
        hostJob?.cancel()
        hostJob = null
    }

    fun shutdown()
    {
        stopHosting()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Host loop (core state machine)
    // -------------------------------------------------------------------------

    private suspend fun runHostLoop()
    {
        while (scope.isActive)
        {
            val id = UUID.randomUUID().toString()

            sessionId = id

            startAdvertising(id)

            _state.value = NearbySessionState.Advertising(id)

            waitForClientConnection()

            _state.value = NearbySessionState.Connected

            waitForClientDisconnect()

            connectedEndpointId = null
        }
    }

    // -------------------------------------------------------------------------
    // Advertising
    // -------------------------------------------------------------------------

    private fun startAdvertising(id: String)
    {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        client.startAdvertising(
            id,
            SERVICE_ID,
            connectionCallback,
            options
        ).addOnFailureListener {
            _state.value =
                NearbySessionState.Error(
                    "Advertising failed",
                    it
                )
        }
    }

    private fun stopAdvertising()
    {
        client.stopAdvertising()
    }

    // -------------------------------------------------------------------------
    // Connection wait
    // -------------------------------------------------------------------------

    private var connectionDeferred = CompletableDeferred<Unit>()

    private suspend fun waitForClientConnection()
    {
        connectionDeferred = CompletableDeferred()
        connectionDeferred.await()
    }

    private fun signalConnected()
    {
        connectionDeferred.complete(Unit)
    }

    // -------------------------------------------------------------------------
    // Disconnect wait
    // -------------------------------------------------------------------------

    private suspend fun waitForClientDisconnect()
    {
        disconnectDeferred = CompletableDeferred()
        disconnectDeferred!!.await()
    }

    private fun signalDisconnected()
    {
        disconnectDeferred?.complete(Unit)
    }

    // -------------------------------------------------------------------------
    // Connection callbacks
    // -------------------------------------------------------------------------

    private val connectionCallback = object : ConnectionLifecycleCallback()
    {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo)
        {
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution)
        {
            if (result.status.isSuccess)
            {
                connectedEndpointId = endpointId
                stopAdvertising()
                signalConnected()
            }
            else
            {
                _state.value = NearbySessionState.Error("Connection failed" )
            }
        }

        override fun onDisconnected(endpointId: String)
        {
            signalDisconnected()
        }
    }

    // -------------------------------------------------------------------------
    // Payload handling (HOST side)
    // -------------------------------------------------------------------------

    private val payloadCallback = object : PayloadCallback()
    {
        override fun onPayloadReceived(endpointId: String, payload: Payload)
        {
            if (payload.type != Payload.Type.BYTES) return

            val request = json.decodeFromString(Request.serializer(), String(payload.asBytes()!!))

            when (request.command)
            {
                Command.GET_CONFIG -> {
                    _state.value = NearbySessionState.SendingConfig
                    sendConfig(endpointId)
                }

                Command.GET_IMAGE -> {
                    _state.value = NearbySessionState.SendingImage
                    request.imageUuid?.let { sendImage(endpointId, it) }
                }

                Command.DONE -> {
                    _state.value = NearbySessionState.Done
                    client.disconnectFromEndpoint(endpointId)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) = Unit
    }

    // -------------------------------------------------------------------------
    // Send Config
    // -------------------------------------------------------------------------

    private fun sendConfig(endpointId: String)
    {
        val output = PipedOutputStream()
        val input = PipedInputStream(output, 64 * 1024)

        client.sendPayload(endpointId, Payload.fromStream(input))

        scope.launch(Dispatchers.IO) {
            try {
                json.encodeToStream(Config.serializer(), config, output)
            } finally {
                output.close()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Send Image
    // -------------------------------------------------------------------------

    private fun sendImage(endpointId: String, imageId: String)
    {
        val image = ImageDAO.instance().getImage(imageId) ?: return

        val output = PipedOutputStream()
        val input = PipedInputStream(output, 64 * 1024)

        client.sendPayload(endpointId, Payload.fromStream(input))

        scope.launch(Dispatchers.IO) {
            try {
                json.encodeToStream(Image.serializer(), image, output)
            } finally {
                output.close()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    private fun cleanup()
    {
        stopAdvertising()

        connectedEndpointId?.let {
            try {
                client.disconnectFromEndpoint(it)
            } catch (_: Exception) {}
        }

        connectedEndpointId = null

        sessionId = null

        _state.value = NearbySessionState.Idle
    }
}