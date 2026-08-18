package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableResetPinDialogHost
{
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        title: String?,
        currentPin: String,
        onResult: (String) -> Unit
    ) {
        dialogContent = DialogContent(
            title = title,
            currentPin = currentPin,
            onResult = onResult
        )
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            dialogContent?.let {
                ComposableResetPinDialog(
                    title = it.title,
                    currentPin = it.currentPin,
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
        val currentPin: String,
        val onResult: (String) -> Unit
    )
}