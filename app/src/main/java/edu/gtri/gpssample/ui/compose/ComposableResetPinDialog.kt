package edu.gtri.gpssample.ui.compose

import edu.gtri.gpssample.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ComposableResetPinDialog(
    title: String?,
    currentPin: String,
    onResult: (String) -> Unit
) {
    Dialog(
        onDismissRequest = {
            onResult("")
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding( all = 20.dp )
            ) {
                if (!title.isNullOrEmpty())
                {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                var oldPin by remember { mutableStateOf("") }
                var oldPinError by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = oldPin,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    onValueChange = {
                        oldPin = it
                        oldPinError = false
                    },
                    isError = oldPinError,
                    supportingText = {
                        if (oldPinError) {
                            Text("Incorrect current PIN")
                        }
                    },
                    placeholder = { Text("Enter your old PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                Spacer(modifier = Modifier.height(10.dp))

                var newPin1 by remember { mutableStateOf("") }
                var pin1Missing by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = newPin1,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    onValueChange = {
                        newPin1 = it
                        pin1Missing = false },
                    isError = pin1Missing,
                    supportingText = {
                        if (pin1Missing) {
                            Text("Please enter a PIN")
                        }
                    },
                    placeholder = { Text("Enter your new PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                Spacer(modifier = Modifier.height(10.dp))

                var newPin2 by remember { mutableStateOf("") }
                var pinMismatch by remember { mutableStateOf(false) }
                var pin2Missing by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = newPin2,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    onValueChange = {
                        newPin2 = it
                        pin2Missing = false
                        pinMismatch = false
                    },
                    isError = pin2Missing || pinMismatch,
                    supportingText = {
                        if (pin2Missing) {
                            Text("Please enter a PIN")
                        }
                        else if (pinMismatch) {
                            Text("PINs do not match")
                        }
                    },
                    placeholder = { Text("Re-Enter your new PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

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
                            text = stringResource(R.string.cancel),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = {
                            if (oldPin != currentPin)
                            {
                                oldPinError = true
                            }
                            else if (newPin1.isEmpty())
                            {
                                pin1Missing = true
                            }
                            else if (newPin2.isEmpty())
                            {
                                pin2Missing = true
                            }
                            else if (newPin1 != newPin2)
                            {
                                pinMismatch = true
                            }
                            else
                            {
                                onResult( newPin1 )
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.save),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}