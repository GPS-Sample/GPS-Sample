package edu.gtri.gpssample.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import edu.gtri.gpssample.database.models.Strata
import edu.gtri.gpssample.ui.GPSSampleComposeTheme

class ComposableDropdownDialogHost
{
    private var dialogContent by mutableStateOf<DialogContent?>(null)

    fun show(
        title: String?,
        items: ArrayList<String>,
        completion: (String) -> Unit
    ) {
        dialogContent = DialogContent(
            title = title,
            items = items,
            onResult = { index ->
                if (index == null) {
                    completion("")
                } else {
                    completion(items[index])
                }
            }
        )
    }

    fun showStrata(
        title: String?,
        strataList: ArrayList<Strata>,
        completion: (Strata?) -> Unit
    ) {
        val items = ArrayList<String>()

        for (strata in strataList) {
            items.add(strata.name)
        }

        dialogContent = DialogContent(
            title = title,
            items = items,
            onResult = { index ->
                if (index == null) {
                    completion(null)
                } else {
                    completion(strataList[index])
                }
            }
        )
    }

    @Composable
    fun Content() {
        GPSSampleComposeTheme {
            dialogContent?.let {
                ComposableDropdownDialog(
                    title = it.title,
                    items = it.items,
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
        val items: ArrayList<String>,
        val onResult: (Int?) -> Unit
    )
}