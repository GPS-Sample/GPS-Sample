package edu.gtri.gpssample.managers

import android.content.Context
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import edu.gtri.gpssample.database.DAO
import edu.gtri.gpssample.database.ImageDAO
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.database.models.EnumArea
import edu.gtri.gpssample.database.models.Image
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

    private val imageIds = ArrayList<String>()

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
        ENUMERATION_AREAS,
        IMAGE
    }

    private var pendingRequest: PendingRequest? = null

    private var configDeferred: CompletableDeferred<Config>? = null
    private var enumItemsDeferred: CompletableDeferred<Unit>? = null
    private var imageDeferred: CompletableDeferred<Image>? = null

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun connect(sessionId: String, completion: (Config) -> Unit)
    {
        if (clientJob != null) return

        clientJob = scope.launch {
            try {
                runClient(sessionId, completion)
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
    // Main flow
    // -------------------------------------------------------------------------

    private suspend fun runClient(sessionId: String, completion: (Config) -> Unit)
    {
        _state.value = NearbySessionState.Connecting

        startDiscovery(sessionId)
        waitForConnection()

        _state.value = NearbySessionState.Connected

        val config = requestConfig()

        // EnumAreas are saved directly to the database
        requestEnumerationAreas()

        // Images are saved directly to the database
        requestImages(config)

        withContext(Dispatchers.Main) {
            completion(config)
        }

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

        client.startDiscovery(NearbySessionCore.SERVICE_ID, discoveryCallback(sessionId), options )
            .addOnSuccessListener {
                Log.d("xxx", "Discovery started")
            }
            .addOnFailureListener {
                Log.d("xxx", "Discovery failed", it)
            }
    }

    private fun stopDiscovery()
    {
        client.stopDiscovery()
    }

    private fun discoveryCallback(sessionId: String) =
        object : EndpointDiscoveryCallback()
        {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo)
            {
                Log.d("xxx","Endpoint found. endpointId=$endpointId endpointName=${info.endpointName}")

                if (info.endpointName != sessionId) return

                stopDiscovery()

                client.requestConnection("client", endpointId, connectionCallback)
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
            client.acceptConnection(endpointId, payloadCallback)
        }

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

            val stream = payload.asStream() ?: return
            val input = stream.asInputStream()

            when (pendingRequest)
            {
                PendingRequest.CONFIG ->
                    receiveConfig(input)

                PendingRequest.ENUMERATION_AREAS ->
                    receiveEnumerationAreas(input)

                PendingRequest.IMAGE ->
                    receiveImage(input)

                null ->
                    Log.d("xxx", "Unexpected payload")
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate)
        {
            if (update.status == PayloadTransferUpdate.Status.FAILURE)
            {
                Log.d("xxx","PayloadTransfer failed. id=${update.payloadId}")
            }
        }
    }

    // -------------------------------------------------------------------------
    // CONFIG
    // -------------------------------------------------------------------------

    private suspend fun requestConfig(): Config
    {
        pendingRequest = PendingRequest.CONFIG
        val deferred = CompletableDeferred<Config>()
        configDeferred = deferred

        sendRequest(Command.GET_CONFIG)

        _state.value = NearbySessionState.ReceivingConfig

        return try {
            deferred.await()
        } finally {
            pendingRequest = null
            configDeferred = null
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun receiveConfig(input: InputStream)
    {
        scope.launch(Dispatchers.IO) {
            input.use {
                try {
                    val config = json.decodeFromStream(Config.serializer(), it)
                    configDeferred?.complete(config)
                } catch( ex: Exception ) {}
            }
        }
    }

    // -------------------------------------------------------------------------
    // ENUMERATION ITEMS (NEW)
    // -------------------------------------------------------------------------

    private suspend fun requestEnumerationAreas(): Unit
    {
        pendingRequest = PendingRequest.ENUMERATION_AREAS
        val deferred = CompletableDeferred<Unit>()
        enumItemsDeferred = deferred

        _state.value = NearbySessionState.ReceivingEnumerationAreas

        sendRequest(Command.GET_ENUMERATION_AREAS)

        return try {
            deferred.await()
        } finally {
            pendingRequest = null
            enumItemsDeferred = null
        }
    }

    private fun receiveEnumerationAreas(input: InputStream)
    {
        imageIds.clear()

        scope.launch(Dispatchers.IO)
        {
            try {
                DAO.instance().writableDatabase.beginTransaction()

                input.bufferedReader().useLines { lines ->
                    for (line in lines)
                    {
                        if (line.isBlank()) continue

                        try {
                            val enumArea = json.decodeFromString(EnumArea.serializer(), line)

                            for (location in enumArea.locations)
                            {
                                if (location.imageUuid.isNotEmpty() && ImageDAO.instance().doesNotExist( location.imageUuid ))
                                {
                                    imageIds.add( location.imageUuid )
                                }
                            }

                            DAO.enumAreaDAO.createOrUpdateEnumArea( enumArea, enumArea.version )
                        }
                        catch (ex: Exception)
                        {
                            Log.d("xxx", "Failed to parse item line: $line", ex)
                        }
                    }
                }

                DAO.instance().writableDatabase.setTransactionSuccessful()

                enumItemsDeferred?.complete(Unit )
            }
            catch (ex: Exception)
            {
                Log.d("xxx", "Enumeration stream failed", ex)
                enumItemsDeferred?.completeExceptionally(ex)
            }
            finally
            {
                try {
                    DAO.instance().writableDatabase.endTransaction()
                    input.close()
                } catch (_: Exception) {}
            }
        }
    }

    // -------------------------------------------------------------------------
    // IMAGES
    // -------------------------------------------------------------------------

    private suspend fun requestImages(config: Config) {
        try {
            for ((index,uuid) in imageIds.withIndex())
            {
                pendingRequest = PendingRequest.IMAGE
                val deferred = CompletableDeferred<Image>()
                imageDeferred = deferred

                sendRequest( Command.GET_IMAGE,uuid )

                _state.value = NearbySessionState.Message("Requesting Image ${index+1}/${imageIds.count()}" )

                val image = try {
                    deferred.await()
                } finally {
                    pendingRequest = null
                    imageDeferred = null
                }

                ImageDAO.instance().createImage(image)
            }
        }
        catch (ex: Exception) {
            Log.d( "xxx", ex.stackTraceToString())
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun receiveImage(input: InputStream)
    {
        scope.launch(Dispatchers.IO) {
            input.use {
                try {
                    val image = json.decodeFromStream(Image.serializer(), it)
                    imageDeferred?.complete(image)
                } catch( ex: Exception ) {}
            }
        }
    }

    // -------------------------------------------------------------------------
    // Send helpers
    // -------------------------------------------------------------------------

    private fun sendRequest(command: Command, imageUuid: String? = null)
    {
        val endpoint = connectedEndpointId ?: return

        val req = Request(command, imageUuid)
        val bytes = json.encodeToString(Request.serializer(), req).toByteArray()

        client.sendPayload(endpoint, Payload.fromBytes(bytes))
    }

    private fun sendDone()
    {
        sendRequest(Command.DONE)
    }

    // -------------------------------------------------------------------------
    // Cleanup
    // -------------------------------------------------------------------------

    private fun disconnect()
    {
        connectedEndpointId?.let {
            client.disconnectFromEndpoint(it)
        }
        connectedEndpointId = null
    }

    private fun cleanup()
    {
        stopDiscovery()
        disconnect()

        connectDeferred?.cancel()
        configDeferred?.cancel()
        enumItemsDeferred?.cancel()
        imageDeferred?.cancel()

        pendingRequest = null

        _state.value = NearbySessionState.Idle
    }
}