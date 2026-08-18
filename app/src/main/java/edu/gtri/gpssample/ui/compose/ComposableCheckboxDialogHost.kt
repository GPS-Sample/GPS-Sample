package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableCheckboxDialogHost
{
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        title: String?,
        items: List<String>,
        cancelButtonText: String,
        continueButtonText: String,
        isChecked: List<Boolean> = emptyList(),
        onContinue: (ArrayList<String>) -> Unit
    ) {
        dialogContent = DialogContent(
            title = title,
            items = items,
            cancelButtonText = cancelButtonText,
            continueButtonText = continueButtonText,
            isChecked = isChecked,
            onContinue = onContinue
        )
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            dialogContent?.let {
                ComposableCheckboxDialog(
                    title = it.title,
                    items = it.items,
                    cancelButtonText = it.cancelButtonText,
                    continueButtonText = it.continueButtonText,
                    isChecked = it.isChecked,
                    onCancel = {
                        dialogContent = null
                    },
                    onContinue = { selections ->
                        dialogContent = null
                        it.onContinue( selections )
                    }
                )
            }
        }
    }

    private data class DialogContent(
        val title: String?,
        val items: List<String>,
        val cancelButtonText: String,
        val continueButtonText: String,
        val isChecked: List<Boolean> = emptyList(),
        val onContinue: (ArrayList<String>) -> Unit
    )
}