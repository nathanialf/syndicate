package com.defnf.syndicate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import com.defnf.syndicate.ui.dialogs.AddFeedDialog
import com.defnf.syndicate.ui.dialogs.AddGroupDialog
import com.defnf.syndicate.ui.dialogs.DeleteFeedDialog
import com.defnf.syndicate.ui.dialogs.DeleteGroupDialog
import com.defnf.syndicate.ui.dialogs.EditGroupDialog
import com.defnf.syndicate.ui.dialogs.FeedGroupDialog
import com.defnf.syndicate.ui.dialogs.OpmlFilePickerDialog
import com.defnf.syndicate.ui.dialogs.OpmlPreviewDialog
import com.defnf.syndicate.ui.dialogs.OpmlImportProgressDialog
import com.defnf.syndicate.ui.dialogs.OpmlImportResultDialog
import com.defnf.syndicate.ui.components.ExpandableFabMenu
import com.defnf.syndicate.ui.components.FaviconIcon
import com.defnf.syndicate.ui.components.FeedListItem
import com.defnf.syndicate.ui.components.GroupItem
import com.defnf.syndicate.ui.components.SwipeableGroupItem
import com.defnf.syndicate.ui.components.SwipeableFeedItem
import com.defnf.syndicate.ui.components.SwipeBackground
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.CreateNewFolder
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.common.LayoutUtils
import com.defnf.syndicate.ui.theme.CormorantGaramond
import com.defnf.syndicate.ui.viewmodel.FeedListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedListScreen(
    onFeedClick: (Long) -> Unit,
    isSidebarMode: Boolean = false,
    selectedFeedId: Long? = null,
    selectedGroupId: Long? = null,
    onAllFeedsClick: () -> Unit = {},
    onDeleteFeed: (Long) -> Unit = {},
    onGroupClick: (Long) -> Unit = {},
    onDeleteGroup: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FeedListViewModel = hiltViewModel(),
    opmlImportViewModel: com.defnf.syndicate.ui.viewmodel.OpmlImportViewModel = hiltViewModel()
) {
    val feeds by viewModel.feeds.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    val showAddFeedDialog by viewModel.showAddFeedDialog.collectAsState()
    val showAddGroupDialog by viewModel.showAddGroupDialog.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var fabExpanded by remember { mutableStateOf(false) }
    var feedUrlInput by remember { mutableStateOf("") }
    var groupNameInput by remember { mutableStateOf("") }
    
    val showFeedGroupDialog by viewModel.showFeedGroupDialog.collectAsState()
    val selectedFeedForGrouping by viewModel.selectedFeedForGrouping.collectAsState()
    val feedGroupAssignments by viewModel.feedGroupAssignments.collectAsState()
    val showDeleteFeedDialog by viewModel.showDeleteFeedDialog.collectAsState()
    val feedToDelete by viewModel.feedToDelete.collectAsState()
    val resetSwipeState by viewModel.resetSwipeState.collectAsState()
    val showDeleteGroupDialog by viewModel.showDeleteGroupDialog.collectAsState()
    val groupToDelete by viewModel.groupToDelete.collectAsState()
    val showEditGroupDialog by viewModel.showEditGroupDialog.collectAsState()
    val groupToEdit by viewModel.groupToEdit.collectAsState()
    val groupEditFeedAssignments by viewModel.groupEditFeedAssignments.collectAsState()
    
    // OPML Import state
    val importState by opmlImportViewModel.importState.collectAsState()
    val showFileDialog by opmlImportViewModel.showFileDialog.collectAsState()
    val showPreviewDialog by opmlImportViewModel.showPreviewDialog.collectAsState()
    val importResult by opmlImportViewModel.importResult.collectAsState()
    val importErrorMessage by opmlImportViewModel.errorMessage.collectAsState()
    
    val context = LocalContext.current
    

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    
    LaunchedEffect(successMessage) {
        successMessage?.let {
            Log.d("FeedListScreen", "Showing success snackbar: $it")
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccess()
        }
    }

    // Unified layout for both modes - no TopAppBar
    Box(modifier = modifier.fillMaxSize()) {
        FeedListContent(
            feeds = feeds,
            groups = groups,
            isLoading = isLoading,
            onFeedClick = onFeedClick,
            onDeleteFeed = { feedId ->
                viewModel.showDeleteFeedDialog(feedId)
            },
            onNotificationToggle = { feedId -> viewModel.toggleFeedNotifications(feedId) },
            onGroupClick = onGroupClick,
            onDeleteGroup = { groupId ->
                viewModel.showDeleteGroupDialog(groupId)
            },
            onEditGroup = { groupId ->
                viewModel.showEditGroupDialog(groupId)
            },
            resetSwipeState = resetSwipeState,
            listState = listState,
            paddingValues = if (isSidebarMode) {
                androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 0.dp,
                    bottom = 0.dp
                )
            } else {
                androidx.compose.foundation.layout.PaddingValues(16.dp)
            },
            selectedFeedId = selectedFeedId,
            selectedGroupId = selectedGroupId,
            onAllFeedsClick = onAllFeedsClick,
            isSidebarMode = isSidebarMode
        )
        
        // FAB
        ExpandableFabMenu(
            expanded = fabExpanded,
            onToggle = { fabExpanded = !fabExpanded },
            onAddFeed = { 
                fabExpanded = false
                viewModel.showAddFeedDialog() 
            },
            onImportFeed = {
                fabExpanded = false
                opmlImportViewModel.showFileDialog()
            },
            onAddGroup = {
                fabExpanded = false
                viewModel.showAddGroupDialog()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (isSidebarMode) {
                        80.dp // Sidebar mode: higher up for better positioning
                    } else {
                        // Single pane mode: account for bottom navigation + extra height
                        WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 120.dp
                    }
                )
        )
        
        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = if (isSidebarMode) {
                        16.dp // Sidebar mode: just standard padding
                    } else {
                        // Single pane mode: account for bottom navigation
                        WindowInsets.systemBars.asPaddingValues().calculateBottomPadding() + 72.dp
                    }
                )
        ) { snackbarData ->
            androidx.compose.material3.Snackbar(
                snackbarData = snackbarData,
                containerColor = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }

    // Dialogs
    if (showAddFeedDialog) {
        AddFeedDialog(
            urlInput = feedUrlInput,
            onUrlChange = { feedUrlInput = it },
            onConfirm = { 
                if (feedUrlInput.isNotBlank()) {
                    viewModel.addFeed(feedUrlInput)
                    feedUrlInput = ""
                }
            },
            onDismiss = { 
                viewModel.hideAddFeedDialog()
                feedUrlInput = ""
            }
        )
    }

    if (showAddGroupDialog) {
        AddGroupDialog(
            groupNameInput = groupNameInput,
            onGroupNameChange = { groupNameInput = it },
            onConfirm = { 
                if (groupNameInput.isNotBlank()) {
                    viewModel.addGroup(groupNameInput)
                    groupNameInput = ""
                }
            },
            onDismiss = { 
                viewModel.hideAddGroupDialog()
                groupNameInput = ""
            }
        )
    }

    if (showDeleteFeedDialog) {
        feedToDelete?.let { feed ->
            DeleteFeedDialog(
                feed = feed,
                onConfirm = {
                    // Call the provided callback first to update parent state
                    onDeleteFeed(feed.id)
                    // Then confirm deletion which will close dialog and update VM state
                    viewModel.confirmDeleteFeed()
                },
                onDismiss = viewModel::hideDeleteFeedDialog
            )
        }
    }

    if (showDeleteGroupDialog) {
        groupToDelete?.let { group ->
            DeleteGroupDialog(
                group = group,
                onConfirm = {
                    viewModel.confirmDeleteGroup()
                    // Also call the provided callback
                    onDeleteGroup(group.id)
                },
                onDismiss = viewModel::hideDeleteGroupDialog
            )
        }
    }

    if (showFeedGroupDialog && selectedFeedForGrouping != null) {
        FeedGroupDialog(
            feedId = selectedFeedForGrouping,
            groups = groups,
            selectedGroupIds = feedGroupAssignments,
            onToggleGroup = viewModel::toggleFeedGroup,
            onConfirm = viewModel::confirmFeedGroupAssignment,
            onDismiss = viewModel::hideFeedGroupDialog
        )
    }

    if (showEditGroupDialog && groupToEdit != null) {
        EditGroupDialog(
            group = groupToEdit!!,
            feeds = feeds,
            selectedFeedIds = groupEditFeedAssignments,
            onFeedToggle = viewModel::toggleGroupEditFeedAssignment,
            onConfirm = { name, isDefault, notificationsEnabled ->
                viewModel.updateGroup(
                    groupToEdit!!.id,
                    name,
                    isDefault,
                    notificationsEnabled
                )
            },
            onDismiss = viewModel::hideEditGroupDialog
        )
    }
    
    // OPML Import Dialogs
    if (showFileDialog) {
        OpmlFilePickerDialog(
            onFileSelected = { uri ->
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    inputStream?.let { opmlImportViewModel.parseOpmlFile(it) }
                } catch (e: Exception) {
                    // Handle error - could show error message
                }
            },
            onDismiss = opmlImportViewModel::hideFileDialog
        )
    }
    
    if (showPreviewDialog && importResult != null) {
        OpmlPreviewDialog(
            importResult = importResult!!,
            onImport = { skipDuplicates ->
                opmlImportViewModel.executeImport(skipDuplicates)
            },
            onDismiss = opmlImportViewModel::hidePreviewDialog
        )
    }
    
    when (importState) {
        is com.defnf.syndicate.ui.viewmodel.OpmlImportState.Parsing,
        is com.defnf.syndicate.ui.viewmodel.OpmlImportState.Importing -> {
            OpmlImportProgressDialog()
        }
        is com.defnf.syndicate.ui.viewmodel.OpmlImportState.Success -> {
            OpmlImportResultDialog(
                success = true,
                message = (importState as com.defnf.syndicate.ui.viewmodel.OpmlImportState.Success).message,
                onDismiss = opmlImportViewModel::resetState
            )
        }
        is com.defnf.syndicate.ui.viewmodel.OpmlImportState.Error -> {
            importErrorMessage?.let { message ->
                OpmlImportResultDialog(
                    success = false,
                    message = message,
                    onDismiss = opmlImportViewModel::clearError
                )
            }
        }
        else -> { /* No dialog needed */ }
    }
}

@Composable
private fun FeedListContent(
    feeds: List<com.defnf.syndicate.data.models.Feed>,
    groups: List<com.defnf.syndicate.data.models.Group>,
    isLoading: Boolean,
    onFeedClick: (Long) -> Unit,
    onDeleteFeed: (Long) -> Unit,
    onNotificationToggle: (Long) -> Unit,
    onGroupClick: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onEditGroup: (Long) -> Unit = {},
    resetSwipeState: Boolean,
    listState: LazyListState,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    selectedFeedId: Long? = null,
    selectedGroupId: Long? = null,
    onAllFeedsClick: () -> Unit = {},
    isSidebarMode: Boolean = false
) {
    if (feeds.isEmpty() && !isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RssFeed,
                    contentDescription = null,
                    modifier = Modifier.width(64.dp).height(64.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "No feeds yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Tap the + button to add your first RSS feed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Group 1: All Articles (independent)
            item(
                key = "all_articles_group",
                contentType = "card_group"
            ) {
                GroupedCard(
                    isFirstInGroup = true,
                    isLastInGroup = true,
                    content = {
                        AllArticlesItem(
                            isSelected = isSidebarMode && selectedFeedId == null && selectedGroupId == null,
                            onAllFeedsClick = onAllFeedsClick
                        )
                    }
                )
            }
            
            // Spacer between groups
            item(key = "spacer_1") {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Group 2: Feed Groups
            if (groups.isNotEmpty()) {
                items(
                    items = groups.withIndex().toList(),
                    key = { "group_${it.value.id}" },
                    contentType = { "group" }
                ) { (index, group) ->
                    GroupedCard(
                        isFirstInGroup = index == 0,
                        isLastInGroup = index == groups.size - 1,
                        content = {
                            SwipeableGroupItem(
                                group = group,
                                onGroupClick = onGroupClick,
                                onDeleteGroup = onDeleteGroup,
                                onEditGroup = onEditGroup,
                                isSidebarMode = isSidebarMode,
                                isSelected = selectedGroupId == group.id
                            )
                        }
                    )
                }
            }
            
            // Spacer between groups
            if (groups.isNotEmpty()) {
                item(key = "spacer_2") {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            
            // Group 3: Individual Feeds
            if (feeds.isNotEmpty()) {
                items(
                    items = feeds.withIndex().toList(),
                    key = { "feed_${it.value.id}" },
                    contentType = { "feed" }
                ) { (index, feed) ->
                    GroupedCard(
                        isFirstInGroup = index == 0,
                        isLastInGroup = index == feeds.size - 1,
                        content = {
                            SwipeableFeedItem(
                                feed = feed,
                                isSelected = selectedFeedId == feed.id,
                                onFeedClick = onFeedClick,
                                onDeleteFeed = onDeleteFeed,
                                onNotificationToggle = onNotificationToggle,
                                isSidebarMode = isSidebarMode,
                                resetSwipe = resetSwipeState
                            )
                        }
                    )
                }
            }
            
            // Empty spacer item at the end
            item(
                key = "bottom_spacer",
                contentType = "spacer"
            ) {
                Spacer(modifier = Modifier.height(160.dp))
            }
        }
    }
    
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

/**
 * A card container that handles grouped card styling similar to Android Settings.
 * Cards within a group have connecting corners - top card has rounded top corners only,
 * bottom card has rounded bottom corners only, middle cards have square corners.
 */
@Composable
private fun GroupedCard(
    isFirstInGroup: Boolean,
    isLastInGroup: Boolean,
    content: @Composable () -> Unit
) {
    val shape = when {
        isFirstInGroup && isLastInGroup -> RoundedCornerShape(12.dp) // Single card in group
        isFirstInGroup -> RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        isLastInGroup -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 12.dp)
        else -> RoundedCornerShape(4.dp) // Middle cards have rounded corners
    }
    
    val topPadding = if (isFirstInGroup) 0.dp else 2.dp // Gap between connected cards
    
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer // Match the list item colors
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp // No elevation
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
    ) {
        content()
    }
}

/**
 * The "All Articles" list item extracted as a separate composable for use in grouped cards
 */
@Composable
private fun AllArticlesItem(
    isSelected: Boolean,
    onAllFeedsClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = "All Articles",
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.RssFeed,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer // Same as default card color
            }
        ),
        modifier = Modifier
            .clickable { onAllFeedsClick() }
            .fillMaxWidth()
    )
}
