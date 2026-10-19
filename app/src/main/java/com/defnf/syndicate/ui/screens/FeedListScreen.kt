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
import androidx.compose.foundation.layout.PaddingValues
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
import com.defnf.syndicate.ui.components.EmptyState
import com.defnf.syndicate.ui.components.ExpandableFabMenu
import com.defnf.syndicate.ui.components.FaviconIcon
import com.defnf.syndicate.ui.components.FeedListItem
import com.defnf.syndicate.ui.components.GroupItem
import com.defnf.syndicate.ui.components.SegmentedItem
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.toShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.viewmodel.FeedListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedListScreen(
    onFeedClick: (Long) -> Unit,
    selectedFeedId: Long? = null,
    selectedGroupId: Long? = null,
    isAllArticlesSelected: Boolean = false,
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

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
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
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { snackbarData ->
                androidx.compose.material3.Snackbar(
                    snackbarData = snackbarData,
                    containerColor = MaterialTheme.colorScheme.inverseSurface,
                    contentColor = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    ) { innerPadding ->
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
            contentPadding = innerPadding,
            selectedFeedId = selectedFeedId,
            selectedGroupId = selectedGroupId,
            isAllArticlesSelected = isAllArticlesSelected,
            onAllFeedsClick = onAllFeedsClick
        )
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    contentPadding: PaddingValues,
    selectedFeedId: Long? = null,
    selectedGroupId: Long? = null,
    isAllArticlesSelected: Boolean = false,
    onAllFeedsClick: () -> Unit = {}
) {
    if (feeds.isEmpty() && !isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            EmptyState(
                icon = Icons.Default.RssFeed,
                title = "No feeds yet",
                message = "Tap the + button to add your first RSS feed"
            )
        }
    } else {
        LazyColumn(
            state = listState,
            // Horizontal padding as a modifier so swiped items are clipped at the card edges
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            // Scroll behind the system navigation bar, leaving room for the FAB menu
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + 16.dp,
                bottom = contentPadding.calculateBottomPadding() + LayoutConstants.ContentBottomPaddingForFab
            )
        ) {
            // Group 1: All Articles (independent)
            item(
                key = "all_articles_group",
                contentType = "card_group"
            ) {
                SegmentedItem(
                    index = 0,
                    count = 1,
                    selected = isAllArticlesSelected
                ) {
                    AllArticlesItem(
                        isSelected = isAllArticlesSelected,
                        onAllFeedsClick = onAllFeedsClick
                    )
                }
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
                    SegmentedItem(
                        index = index,
                        count = groups.size,
                        selected = selectedGroupId == group.id
                    ) {
                        SwipeableGroupItem(
                            group = group,
                            onGroupClick = onGroupClick,
                            onDeleteGroup = onDeleteGroup,
                            onEditGroup = onEditGroup,
                            isSelected = selectedGroupId == group.id
                        )
                    }
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
                    SegmentedItem(
                        index = index,
                        count = feeds.size,
                        selected = selectedFeedId == feed.id
                    ) {
                        SwipeableFeedItem(
                            feed = feed,
                            isSelected = selectedFeedId == feed.id,
                            onFeedClick = onFeedClick,
                            onDeleteFeed = onDeleteFeed,
                            onNotificationToggle = onNotificationToggle,
                            resetSwipe = resetSwipeState
                        )
                    }
                }
            }
        }
    }
    
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center
        ) {
            LoadingIndicator()
        }
    }
}

/**
 * The "All Articles" list item extracted as a separate composable for use in grouped cards
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
                    .clip(MaterialShapes.Cookie4Sided.toShape())
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
