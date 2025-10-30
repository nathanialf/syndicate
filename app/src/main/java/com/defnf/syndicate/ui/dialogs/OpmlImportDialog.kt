package com.defnf.syndicate.ui.dialogs

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.data.models.OpmlImportResult

@Composable
fun OpmlFilePickerDialog(
    onFileSelected: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onFileSelected(it) }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import OPML File") },
        text = {
            Column {
                Text(
                    text = "Select an OPML file to import RSS feeds and groups.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "OPML files are commonly exported from other RSS readers and contain your feed subscriptions and folder organization.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { launcher.launch("*/*") }
            ) {
                Text("Select File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun OpmlPreviewDialog(
    importResult: OpmlImportResult,
    onImport: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var skipDuplicates by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("OPML Import Preview") },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Summary card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Import Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Total feeds: ${importResult.totalFeeds}")
                            Text("New groups: ${importResult.groupsToCreate.size}")
                            if (importResult.duplicateFeeds.isNotEmpty()) {
                                Text(
                                    text = "Duplicate feeds: ${importResult.duplicateFeeds.size}",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
                
                if (importResult.duplicateFeeds.isNotEmpty()) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = skipDuplicates,
                                onCheckedChange = { skipDuplicates = it }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Skip duplicate feeds")
                        }
                    }
                    
                    item {
                        Text(
                            text = "Duplicate Feeds:",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    items(importResult.duplicateFeeds.take(10)) { url ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = url,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    if (importResult.duplicateFeeds.size > 10) {
                        item {
                            Text(
                                text = "... and ${importResult.duplicateFeeds.size - 10} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                if (importResult.groupsToCreate.isNotEmpty()) {
                    item {
                        Text(
                            text = "Groups to Create:",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    
                    items(importResult.groupsToCreate) { groupName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(groupName)
                        }
                    }
                }
                
                item {
                    Text(
                        text = "Feeds to Import:",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                items(importResult.feedsToImport.take(10)) { feed ->
                    Column(
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Text(
                            text = feed.title,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = feed.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        feed.groupName?.let { groupName ->
                            Text(
                                text = "Group: $groupName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                if (importResult.feedsToImport.size > 10) {
                    item {
                        Text(
                            text = "... and ${importResult.feedsToImport.size - 10} more feeds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(skipDuplicates) }
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun OpmlImportProgressDialog() {
    AlertDialog(
        onDismissRequest = { /* Prevent dismissal during import */ },
        title = { Text("Importing Feeds") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please wait while we import your feeds...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = { /* No confirm button during progress */ }
    )
}

@Composable
fun OpmlImportResultDialog(
    success: Boolean,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (success) Icons.Default.CheckCircle else Icons.Default.Error,
                    contentDescription = null,
                    tint = if (success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (success) "Import Successful" else "Import Failed")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}