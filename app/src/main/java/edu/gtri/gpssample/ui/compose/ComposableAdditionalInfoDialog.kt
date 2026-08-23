package edu.gtri.gpssample.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import edu.gtri.gpssample.R

@Composable
fun ComposableAdditionalInfoDialog(
    complete: Boolean,
    incompleteReason: String?,
    notes: String,
    onCancel: () -> Unit,
    onSave: (
        complete: Boolean,
        incompleteReason: String?,
        notes: String
    ) -> Unit
) {
    var isComplete by remember {
        mutableStateOf(complete)
    }

    var selectedReason by remember {
        mutableStateOf(incompleteReason)
    }

    var notesText by remember {
        mutableStateOf(notes)
    }

    Dialog(
        onDismissRequest = onCancel
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                // Title bar
                ComposableDialogTitleBar(
                    title = stringResource(R.string.additional_info)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {

                    // Complete / Incomplete
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isComplete = true
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isComplete,
                            onClick = {
                                isComplete = true
                            }
                        )

                        Text(
                            text = stringResource(R.string.complete)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isComplete = false
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = !isComplete,
                            onClick = {
                                isComplete = false
                            }
                        )

                        Text(
                            text = stringResource(R.string.incomplete)
                        )
                    }

                    // Reason for incomplete
                    if (!isComplete) {

                        Text(
                            text = stringResource(R.string.reason_incomplete_label),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(
                                top = 8.dp,
                                bottom = 4.dp
                            )
                        )

                        val reasons = listOf(
                            R.string.nobody_home,
                            R.string.does_not_exist,
                            R.string.other
                        )

                        reasons.forEach { reasonRes ->

                            val reason = stringResource(reasonRes)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedReason = reason
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedReason == reason,
                                    onClick = {
                                        selectedReason = reason
                                    }
                                )

                                Text(
                                    text = reason
                                )
                            }
                        }
                    }

                    // Notes
                    Text(
                        text = stringResource(R.string.notes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(
                            top = 12.dp,
                            bottom = 4.dp
                        )
                    )

                    OutlinedTextField(
                        value = notesText,
                        onValueChange = {
                            notesText = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 20.dp,
                            top = 20.dp,
                            end = 20.dp,
                            bottom = 20.dp
                        ),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onCancel
                    ) {
                        Text(
                            text = stringResource(R.string.cancel),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    Button(
                        onClick = {
                            onSave(
                                isComplete,
                                if (!isComplete) {
                                    selectedReason
                                } else {
                                    null
                                },
                                notesText
                            )
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