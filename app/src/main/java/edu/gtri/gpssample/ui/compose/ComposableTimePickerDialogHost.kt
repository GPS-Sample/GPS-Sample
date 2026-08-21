package edu.gtri.gpssample.ui.compose
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import edu.gtri.gpssample.ui.GPSSampleComposeTheme
import java.util.Date
import edu.gtri.gpssample.R
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource

class ComposableTimePickerDialogHost
{
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        date: Date,
        onResult: (Date?) -> Unit
    ) {
        dialogContent = DialogContent(
            date = date,
            onResult = onResult
        )
    }

    fun cancel() {
        dialogContent = null
    }

    @Composable
    fun Content() {
        GPSSampleComposeTheme {
            dialogContent?.let {
                ComposableTimePickerDialog(
                    title = stringResource(R.string.select_a_time),
                    date = it.date,
                    onCancel = {
                        dialogContent = null
                        it.onResult(null)
                    },
                    onSelect = { date ->
                        dialogContent = null
                        it.onResult(date)
                    }
                )
            }
        }
    }

    private data class DialogContent(
        val date: Date,
        val onResult: (Date?) -> Unit
    )
}