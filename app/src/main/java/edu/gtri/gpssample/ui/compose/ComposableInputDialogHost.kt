package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableInputDialogHost
{
    private var dialogInstance = 0
    private var qrText by mutableStateOf<String?>(null)
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        title: String?,
        text: String,
        required: Boolean = false,
        inputTypeNumber: Boolean = false,
        isPassword: Boolean = false,
        cancelable: Boolean = true,
        onQrClick: (() -> Unit)? = null,
        onResult: (String) -> Unit
    ) {
        dialogInstance++
        qrText = null

        dialogContent = DialogContent(
            title = title,
            text = text,
            required = required,
            inputTypeNumber = inputTypeNumber,
            isPassword = isPassword,
            cancelable = cancelable,
            onQrClick = onQrClick,
            onResult = onResult
        )
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            dialogContent?.let {
                key(dialogInstance)
                {
                    ComposableInputDialog(
                        title = it.title,
                        text = it.text,
                        required = it.required,
                        inputTypeNumber = it.inputTypeNumber,
                        isPassword = it.isPassword,
                        cancelable = it.cancelable,
                        qrText = qrText,
                        onQrClick = it.onQrClick,
                        onResult = { result ->
                            dialogContent = null
                            it.onResult(result)
                        }
                    )
                }
            }
        }
    }

    fun updateQrText(text: String) {
        qrText = text
    }

    private data class DialogContent(
        val title: String?,
        val text: String,
        val required: Boolean = true,
        val inputTypeNumber: Boolean = false,
        val isPassword: Boolean = false,
        val cancelable: Boolean = true,
        val onQrClick: (() -> Unit)?,
        val onResult: (String) -> Unit
    )
}