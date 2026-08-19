package edu.gtri.gpssample.ui.compose

import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import androidmads.library.qrgenearator.QRGContents
import androidmads.library.qrgenearator.QRGEncoder

import edu.gtri.gpssample.R
import edu.gtri.gpssample.managers.NearbySessionState

@Composable
fun ComposableNearbySessionStatusDialog(
    title: String,
    state: NearbySessionState,
    onCancel: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            // Dialog is not dismissible by Back or outside click.
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 20.dp,
                        top = 20.dp,
                        end = 20.dp,
                        bottom = 12.dp
                    )
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (state) {
                    is NearbySessionState.Advertising -> {
                        NearbySessionQrCode(
                            sessionId = state.sessionId
                        )
                    }

                    NearbySessionState.Idle -> {
                        // Nothing to display.
                    }

                    else -> {
                        NearbySessionStatus(
                            state = state
                        )
                    }
                }

                if (state != NearbySessionState.Idle) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onCancel
                        ) {
                            Text(
                                text = when (state) {
                                    NearbySessionState.Connecting,
                                    NearbySessionState.Connected ->
                                        stringResource(R.string.cancel)

                                    else ->
                                        stringResource(R.string.done)
                                },
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbySessionStatus(
    state: NearbySessionState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = nearbySessionStatusText(state),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        CircularProgressIndicator(
            modifier = Modifier.size(30.dp),
            strokeWidth = 3.dp
        )
    }
}

@Composable
private fun nearbySessionStatusText(
    state: NearbySessionState
): String {
    return when (state) {
        NearbySessionState.Connecting ->
            "Connecting..."

        NearbySessionState.Connected ->
            "Connected."

        NearbySessionState.SendingConfig ->
            "Sending Config..."

        NearbySessionState.SendingImage ->
            "Sending Image..."

        NearbySessionState.ReceivingConfig ->
            "Receiving Config..."

        NearbySessionState.ReceivingEnumerationAreas ->
            "Receiving EnumerationAreas..."

        NearbySessionState.ReceivingImages ->
            "Receiving Images..."

        NearbySessionState.Done ->
            "Done."

        NearbySessionState.Closed ->
            "Closed"

        is NearbySessionState.Message ->
            state.message

        is NearbySessionState.Error ->
            state.message

        NearbySessionState.Idle ->
            ""

        is NearbySessionState.Advertising ->
            ""
    }
}

@Composable
private fun NearbySessionQrCode(
    sessionId: String
) {
    val bitmap = remember(sessionId) {
        val qrgEncoder = QRGEncoder(
            sessionId,
            null,
            QRGContents.Type.TEXT,
            500
        )

        qrgEncoder.colorBlack = Color.WHITE
        qrgEncoder.colorWhite = Color.BLACK

        qrgEncoder.bitmap
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "QR Code",
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp)
            .aspectRatio(1f)
    )
}

