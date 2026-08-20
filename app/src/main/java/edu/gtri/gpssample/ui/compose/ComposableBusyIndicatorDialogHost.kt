package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableBusyIndicatorDialogHost
{
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        title: String,
        message: String?,
        onCancel: (() -> Unit)? = null
    ) {
        dialogContent = DialogContent(
            title = title,
            message = message,
            onCancel = onCancel
        )
    }

    fun updateMessage(message: String) {
        dialogContent = dialogContent?.copy(message = message)
    }

    fun cancel()
    {
        dialogContent = null
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            dialogContent?.let {
                ComposableBusyIndicatorDialog(
                    title = it.title,
                    message = it.message,
                    onCancel = {
                        it.onCancel?.invoke()
                        dialogContent = null
                    }
                )
            }
        }
    }

    private data class DialogContent(
        val title: String,
        val message: String?,
        val onCancel: (() -> Unit)?
    )
}