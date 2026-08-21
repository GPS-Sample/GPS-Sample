package edu.gtri.gpssample.ui.compose

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
fun ComposableMapLegendDialog(
    onDismiss: () -> Unit
) {
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
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                ComposableDialogTitleBar()

                Spacer(modifier = Modifier.height( 20.dp ))

                LegendItem(
                    R.drawable.home_black,
                    stringResource(R.string.not_visited)
                )

                LegendItem(
                    R.drawable.home_red,
                    stringResource(R.string.incomplete_enumeration)
                )

                LegendItem(
                    R.drawable.home_green,
                    stringResource(R.string.completed_enumeration)
                )

                LegendItem(
                    R.drawable.home_light_blue,
                    stringResource(R.string.sampled)
                )

                LegendItem(
                    R.drawable.home_orange,
                    stringResource(R.string.incomplete_collection)
                )

                LegendItem(
                    R.drawable.home_purple,
                    stringResource(R.string.completed_collection)
                )

                Spacer(modifier = Modifier.height( 20.dp ))
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
            .padding(vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier
                .padding(start = 10.dp)
                .size(50.dp)
                .scale(1.5f)
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 20.dp)
        )
    }
}