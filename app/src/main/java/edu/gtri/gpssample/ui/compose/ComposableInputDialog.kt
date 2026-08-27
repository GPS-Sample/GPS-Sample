package edu.gtri.gpssample.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import edu.gtri.gpssample.R

@Composable
fun ComposableInputDialog(
    title: String?,
    description: String?,
    text: String,
    required: Boolean = true,
    inputTypeNumber: Boolean = false,
    isPassword: Boolean = false,
    cancelable: Boolean = true,
    leftButtonText: String? = null,
    rightButtonText: String? = null,
    qrText: String? = null,
    onQrClick: (() -> Unit)? = null,
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
                if (title == null && onQrClick == null)
                {
                    ComposableDialogTitleBar()
                }
                else if (title != null && onQrClick == null) {
                    ComposableDialogTitleBar(title)
                }
                else if (onQrClick != null)
                {
                    ComposableDialogTitleBar(
                        title = title,
                        actionIcon = {
                            IconButton(onClick = onQrClick) {
                                Icon(
                                    painter = painterResource(R.drawable.qrcode),
                                    contentDescription = "Scan QR code",
                                    tint = Color.White
                                )
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding( all = 20.dp )
                ) {
                    description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                        )
                    }

                    var textValue by remember { mutableStateOf(text) }
                    val keyboardType = if (inputTypeNumber) KeyboardType.Decimal else KeyboardType.Text

                    LaunchedEffect(qrText) {
                        if (qrText != null) {
                            textValue = qrText
                        }
                    }

                    OutlinedTextField(
                        value = textValue,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        textStyle = MaterialTheme.typography.bodyLarge,
                        visualTransformation = if (isPassword) { PasswordVisualTransformation() } else { VisualTransformation.None },
                        onValueChange = {
                            textValue = it
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.
                        fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                onResult("")
                            }
                        ) {
                            Text(
                                text = if (leftButtonText.isNullOrEmpty()) stringResource(R.string.cancel) else leftButtonText,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                if (!required || (textValue.length > 0))
                                {
                                    onResult(textValue)
                                }
                            }
                        ) {
                            Text(
                                text = if (rightButtonText.isNullOrEmpty()) stringResource(R.string.save) else rightButtonText,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}