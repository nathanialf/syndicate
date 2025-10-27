package com.syndicate.rssreader.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.syndicate.rssreader.data.models.Feed
import com.syndicate.rssreader.data.models.Group

@Composable
fun EditGroupDialog(
    group: Group,
    feeds: List<Feed>,
    selectedFeedIds: Set<Long>,
    onFeedToggle: (Long) -> Unit,
    onConfirm: (String, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf(group.name) }
    var isDefault by remember { mutableStateOf(group.isDefault) }
    var notificationsEnabled by remember { mutableStateOf(group.notificationsEnabled) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Group") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = groupName,
                    onValueChange = { groupName = it },
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Default setting with star icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { isDefault = !isDefault }
                    ) {
                        IconButton(onClick = { isDefault = !isDefault }) {
                            Icon(
                                imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (isDefault) "Remove as default" else "Set as default",
                                tint = if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = "Default",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    
                    // Notifications setting with bell icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { notificationsEnabled = !notificationsEnabled }
                    ) {
                        IconButton(onClick = { notificationsEnabled = !notificationsEnabled }) {
                            Icon(
                                imageVector = if (notificationsEnabled) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                contentDescription = if (notificationsEnabled) "Disable notifications" else "Enable notifications",
                                tint = if (notificationsEnabled) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                
                if (feeds.isNotEmpty()) {
                    Text(
                        text = "Select Feeds:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    
                    Column(
                        modifier = Modifier.height(200.dp)
                    ) {
                        LazyColumn {
                            items(feeds, key = { it.id }) { feed ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFeedToggle(feed.id) }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = selectedFeedIds.contains(feed.id),
                                        onCheckedChange = { onFeedToggle(feed.id) }
                                    )
                                    Text(
                                        text = feed.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(groupName, isDefault, notificationsEnabled) },
                enabled = groupName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}