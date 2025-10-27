package com.syndicate.rssreader.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.syndicate.rssreader.data.models.Feed
import com.syndicate.rssreader.data.models.Group
import com.syndicate.rssreader.ui.viewmodel.GroupManagementViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: GroupManagementViewModel = hiltViewModel()
    val groups by viewModel.groups.collectAsState()
    val feeds by viewModel.feeds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showCreateDialog by viewModel.showCreateGroupDialog.collectAsState()
    val showEditDialog by viewModel.showEditGroupDialog.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val selectedFeedIds by viewModel.selectedFeedIds.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf<Group?>(null) }
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Groups") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateGroupDialog() },
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { paddingValues ->
        
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (groups.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "No groups yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tap the + button to create your first group",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(groups, key = { it.id }) { group ->
                        GroupItem(
                            group = group,
                            onEditClick = { viewModel.showEditGroupDialog(group) },
                            onDeleteClick = { showDeleteDialog = group },
                            onToggleDefault = { viewModel.setDefaultGroup(group.id) },
                            onToggleNotifications = { enabled ->
                                viewModel.updateGroup(
                                    group.id,
                                    group.name,
                                    group.isDefault,
                                    enabled
                                )
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Create Group Dialog
    if (showCreateDialog) {
        CreateGroupDialog(
            feeds = feeds,
            selectedFeedIds = selectedFeedIds,
            onFeedToggle = viewModel::toggleFeedSelection,
            onConfirm = { name, isDefault, notificationsEnabled ->
                viewModel.createGroup(name, isDefault, notificationsEnabled)
            },
            onDismiss = { viewModel.hideCreateGroupDialog() }
        )
    }
    
    // Edit Group Dialog
    if (showEditDialog && selectedGroup != null) {
        EditGroupDialog(
            group = selectedGroup!!,
            feeds = feeds,
            selectedFeedIds = selectedFeedIds,
            onFeedToggle = viewModel::toggleFeedSelection,
            onConfirm = { name, isDefault, notificationsEnabled ->
                viewModel.updateGroup(
                    selectedGroup!!.id,
                    name,
                    isDefault,
                    notificationsEnabled
                )
            },
            onDismiss = { viewModel.hideEditGroupDialog() }
        )
    }
    
    // Delete Confirmation Dialog
    showDeleteDialog?.let { group ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Group") },
            text = {
                Text("Are you sure you want to delete \"${group.name}\"? Feeds will not be unsubscribed.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteGroup(group)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupItem(
    group: Group,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleDefault: () -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteClick()
                    false // Don't dismiss - we'll handle it via dialog
                }
                else -> false
            }
        }
    )
    
    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onEditClick() },
            colors = CardDefaults.cardColors(
                containerColor = if (group.isDefault) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (group.isDefault) 4.dp else 1.dp
            )
        ) {
            ListItem(
                headlineContent = {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (group.isDefault) FontWeight.Bold else FontWeight.Normal,
                        color = if (group.isDefault) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                },
                supportingContent = {
                    Text(
                        text = "${group.feedCount} feeds${if (group.isDefault) " • Default" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (group.isDefault) {
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = if (group.isDefault) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                        modifier = Modifier.size(24.dp)
                    )
                },
                trailingContent = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Notifications toggle
                        IconButton(
                            onClick = { onToggleNotifications(!group.notificationsEnabled) }
                        ) {
                            Icon(
                                imageVector = if (group.notificationsEnabled) {
                                    Icons.Default.Notifications
                                } else {
                                    Icons.Default.NotificationsOff
                                },
                                contentDescription = if (group.notificationsEnabled) {
                                    "Disable notifications"
                                } else {
                                    "Enable notifications"
                                },
                                tint = if (group.notificationsEnabled) {
                                    MaterialTheme.colorScheme.tertiary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Default toggle
                        IconButton(onClick = onToggleDefault) {
                            Icon(
                                imageVector = if (group.isDefault) {
                                    Icons.Default.Star
                                } else {
                                    Icons.Default.StarBorder
                                },
                                contentDescription = if (group.isDefault) {
                                    "Remove as default"
                                } else {
                                    "Set as default"
                                },
                                tint = if (group.isDefault) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        // Edit button
                        IconButton(onClick = onEditClick) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit group",
                                tint = if (group.isDefault) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun CreateGroupDialog(
    feeds: List<Feed>,
    selectedFeedIds: Set<Long>,
    onFeedToggle: (Long) -> Unit,
    onConfirm: (String, Boolean, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
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
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = feed.title,
                                        style = MaterialTheme.typography.bodyMedium
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
                Text("Create")
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
private fun EditGroupDialog(
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
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = feed.title,
                                        style = MaterialTheme.typography.bodyMedium
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