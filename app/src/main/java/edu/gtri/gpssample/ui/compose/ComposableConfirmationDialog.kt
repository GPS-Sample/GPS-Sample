package edu.gtri.gpssample.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

enum class ButtonPress {
    Left,
    Right,
    None
}

@Composable
fun ComposableConfirmationDialog(
    title: String?,
    message: String?,
    leftButtonText: String,
    rightButtonText: String,
    layoutVertically: Boolean = false,
    cancelable: Boolean = true,
    onResult: (ButtonPress) -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (cancelable) {
                onResult(ButtonPress.None)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = cancelable,
            dismissOnClickOutside = cancelable
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                // Title
                if (!title.isNullOrEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(
                            start = 20.dp,
                            top = 20.dp,
                            end = 20.dp
                        )
                    )
                }

                // Message
                if (!message.isNullOrEmpty()) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                if (layoutVertically) {

                    // Vertical buttons
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 20.dp
                            )
                    ) {
                        Button(
                            onClick = {
                                onResult(ButtonPress.Left)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Text(leftButtonText)
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                onResult(ButtonPress.Right)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        ) {
                            Text(rightButtonText)
                        }
                    }

                } else {

                    // Horizontal buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                bottom = 20.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Button(
                            onClick = {
                                onResult(ButtonPress.Left)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                        ) {
                            Text(leftButtonText)
                        }

                        Button(
                            onClick = {
                                onResult(ButtonPress.Right)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp)
                        ) {
                            Text(rightButtonText)
                        }
                    }
                }
            }
        }
    }
}