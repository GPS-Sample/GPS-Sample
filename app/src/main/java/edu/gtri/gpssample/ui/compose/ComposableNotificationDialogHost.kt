package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableNotificationDialogHost
{
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        title: String?,
        message: String?
    ) {
        dialogContent = DialogContent(
            title = title,
            message = message
        )
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            dialogContent?.let {
                ComposableNotificationDialog(
                    title = it.title,
                    message = it.message,
                    onDismiss = {
                        dialogContent = null
                    }
                )
            }
        }
    }

    private data class DialogContent(
        val title: String?,
        val message: String?
    )
}