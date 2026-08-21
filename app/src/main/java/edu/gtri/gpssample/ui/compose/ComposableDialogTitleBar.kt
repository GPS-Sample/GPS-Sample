package edu.gtri.gpssample.ui.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ComposableDialogTitleBar(
    title: String? = null,
    actionIcon: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.secondary)
    ) {
        title?.let {
            val paddingEnd = if (actionIcon == null) 20.dp else 60.dp

            Text(
                text = it,
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding( start = 20.dp, end = paddingEnd )
                    .align(Alignment.Center)
            )
        }

        actionIcon?.let {
            Box(
                modifier = Modifier
                    .padding(end = 5.dp)
                    .align(Alignment.CenterEnd)
            ) {
                it()
            }
        }
    }
}