package edu.gtri.gpssample.fragments.manage_archives

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import edu.gtri.gpssample.R
import edu.gtri.gpssample.database.models.Config
import edu.gtri.gpssample.extensions.toLocalizedDateTimeString
import edu.gtri.gpssample.ui.compose.ComposableBusyIndicatorDialog
import edu.gtri.gpssample.ui.compose.ComposableSelectionDialog
import edu.gtri.gpssample.ui.compose.ComposableConfirmationDialog
import java.util.Date

@Composable
fun ManageArchivesScreen(
    archives: List<Config>,
    onRestore: (Config) -> Unit,
    onDelete: (Config) -> Unit
) {
    var selectedConfig by remember {
        mutableStateOf<Config?>(null)
    }

    var showSelectionDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteConfirmation by remember {
        mutableStateOf(false)
    }

    var showBusyIndicatorDialog by remember {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Archives",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .wrapContentHeight(
                    Alignment.CenterVertically
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = 0.dp,
                bottom = 10.dp
            )
        ) {
            items(archives) { archive ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 20.dp,
                            vertical = 10.dp
                        )
                        .clickable {
                            selectedConfig = archive
                            showSelectionDialog = true
                        },
                    shape = RoundedCornerShape(4.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = archive.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(45.dp)
                                .padding(horizontal = 20.dp)
                                .wrapContentHeight(
                                    Alignment.CenterVertically
                                )
                        )

                        Text(
                            text = stringResource(R.string.created ) + " " + Date(archive.creationDate).toLocalizedDateTimeString(),
                            fontSize = 16.sp,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(45.dp)
                                .padding(horizontal = 20.dp)
                                .wrapContentHeight(
                                    Alignment.CenterVertically
                                )
                        )
                    }
                }
            }
        }
    }

    // Restore / Delete selection dialog
    if (showSelectionDialog)
    {
        selectedConfig?.let { config ->

            ComposableSelectionDialog(
                title = config.name,
                message = "What would you like to do with this configuration?",
                items = listOf(
                    "Restore",
                    "Delete"
                ),
                cancelable = true,
                onResult = { result ->

                    showSelectionDialog = false

                    when (result) {

                        "Restore" -> {
                            selectedConfig = null
                            onRestore(config)
                        }

                        "Delete" -> {
                            showDeleteConfirmation = true
                        }
                    }
                }
            )
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmation)
    {
        selectedConfig?.let { config ->

            ComposableConfirmationDialog(
                title = "Delete Configuration",
                message = "Are you sure you want to delete ${config.name}?",
                leftButtonText = "Cancel",
                rightButtonText = "Delete",
                cancelable = true,
                destructive = true,
                onResult = { result ->

                    showDeleteConfirmation = false

                    when (result) {

                        "Delete" -> {
                            selectedConfig = null
                            showBusyIndicatorDialog = true
                            onDelete(config)
                        }

                        "Cancel" -> {
                            selectedConfig = null
                        }
                    }
                }
            )
        }
    }

    if (showBusyIndicatorDialog)
    {
        ComposableBusyIndicatorDialog( "Deleting Configuration...", "", null )
    }
}