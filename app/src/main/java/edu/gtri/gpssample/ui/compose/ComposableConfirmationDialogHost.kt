package edu.gtri.gpssample.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.dialogs.ButtonPress
import edu.gtri.gpssample.dialogs.ComposableConfirmationDialog

class ComposableConfirmationDialogHost {

    private var confirmation by mutableStateOf<Confirmation?>(null)

    fun show(
        title: String?,
        message: String?,
        leftButtonText: String,
        rightButtonText: String,
        layoutVertically: Boolean = false,
        cancelable: Boolean = true,
        onResult: (ButtonPress) -> Unit
    ) {
        confirmation = Confirmation(
            title = title,
            message = message,
            leftButtonText = leftButtonText,
            rightButtonText = rightButtonText,
            layoutVertically = layoutVertically,
            cancelable = cancelable,
            onResult = onResult
        )
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            confirmation?.let { dialog ->
                ComposableConfirmationDialog(
                    title = dialog.title,
                    message = dialog.message,
                    leftButtonText = dialog.leftButtonText,
                    rightButtonText = dialog.rightButtonText,
                    layoutVertically = dialog.layoutVertically,
                    cancelable = dialog.cancelable,
                    onResult = { result ->
                        confirmation = null
                        dialog.onResult(result)
                    }
                )
            }
        }
    }

    private data class Confirmation(
        val title: String?,
        val message: String?,
        val leftButtonText: String,
        val rightButtonText: String,
        val layoutVertically: Boolean,
        val cancelable: Boolean,
        val onResult: (ButtonPress) -> Unit
    )
}