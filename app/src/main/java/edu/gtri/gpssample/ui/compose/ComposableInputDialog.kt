package edu.gtri.gpssample.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun ComposableInputDialog(
    title: String?,
    text: String,
    leftButton: String,
    rightButton: String,
    required: Boolean = true,
    inputTypeNumber: Boolean = false,
    allowQr: Boolean = false,
    cancelable: Boolean = true,
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
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                if (!title.isNullOrEmpty())
                {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 20.dp
                        )
                    )
                }

                var textValue by remember { mutableStateOf(text) }

                val keyboardType = if (inputTypeNumber) KeyboardType.Decimal else KeyboardType.Text

                OutlinedTextField(
                    value = textValue,
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = 20.dp
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    onValueChange = { textValue = it }
                )

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
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        onClick = {
                            onResult("")
                        }
                    ) {
                        Text(leftButton)
                    }

                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        onClick = {
                            if (!required || (textValue.length > 0))
                            {
                                onResult(textValue)
                            }
                        }
                    ) {
                        Text(rightButton)
                    }
                }
            }
        }
    }
}