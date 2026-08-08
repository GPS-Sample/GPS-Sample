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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.*
import java.util.UUID

class NearbySessionHostManager( private val context: Context, private val config: Config )
{
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
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
    // Start / Stop
    // -------------------------------------------------------------------------

    fun startHosting()
    {
        if (hostJob != null) return

        hostJob = scope.launch {
            try {
                runHostLoop()
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
    // Main loop
    // -------------------------------------------------------------------------

    private suspend fun runHostLoop()
    {
        while (scope.isActive)
        {
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

    private fun startAdvertising(id: String)
    {
        val options = AdvertisingOptions.Builder().setStrategy(Strategy.P2P_STAR).build()

        client.startAdvertising(id, NearbySessionCore.SERVICE_ID, connectionCallback, options)
            .addOnSuccessListener {
                Log.d("xxx", "Advertising started. SessionId=$id")
            }
            .addOnFailureListener { ex ->
                Log.d("xxx", "Advertising failed. SessionId=$id", ex)
            }
    }

    private fun stopAdvertising()
    {
        client.stopAdvertising()
    }

    // -------------------------------------------------------------------------
    // Connection sync
    // -------------------------------------------------------------------------

    private var connectionDeferred = CompletableDeferred<Unit>()
    private var disconnectDeferred = CompletableDeferred<Unit>()

    private suspend fun waitForConnection()
    {
        connectionDeferred = CompletableDeferred()
        connectionDeferred.await()
    }

    private fun signalConnected()
    {
        connectionDeferred.complete(Unit)
    }

    private suspend fun waitForDisconnect()
    {
        disconnectDeferred = CompletableDeferred()
        disconnectDeferred.await()
    }

    private fun signalDisconnected()
    {
        disconnectDeferred.complete(Unit)
    }

    // -------------------------------------------------------------------------
    // Connection callback
    // -------------------------------------------------------------------------

    private val connectionCallback = object : ConnectionLifecycleCallback()
    {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo)
        {
            Log.d("xxx","Connection initiated. endpointId=$endpointId endpointName=${connectionInfo.endpointName}")

            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution)
        {
            Log.d("xxx", "Connection result. endpointId=$endpointId status=${result.status.statusCode}")

            if (result.status.isSuccess)
            {
                connectedEndpointId = endpointId
                stopAdvertising()
                signalConnected()
            }
            else
            {
                _state.value = NearbySessionState.Error("Connection failed")
            }
        }

        override fun onDisconnected(endpointId: String)
        {
            signalDisconnected()
        }
    }

    // -------------------------------------------------------------------------
    // Payload handler
    // -------------------------------------------------------------------------

    private val payloadCallback = object : PayloadCallback()
    {
        override fun onPayloadReceived(endpointId: String, payload: Payload)
        {
            if (payload.type != Payload.Type.BYTES) return

            val request = json.decodeFromString(Request.serializer(), String(payload.asBytes()!!))

            scope.launch {
                when (request.command)
                {
                    Command.GET_CONFIG -> {
                        sendConfig(endpointId)
                    }

                    Command.GET_ENUMERATION_AREAS -> {
                        sendEnumerationAreas(endpointId)
                    }

                    Command.GET_IMAGE -> {
                        sendImage(endpointId, request.imageUuid!!)
                    }

                    Command.DONE -> {
                        client.disconnectFromEndpoint(endpointId)
                    }
                }
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

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun sendConfig(endpointId: String) =
        transferMutex.withLock {
            _state.value = NearbySessionState.SendingConfig

            val output = PipedOutputStream()
            val input = PipedInputStream(output, 64 * 1024)

            client.sendPayload(endpointId, Payload.fromStream(input))

            try {
                json.encodeToStream(Config.serializer(), config, output)
            } catch( ex: Exception ) { } finally { try { output.close() } catch( ex: Exception ) {}}
        }

    // -------------------------------------------------------------------------
    // ENUMERATION AREAS (streaming NDJSON)
    // -------------------------------------------------------------------------

    private suspend fun sendEnumerationAreas(endpointId: String) =
        transferMutex.withLock {
            val output = PipedOutputStream()
            val input = PipedInputStream(output, 64 * 1024)
            client.sendPayload(endpointId, Payload.fromStream(input))

            try {
                if (config.enumAreas.isEmpty())
                {
                    output.write('\n'.code )
                }
                else if (config.enumAreas.size == 1)
                {
                    _state.value = NearbySessionState.Message("Sending EnumArea 1/1" )
                    val jsonLine = json.encodeToString(EnumArea.serializer(),config.enumAreas.first()) + "\n"
                    output.write(jsonLine.toByteArray())
                }
                else
                {
                    val db = DAO.instance().readableDatabase
                    val query = "SELECT ${DAO.COLUMN_UUID} FROM ${DAO.TABLE_ENUM_AREA} WHERE ${DAO.COLUMN_CONFIG_UUID} = '${config.uuid}' ORDER BY ${DAO.COLUMN_CREATION_DATE} ASC"

                    db.rawQuery(query, null).use { cursor ->
                        val numItems = cursor.count
                        var count = 1

                        if (numItems > 0) {
                            while (cursor.moveToNext()) {
                                _state.value = NearbySessionState.Message("Sending EnumArea ${count++}/${numItems}")
                                val uuid = cursor.getString(cursor.getColumnIndexOrThrow(DAO.COLUMN_UUID))
                                DAO.enumAreaDAO.getEnumArea(uuid)?.let { enumArea ->
                                    val jsonLine = json.encodeToString(EnumArea.serializer(), enumArea) + "\n"
                                    output.write(jsonLine.toByteArray())
                                }
                            }
                        }
                    }
                }
            } catch( ex: Exception ) {
                Log.d( "xxx", "xxx" )
            } finally {
                try {
                    output.close()
                }
                catch( ex: Exception ) {
                    Log.d( "xxx", "xxx" )
                }
            }
        }

    // -------------------------------------------------------------------------
    // IMAGE
    // -------------------------------------------------------------------------

    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun sendImage(endpointId: String, imageId: String) =
        transferMutex.withLock {
            _state.value = NearbySessionState.SendingImage

            val image = ImageDAO.instance().getImage(imageId) ?: return@withLock

            val output = PipedOutputStream()
            val input = PipedInputStream(output, 64 * 1024)

            client.sendPayload(endpointId, Payload.fromStream(input))

            try {
                json.encodeToStream(Image.serializer(), image, output)
            }
            catch( ex: Exception ) {}
            finally {
                try {
                    output.close()
                }
                catch( ex: Exception ) {}}
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