package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableConfirmationDialogHost
{
    private var confirmation by mutableStateOf<Confirmation?>(null)

    fun show(
        title: String?,
        message: String?,
        items: List<String>,
        layoutVertically: Boolean = false,
        cancelable: Boolean = true,
        onResult: (String) -> Unit
    ) {
        confirmation = Confirmation(
            title = title,
            message = message,
            items = items,
            layoutVertically = layoutVertically,
            cancelable = cancelable,
            onResult = onResult
        )
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme {
            confirmation?.let {
                ComposableConfirmationDialog(
                    title = it.title,
                    message = it.message,
                    items = it.items,
                    layoutVertically = it.layoutVertically,
                    cancelable = it.cancelable,
                    onResult = { result ->
                        confirmation = null
                        it.onResult(result)
                    }
                )
            }
        }
    }

    private data class Confirmation(
        val title: String?,
        val message: String?,
        val items: List<String>,
        val layoutVertically: Boolean,
        val cancelable: Boolean,
        val onResult: (String) -> Unit
    )
}