package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableSelectionDialogHost
{
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        title: String?,
        message: String?,
        items: List<String>,
        cancelable: Boolean = true,
        onResult: (String) -> Unit
    ) {
        dialogContent = DialogContent(
            title = title,
            message = message,
            items = items,
            cancelable = cancelable,
            onResult = onResult
        )
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            dialogContent?.let {
                ComposableSelectionDialog(
                    title = it.title,
                    message = it.message,
                    items = it.items,
                    cancelable = it.cancelable,
                    onResult = { result ->
                        dialogContent = null
                        it.onResult(result)
                    }
                )
            }
        }
    }

    private data class DialogContent(
        val title: String?,
        val message: String?,
        val items: List<String>,
        val cancelable: Boolean,
        val onResult: (String) -> Unit
    )
}