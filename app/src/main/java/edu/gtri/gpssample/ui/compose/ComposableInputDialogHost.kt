package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import edu.gtri.gpssample.R
import edu.gtri.gpssample.ui.GPSSampleComposeTheme
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

class ComposableInputDialogHost
{
    private var dialogInstance = 0
    private var qrText by mutableStateOf<String?>(null)
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        title: String?,
        text: String,
        description: String?,
        required: Boolean = false,
        keyboardType: KeyboardType = KeyboardType.Text,
        capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
        isPassword: Boolean = false,
        cancelable: Boolean = true,
        leftButtonText: String? = null,
        rightButtonText: String? = null,
        onQrClick: (() -> Unit)? = null,
        onResult: (String) -> Unit
    ) {
        dialogInstance++
        qrText = null

        dialogContent = DialogContent(
            title = title,
            description = description,
            text = text,
            required = required,
            keyboardType = keyboardType,
            capitalization = capitalization,
            isPassword = isPassword,
            cancelable = cancelable,
            leftButtonText = leftButtonText,
            rightButtonText = rightButtonText,
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
                        description = it.description,
                        text = it.text,
                        required = it.required,
                        keyboardType = it.keyboardType,
                        capitalization = it.capitalization,
                        isPassword = it.isPassword,
                        cancelable = it.cancelable,
                        leftButtonText = it.leftButtonText,
                        rightButtonText = it.rightButtonText,
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
        val description: String?,
        val text: String,
        val required: Boolean = true,
        val keyboardType: KeyboardType = KeyboardType.Text,
        val capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
        val isPassword: Boolean = false,
        val cancelable: Boolean = true,
        val leftButtonText: String? = null,
        val rightButtonText: String? = null,
        val onQrClick: (() -> Unit)?,
        val onResult: (String) -> Unit
    )
}