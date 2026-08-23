package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import edu.gtri.gpssample.ui.GPSSampleComposeTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

class ComposableAdditionalInfoDialogHost {

    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        complete: Boolean,
        incompleteReason: String?,
        notes: String,
        onResult: (
            complete: Boolean,
            incompleteReason: String?,
            notes: String
        ) -> Unit
    ) {
        dialogContent = DialogContent(
            complete = complete,
            incompleteReason = incompleteReason,
            notes = notes,
            onResult = onResult
        )
    }

    fun cancel() {
        dialogContent = null
    }

    @Composable
    fun Content() {
        GPSSampleComposeTheme {
            dialogContent?.let { content ->

                ComposableAdditionalInfoDialog(
                    complete = content.complete,
                    incompleteReason = content.incompleteReason,
                    notes = content.notes,

                    onCancel = {
                        dialogContent = null
                    },

                    onSave = { complete, incompleteReason, notes ->

                        dialogContent = null

                        content.onResult(
                            complete,
                            incompleteReason,
                            notes
                        )
                    }
                )
            }
        }
    }

    private data class DialogContent(
        val complete: Boolean,
        val incompleteReason: String?,
        val notes: String,
        val onResult: (
            complete: Boolean,
            incompleteReason: String?,
            notes: String
        ) -> Unit
    )
}