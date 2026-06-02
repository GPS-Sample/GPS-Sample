package edu.gtri.gpssample.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import edu.gtri.gpssample.constants.NearbyConfig
import edu.gtri.gpssample.database.models.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

class NearbyReceiver(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onConfig: (Config) -> Unit,
    private val json: Json
) {
    private val client = Nearby.getConnectionsClient(context)

    fun startDiscovery(sessionId: String) {

        val options = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_POINT_TO_POINT)
            .build()

        client.startDiscovery(
            NearbyConfig.SERVICE_ID,
            endpointCallback(sessionId),
            options
        )
    }

    private fun endpointCallback(
        expectedSessionId: String
    ) = object : EndpointDiscoveryCallback() {

        override fun onEndpointFound(
            endpointId: String,
            info: DiscoveredEndpointInfo
        ) {

            if (info.endpointName != expectedSessionId) return

            client.requestConnection(
                Build.MODEL,
                endpointId,
                connectionCallback
            )
        }

        override fun onEndpointLost(endpointId: String) {}
    }

    private val connectionCallback =
        object : ConnectionLifecycleCallback() {

            override fun onConnectionInitiated(
                endpointId: String,
                connectionInfo: ConnectionInfo
            ) {
                client.acceptConnection(
                    endpointId,
                    payloadCallback
                )
            }

            override fun onConnectionResult(
                endpointId: String,
                result: ConnectionResolution
            ) {}

            override fun onDisconnected(endpointId: String) {}
        }

    private val payloadCallback =
        object : PayloadCallback() {

            override fun onPayloadReceived(
                endpointId: String,
                payload: Payload
            ) {

                if (payload.type != Payload.Type.STREAM) return

                val input =
                    payload.asStream()
                        ?.asInputStream()
                        ?: return

                scope.launch(Dispatchers.IO) {

                    val config = json.decodeFromStream<Config>(input)

                    withContext(Dispatchers.Main) {
                        onConfig(config)
                    }
                }
            }

            override fun onPayloadTransferUpdate(
                endpointId: String,
                update: PayloadTransferUpdate
            ) {
                when (update.status) {
                    PayloadTransferUpdate.Status.SUCCESS ->
                        Log.d("Nearby", "Transfer complete")

                    PayloadTransferUpdate.Status.FAILURE ->
                        Log.e("Nearby", "Transfer failed")
                }
            }
        }
}