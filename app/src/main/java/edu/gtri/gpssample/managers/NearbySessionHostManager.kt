package edu.gtri.gpssample.managers

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumerationItem
import edu.gtri.gpssample.database.models.Image
import edu.gtri.gpssample.managers.NearbySessionCore.Companion.SERVICE_ID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.*
import java.util.UUID

class NearbySessionHostManager(
    private val context: Context,
    private val config: Config
) {
    // -------------------------------------------------------------------------
    // Nearby
    // -------------------------------------------------------------------------

    private val client: ConnectionsClient =
        Nearby.getConnectionsClient(context)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private val _state =
        MutableStateFlow<NearbySessionState>(NearbySessionState.Idle)

    val state: StateFlow<NearbySessionState> = _state.asStateFlow()

    // -------------------------------------------------------------------------
    // Scope
    // -------------------------------------------------------------------------

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var hostJob: Job? = null

    // -------------------------------------------------------------------------
    // Session
    // -------------------------------------------------------------------------

    private var sessionId: String? = null
    private var connectedEndpointId: String? = null

    // -------------------------------------------------------------------------
    // SINGLE TRANSMISSION LOCK (critical)
    // -------------------------------------------------------------------------

    private val transferMutex = Mutex()

    // -------------------------------------------------------------------------
    // ACK tracking
    // -------------------------------------------------------------------------

    private var ackDeferred: CompletableDeferred<Unit>? = null

    private suspend fun awaitAck() = ackDeferred?.await()

    private fun signalAck() {
        ackDeferred?.complete(Unit)
    }

    // -------------------------------------------------------------------------
    // Start / Stop
    // -------------------------------------------------------------------------

    fun startHosting() {
        if (hostJob != null) return

        hostJob = scope.launch {
            try {
                runHostLoop()
            } finally {
                cleanup()
            }
        }
    }

    fun stopHosting() {
        hostJob?.cancel()
        hostJob = null
    }

    fun shutdown() {
        stopHosting()
        scope.cancel()
    }

    // -------------------------------------------------------------------------
    // Main loop
    // -------------------------------------------------------------------------

    private suspend fun runHostLoop() {
        while (scope.isActive) {

            val id = UUID.randomUUID().toString()
            sessionId = id

            startAdvertising(id)
            _state.value = NearbySessionState.Advertising(id)

            waitForConnection()

            _state.value = NearbySessionState.Connected

            waitForDisconnect()

            connectedEndpointId = null
        }
    }

    // -------------------------------------------------------------------------
    // Advertising
    // -------------------------------------------------------------------------

    private fun startAdvertising(id: String) {
        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_STAR)
            .build()

        client.startAdvertising(
            id,
            NearbySessionCore.SERVICE_ID,
            connectionCallback,
            options
        )
    }

    private fun stopAdvertising() {
        client.stopAdvertising()
    }

    // -------------------------------------------------------------------------
    // Connection sync
    // -------------------------------------------------------------------------

    private var connectionDeferred = CompletableDeferred<Unit>()
    private var disconnectDeferred = CompletableDeferred<Unit>()

    private suspend fun waitForConnection() {
        connectionDeferred = CompletableDeferred()
        connectionDeferred.await()
    }

    private fun signalConnected() {
        connectionDeferred.complete(Unit)
    }

    private suspend fun waitForDisconnect() {
        disconnectDeferred = CompletableDeferred()
        disconnectDeferred.await()
    }

    private fun signalDisconnected() {
        disconnectDeferred.complete(Unit)
    }

    // -------------------------------------------------------------------------
    // Connection callback
    // -------------------------------------------------------------------------

    private val connectionCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(
            endpointId: String,
            connectionInfo: ConnectionInfo
        ) {
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(
            endpointId: String,
            result: ConnectionResolution
        ) {
            if (result.status.isSuccess) {
                connectedEndpointId = endpointId
                stopAdvertising()
                signalConnected()
            } else {
                _state.value = NearbySessionState.Error("Connection failed")
            }
        }

        override fun onDisconnected(endpointId: String) {
            signalDisconnected()
        }
    }

    // -------------------------------------------------------------------------
    // Payload handler
    // -------------------------------------------------------------------------

    private val payloadCallback = object : PayloadCallback() {

        override fun onPayloadReceived(endpointId: String, payload: Payload) {

            if (payload.type != Payload.Type.BYTES) return

            val request = json.decodeFromString(
                Request.serializer(),
                String(payload.asBytes()!!)
            )

            scope.launch {
                when (request.command) {

                    Command.GET_CONFIG -> {
                        sendConfig(endpointId)
                        sendAck(endpointId, Command.ACK_CONFIG)
                    }

                    Command.GET_ENUMERATION_ITEMS -> {
                        sendEnumerationItems(endpointId)
                        sendAck(endpointId, Command.ACK_ENUMERATION_ITEMS)
                    }

                    Command.GET_IMAGE -> {
                        sendImage(endpointId, request.imageUuid!!)
                        sendAck(endpointId, Command.ACK_IMAGE)
                    }

                    Command.DONE -> {
                        client.disconnectFromEndpoint(endpointId)
                    }

                    else -> Unit
                }
            }
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) = Unit
    }

    // -------------------------------------------------------------------------
    // CONFIG
    // -------------------------------------------------------------------------

    private suspend fun sendConfig(endpointId: String) =
        transferMutex.withLock {

            _state.value = NearbySessionState.SendingConfig

            val output = PipedOutputStream()
            val input = PipedInputStream(output, 64 * 1024)

            client.sendPayload(endpointId, Payload.fromStream(input))

            withContext(Dispatchers.IO) {
                try {
                    json.encodeToStream(Config.serializer(), config, output)
                } finally {
                    output.close()
                }
            }
        }

    // -------------------------------------------------------------------------
    // ENUMERATION ITEMS (streaming NDJSON)
    // -------------------------------------------------------------------------

    private suspend fun sendEnumerationItems(endpointId: String) =
        transferMutex.withLock {
            _state.value = NearbySessionState.SendingEnumerationItems

            val output = PipedOutputStream()
            val input = PipedInputStream(output, 64 * 1024)

            client.sendPayload(endpointId, Payload.fromStream(input))

            withContext(Dispatchers.IO)
            {
                val db = DAO.instance().readableDatabase
                val cursor = db.rawQuery("SELECT * FROM enumeration_item", null)

                try {
                    while (cursor.moveToNext())
                    {
                        val jsonLine = DAO.enumerationItemDAO.buildJson( cursor )
                        output.write(jsonLine.toByteArray())
                        output.write('\n'.code)
                    }

                    output.flush()
                } finally {
                    cursor.close()
                    output.close()
                }
            }
        }

    // -------------------------------------------------------------------------
    // IMAGE
    // -------------------------------------------------------------------------

    private suspend fun sendImage(endpointId: String, imageId: String) =
        transferMutex.withLock {

            _state.value = NearbySessionState.SendingImage

            val image =
                ImageDAO.instance().getImage(imageId) ?: return

            val output = PipedOutputStream()
            val input = PipedInputStream(output, 64 * 1024)

            client.sendPayload(endpointId, Payload.fromStream(input))

            withContext(Dispatchers.IO) {
                try {
                    json.encodeToStream(Image.serializer(), image, output)
                } finally {
                    output.close()
                }
            }
        }

    // -------------------------------------------------------------------------
    // ACK
    // -------------------------------------------------------------------------

    private fun sendAck(endpointId: String, ack: Command) {
        val bytes = json.encodeToString(
            Request.serializer(),
            Request(ack)
        ).toByteArray()

        client.sendPayload(endpointId, Payload.fromBytes(bytes))
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    private fun cleanup() {

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