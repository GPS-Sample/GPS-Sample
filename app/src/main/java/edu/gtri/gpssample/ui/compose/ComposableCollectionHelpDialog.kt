package edu.gtri.gpssample.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import edu.gtri.gpssample.R

@Composable
fun ComposableCollectionHelpDialog(onDismiss: () -> Unit)
{
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                ComposableDialogTitleBar()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    ComposableDialogHelpItem(
                        R.drawable.cache2,
                        stringResource(R.string.download_map_tiles)
                    )

                    ComposableDialogHelpItem(
                        R.drawable.location_bubble_primary,
                        stringResource(R.string.center_on_location)
                    )

                    ComposableDialogHelpItem(
                        R.drawable.navigate2,
                        stringResource(R.string.show_breadcrumbs)
                    )

                    ComposableDialogHelpItem(
                        R.drawable.export_blue,
                        stringResource(R.string.export_configuration)
                    )
                }
            }
        }
    }
}