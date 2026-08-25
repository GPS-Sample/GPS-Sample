package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableMapHelpDialogHost
{
    private var visible by mutableStateOf(false)

    fun show() {
        visible = true
    }

    fun dismiss() {
        visible = false
    }

    @Composable
    fun Content() {
        GPSSampleComposeTheme {
            if (visible) {
                ComposableMapHelpDialog(
                    onDismiss = {
                        visible = false
                    }
                )
            }
        }
    }
}