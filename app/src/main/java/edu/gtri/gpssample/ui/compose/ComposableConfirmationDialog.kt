package edu.gtri.gpssample.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ComposableConfirmationDialog(
    title: String?,
    message: String?,
    leftButtonText: String,
    rightButtonText: String,
    cancelable: Boolean = true,
    destructive: Boolean = false,
    onResult: (String) -> Unit
) {
    Dialog(
        onDismissRequest = {
            if (cancelable) {
                onResult("")
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = cancelable,
            dismissOnClickOutside = cancelable
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                title?.let {
                    ComposableDialogTitleBar(title = it)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding( all = 20.dp )
                ) {
                    if (!message.isNullOrEmpty())
                    {
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.
                        fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                onResult(leftButtonText)
                            }
                        ) {
                            Text(
                                text = leftButtonText.uppercase(),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        TextButton(
                            onClick = {
                                onResult(rightButtonText)
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (destructive) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                }
                            )
                        ) {
                            Text(
                                text = rightButtonText.uppercase(),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}