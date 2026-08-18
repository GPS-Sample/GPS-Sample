package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableConfirmationDialogHost
{
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        title: String?,
        message: String?,
        leftButtonText: String,
        rightButtonText: String,
        cancelable: Boolean = true,
        destructive: Boolean = false,
        onResult: (String) -> Unit
    ) {
        dialogContent = DialogContent(
            title = title,
            message = message,
            leftButtonText = leftButtonText,
            rightButtonText = rightButtonText,
            cancelable = cancelable,
            destructive = destructive,
            onResult = onResult
        )
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            dialogContent?.let {
                ComposableConfirmationDialog(
                    title = it.title,
                    message = it.message,
                    leftButtonText = it.leftButtonText,
                    rightButtonText =  it.rightButtonText,
                    cancelable = it.cancelable,
                    destructive = it.destructive,
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
        val leftButtonText: String,
        val rightButtonText: String,
        val cancelable: Boolean,
        val destructive: Boolean,
        val onResult: (String) -> Unit
    )
}