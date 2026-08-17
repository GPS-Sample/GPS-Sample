package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableNotificationDialogHost
{
    private var notification by mutableStateOf<Notification?>(null)

    fun show(
        title: String?,
        message: String?,
        buttonText: String
    ) {
        notification = Notification(
            title = title,
            message = message,
            buttonText = buttonText
        )
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            notification?.let {
                ComposableNotificationDialog(
                    title = it.title,
                    message = it.message,
                    buttonText = it.buttonText,
                    onDismiss = {
                        notification = null
                    }
                )
            }
        }
    }

    private data class Notification(
        val title: String?,
        val message: String?,
        val buttonText: String,
    )
}