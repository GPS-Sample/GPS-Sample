package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import edu.gtri.gpssample.managers.NearbySessionState
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableNearbySessionStatusDialogHost {

    private var visible by mutableStateOf(false)
    private var title by mutableStateOf("")
    private var state by mutableStateOf<NearbySessionState?>(null)
    private var onCancel: (() -> Unit)? = null

    fun show(
        title: String,
        onCancel: () -> Unit
    ) {
        this.title = title
        this.onCancel = onCancel
        this.state = null
        this.visible = true
    }

    fun updateState(state: NearbySessionState) {
        this.state = state
    }

    fun dismiss() {
        visible = false
        state = null
        onCancel = null
    }

    @Composable
    fun Content()
    {
        GPSSampleComposeTheme{
            if (visible && state != null)
            {
                ComposableNearbySessionStatusDialog(
                    title = title,
                    state = state!!,
                    onCancel = {
                        onCancel?.invoke()
                        dismiss()
                    }
                )
            }
        }
    }
}