package edu.gtri.gpssample.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import edu.gtri.gpssample.R
import androidx.compose.ui.draw.scale

@Composable
fun ComposableEnumAreaHelpDialog(onDismiss: () -> Unit)
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
                    LegendItem(
                        R.drawable.import_blue,
                        stringResource(R.string.import_enumeration_area)
                    )

                    LegendItem(
                        R.drawable.add_location_blue,
                        stringResource(R.string.create_enumeration_area)
                    )

                    LegendItem(
                        R.drawable.add_house_blue,
                        stringResource(R.string.add_household)
                    )

                    LegendItem(
                        R.drawable.cache2,
                        stringResource(R.string.download_map_tiles)
                    )

                    LegendItem(
                        R.drawable.edit_location,
                        stringResource(R.string.edit_enumeration_area)
                    )

                    LegendItem(
                        R.drawable.delete_blue,
                        stringResource(R.string.delete_all_enumeration_areas)
                    )

                    LegendItem(
                        R.drawable.location_bubble_primary,
                        stringResource(R.string.center_on_location)
                    )
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    imageRes: Int,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(40.dp)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}