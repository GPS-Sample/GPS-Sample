package edu.gtri.gpssample.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import java.util.UUID
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import edu.gtri.gpssample.database.models.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToStream
import java.io.PipedInputStream
import java.io.PipedOutputStream

class NearbySender(
    private val context: Context,
    private val scope: CoroutineScope,
    private val json: Json )
{
//    private val client = Nearby.getConnectionsClient( context )
//
//    private var endpointId: String? = null
//
//    fun startAdvertising() {
//
//        val sessionId = UUID.randomUUID().toString()
//
//        showQrCode(sessionId)
//
//        val options = AdvertisingOptions.Builder()
//            .setStrategy(Strategy.P2P_POINT_TO_POINT)
//            .build()
//
//        client.startAdvertising(
//            sessionId,
//            NearbyConfig.SERVICE_ID,
//            connectionCallback,
//            options
//        )
//    }
//
//    fun sendConfig( config: Config )
//    {
//        val endpoint = endpointId ?: return
//
//        val output = PipedOutputStream()
//        val input = PipedInputStream(output, 64 * 1024)
//
//        val payload = Payload.fromStream(input)
//
//        client.sendPayload(endpoint, payload)
//            .addOnFailureListener {
//                output.close()
//            }
//
//        scope.launch(Dispatchers.IO) {
//            try {
//                json.encodeToStream(config, output)
//            } finally {
//                output.close()
//            }
//        }
//    }
//
//    private val connectionCallback =
//        object : ConnectionLifecycleCallback()
//        {
//            override fun onConnectionInitiated(
//                endpointId: String,
//                connectionInfo: ConnectionInfo
//            ) {
//                client.acceptConnection(
//                    endpointId,
//                    emptyPayloadCallback
//                )
//            }
//
//            override fun onConnectionResult(
//                endpointId: String,
//                result: ConnectionResolution
//            ) {
//                if (result.status.isSuccess) {
//                    this@NearbySender.endpointId = endpointId
//                }
//            }
//
//            override fun onDisconnected(endpointId: String) {
//                this@NearbySender.endpointId = null
//            }
//        }
//
//    private val emptyPayloadCallback =
//        object : PayloadCallback() {
//            override fun onPayloadReceived(
//                endpointId: String,
//                payload: Payload
//            ) {}
//
//            override fun onPayloadTransferUpdate(
//                endpointId: String,
//                update: PayloadTransferUpdate
//            ) {}
//        }
//
//    private fun showQrCode(sessionId: String)
//    {
//        // TODO: generate QR with sessionId
//    }
}