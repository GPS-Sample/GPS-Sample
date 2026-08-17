package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableInputDialogHost
{
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        title: String?,
        text: String,
        leftButton: String,
        rightButton: String,
        required: Boolean = false,
        inputTypeNumber: Boolean = false,
        allowQr: Boolean = false,
        cancelable: Boolean = true,
        onResult: (String) -> Unit
    ) {
        dialogContent = DialogContent(
            title = title,
            text = text,
            leftButton = leftButton,
            rightButton = rightButton,
            required = required,
            inputTypeNumber = inputTypeNumber,
            allowQr = allowQr,
            cancelable = cancelable,
            onResult = onResult
        )
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            dialogContent?.let {
                ComposableInputDialog(
                    title = it.title,
                    text = it.text,
                    leftButton = it.leftButton,
                    rightButton = it.rightButton,
                    required = it.required,
                    inputTypeNumber = it.inputTypeNumber,
                    allowQr = it.allowQr,
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
        val text: String,
        val leftButton: String,
        val rightButton: String,
        val required: Boolean = true,
        val inputTypeNumber: Boolean = false,
        val allowQr: Boolean = false,
        val cancelable: Boolean = true,
        val onResult: (String) -> Unit
    )
}