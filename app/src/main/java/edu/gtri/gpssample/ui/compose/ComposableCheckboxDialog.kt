package edu.gtri.gpssample.ui.compose

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ComposableCheckboxDialog(
    title: String?,
    items: List<String>,
    cancelButtonText: String,
    continueButtonText: String,
    isChecked: List<Boolean> = emptyList(),
    onCancel: () -> Unit,
    onContinue: (ArrayList<String>) -> Unit
) {
    val checkedStates = remember {
        mutableStateListOf<Boolean>().apply {
            if (isChecked.isEmpty()) {
                repeat(items.size) {
                    add(true)
                }
            } else {
                addAll(isChecked)
            }
        }
    }

    Dialog(
        onDismissRequest = onCancel
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
                    .padding(24.dp)
            ) {

                if (!title.isNullOrEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    itemsIndexed(items) { index, item ->

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    checkedStates[index] = !checkedStates[index]
                                }
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 0.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checkedStates[index],
                                onCheckedChange = { checked ->
                                    checkedStates[index] = checked
                                }
                            )

                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onCancel
                    ) {
                        Text(cancelButtonText)
                    }

                    Button(
                        onClick = {
                            val selections = ArrayList<String>()

                            items.forEachIndexed { index, item ->
                                if (checkedStates[index]) {
                                    selections.add(item)
                                }
                            }

                            onContinue(selections)
                        }
                    ) {
                        Text(continueButtonText)
                    }
                }
            }
        }
    }
}