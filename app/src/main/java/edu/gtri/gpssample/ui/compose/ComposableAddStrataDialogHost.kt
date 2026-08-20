package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import edu.gtri.gpssample.ui.GPSSampleComposeTheme
import edu.gtri.gpssample.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource

class ComposableAddStrataDialogHost {

    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        strataName: String = "",
        sampleSize: String = "1",
        sampleTypeIndex: Int = 0,
        onDelete: (() -> Unit)? = null,
        onResult: (
            strataName: String,
            sampleSize: String,
            sampleSizeTypeIndex: Int
        ) -> Unit
    ) {
        dialogContent = DialogContent(
            strataName = strataName,
            sampleSize = sampleSize,
            sampleTypeIndex = sampleTypeIndex,
            onDelete = onDelete,
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
                ComposableAddStrataDialog(
                    title = stringResource(R.string.add_strata),
                    strataName = it.strataName,
                    sampleSize = it.sampleSize,
                    sampleTypeIndex = it.sampleTypeIndex,
                    onStrataNameChange = { value -> dialogContent = it.copy(strataName = value) },
                    onSampleSizeChange = { value -> dialogContent = it.copy(sampleSize = value) },
                    onSampleSizeTypeChange = { value -> dialogContent = it.copy(sampleTypeIndex = value) },

                    onDelete = {
                        it.onDelete?.invoke()
                        dialogContent = null
                    },

                    onCancel = {
                        dialogContent = null
                    },

                    onSave = {
                        dialogContent = null

                        it.onResult(
                            it.strataName,
                            it.sampleSize,
                            it.sampleTypeIndex
                        )
                    }
                )
            }
        }
    }

    private data class DialogContent(
        val strataName: String,
        val sampleSize: String,
        val sampleTypeIndex: Int,
        val onDelete: (() -> Unit)?,
        val onResult: (
            strataName: String,
            sampleSize: String,
            sampleSizeTypeIndex: Int
        ) -> Unit
    )
}