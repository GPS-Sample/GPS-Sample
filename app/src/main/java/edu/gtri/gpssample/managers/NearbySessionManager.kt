package edu.gtri.gpssample.managers

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.Image
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.*
import java.util.UUID

/**
 * ============================================================================
 * Models
 * ============================================================================
 */

@Serializable
data class Request(
    val command: Command,
    val key: String? = null
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

class NearbySessionManager( private val context: Context, private val scope: CoroutineScope, private val config: Config? )
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

    // =========================================================================
    // HOST API
    // =========================================================================

    fun startHosting(): String
    {
        val id = UUID.randomUUID().toString()
        sessionId = id

        val options = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
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
            throw(it)
//            _nearbySessionState.value = NearbySessionState.Error("Advertising failed", it)
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

    // =========================================================================
    // CLIENT API
    // =========================================================================

    fun clientConnect(sessionId: String)
    {
        _nearbySessionState.value = NearbySessionState.Connecting

        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
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

        sendRequest( Request( command = Command.GET_IMAGE, key = imageId ))

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

    private val hostConnectionCallback =
        object : ConnectionLifecycleCallback()
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
                Log.d( "xxx", "Connection Disconnected" )

                stopHosting()
                startHosting()
            }
        }

    // =========================================================================
    // CLIENT CALLBACK
    // =========================================================================

    private val clientConnectionCallback =
        object : ConnectionLifecycleCallback()
        {
            override fun onConnectionInitiated( endpointId: String, connectionInfo: ConnectionInfo )
            {
                Log.d( "xxx", "Connection Accepted" )
                client.acceptConnection(endpointId, clientPayloadCallback )
            }

            override fun onConnectionResult( endpointId: String, result: ConnectionResolution )
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
                Log.d( "xxx", "Connection Disconnected" )
                connectedEndpointId = null
                _nearbySessionState.value = NearbySessionState.Idle
            }
        }

    // =========================================================================
    // HOST PAYLOAD HANDLER
    // =========================================================================

    private val hostPayloadCallback =
        object : PayloadCallback()
        {
            override fun onPayloadReceived( endpointId: String, payload: Payload )
            {
                if (payload.type != Payload.Type.BYTES) return

                val request = json.decodeFromString(Request.serializer(), String(payload.asBytes()!!))

                when (request.command)
                {
                    Command.GET_CONFIG -> sendConfig()
                    Command.GET_IMAGE -> sendImage(request.key!!)
                    Command.DONE -> {
                        client.disconnectFromEndpoint(endpointId)
//                        stopHosting()
//                        startHosting()
//                        _nearbySessionState.value = NearbySessionState.Advertising( sessionId!! )
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

    private val clientPayloadCallback =
        object : PayloadCallback()
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

        scope.launch(Dispatchers.IO) {
            try {
                json.encodeToStream(Config.serializer(), config!!, output )
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

            scope.launch(Dispatchers.IO) {
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
        scope.launch(Dispatchers.IO) {
            val config = json.decodeFromStream(
                Config.serializer(),
                input
            )

            configDeferred?.complete(config)
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun receiveImage(input: InputStream)
    {
        scope.launch(Dispatchers.IO) {
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