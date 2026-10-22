package com.defnf.syndicate.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.components.EmptyState
import com.defnf.syndicate.ui.components.ExpandableFabMenu
import com.defnf.syndicate.ui.components.GroupedListDefaults
import com.defnf.syndicate.ui.components.IconBadge
import com.defnf.syndicate.ui.components.ListSectionHeader
import com.defnf.syndicate.ui.components.SwipeableFeedItem
import com.defnf.syndicate.ui.components.SwipeableGroupItem
import com.defnf.syndicate.ui.components.groupedItemGap
import com.defnf.syndicate.ui.dialogs.AddFeedDialog
import com.defnf.syndicate.ui.dialogs.AddGroupDialog
import com.defnf.syndicate.ui.dialogs.DeleteFeedDialog
import com.defnf.syndicate.ui.dialogs.DeleteGroupDialog
import com.defnf.syndicate.ui.dialogs.OpmlFilePickerDialog
import com.defnf.syndicate.ui.dialogs.OpmlImportProgressDialog
import com.defnf.syndicate.ui.dialogs.OpmlImportResultDialog
import com.defnf.syndicate.ui.dialogs.OpmlPreviewDialog
import com.defnf.syndicate.ui.sheets.FeedOptionsSheet
import com.defnf.syndicate.ui.sheets.GroupOptionsSheet
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
    val message by viewModel.message.collectAsState()
    val appNotificationsEnabled by viewModel.appNotificationsEnabled.collectAsState()
    val showAddFeedDialog by viewModel.showAddFeedDialog.collectAsState()
    val showAddGroupDialog by viewModel.showAddGroupDialog.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var fabExpanded by remember { mutableStateOf(false) }
    var feedUrlInput by remember { mutableStateOf("") }
    var groupNameInput by remember { mutableStateOf("") }

    val feedOptionsId by viewModel.feedOptionsId.collectAsState()
    val feedOptionsGroupIds by viewModel.feedOptionsGroupIds.collectAsState()
    val groupOptionsId by viewModel.groupOptionsId.collectAsState()
    val groupOptionsFeedIds by viewModel.groupOptionsFeedIds.collectAsState()
    val feedToDelete by viewModel.feedToDelete.collectAsState()
    val groupToDelete by viewModel.groupToDelete.collectAsState()

    // OPML Import state
    val importState by opmlImportViewModel.importState.collectAsState()
    val showFileDialog by opmlImportViewModel.showFileDialog.collectAsState()
    val showPreviewDialog by opmlImportViewModel.showPreviewDialog.collectAsState()
    val importResult by opmlImportViewModel.importResult.collectAsState()
    val importErrorMessage by opmlImportViewModel.errorMessage.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(message) {
        message?.let {
            val result = snackbarHostState.showSnackbar(
                message = it.text,
                actionLabel = it.actionLabel,
                duration = if (it.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) it.onAction?.invoke()
            viewModel.clearMessage()
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
            onFeedLongClick = viewModel::showFeedOptions,
            onDeleteFeed = viewModel::requestDeleteFeed,
            onFeedNotificationToggle = viewModel::toggleFeedNotifications,
            onGroupClick = onGroupClick,
            onGroupLongClick = viewModel::showGroupOptions,
            onDeleteGroup = viewModel::requestDeleteGroup,
            onGroupNotificationToggle = viewModel::toggleGroupNotifications,
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

    feedOptionsId?.let { feedId ->
        feeds.find { it.id == feedId }?.let { feed ->
            FeedOptionsSheet(
                feed = feed,
                groups = groups,
                groupIds = feedOptionsGroupIds,
                appNotificationsEnabled = appNotificationsEnabled,
                onNotificationsChange = { viewModel.setFeedNotifications(feed.id, it) },
                onGroupChange = { groupId, inGroup -> viewModel.setFeedInGroup(feed.id, groupId, inGroup) },
                onCreateGroup = { name -> viewModel.createGroupWithFeed(feed.id, name) },
                onDelete = {
                    viewModel.hideFeedOptions()
                    viewModel.requestDeleteFeed(feed.id)
                },
                onDismiss = viewModel::hideFeedOptions
            )
        }
    }

    groupOptionsId?.let { groupId ->
        groups.find { it.id == groupId }?.let { group ->
            GroupOptionsSheet(
                group = group,
                feeds = feeds,
                feedIds = groupOptionsFeedIds,
                appNotificationsEnabled = appNotificationsEnabled,
                onRename = { name -> viewModel.renameGroup(group.id, name) },
                onDefaultChange = { viewModel.setDefaultGroup(group.id, it) },
                onNotificationsChange = { viewModel.setGroupNotifications(group.id, it) },
                onFeedChange = { feedId, inGroup -> viewModel.setFeedInGroup(feedId, group.id, inGroup) },
                onDelete = {
                    viewModel.hideGroupOptions()
                    viewModel.requestDeleteGroup(group.id)
                },
                onDismiss = viewModel::hideGroupOptions
            )
        }
    }

    feedToDelete?.let { feed ->
        DeleteFeedDialog(
            feed = feed,
            onConfirm = {
                // Let navigation drop the feed if it's open before it disappears
                onDeleteFeed(feed.id)
                viewModel.confirmDeleteFeed()
            },
            onDismiss = viewModel::cancelDeleteFeed
        )
    }

    groupToDelete?.let { group ->
        DeleteGroupDialog(
            group = group,
            onConfirm = {
                onDeleteGroup(group.id)
                viewModel.confirmDeleteGroup()
            },
            onDismiss = viewModel::cancelDeleteGroup
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
    onFeedLongClick: (Long) -> Unit,
    onDeleteFeed: (Long) -> Unit,
    onFeedNotificationToggle: (Long) -> Unit,
    onGroupClick: (Long) -> Unit,
    onGroupLongClick: (Long) -> Unit,
    onDeleteGroup: (Long) -> Unit,
    onGroupNotificationToggle: (Long) -> Unit,
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
                AllArticlesItem(
                    isSelected = isAllArticlesSelected,
                    onAllFeedsClick = onAllFeedsClick
                )
            }

            // Group 2: Feed Groups
            if (groups.isNotEmpty()) {
                item(key = "groups_header", contentType = "header") {
                    ListSectionHeader(text = "Groups", modifier = Modifier.padding(top = 24.dp))
                }
                items(
                    items = groups.withIndex().toList(),
                    key = { "group_${it.value.id}" },
                    contentType = { "group" }
                ) { (index, group) ->
                    val selected = selectedGroupId == group.id
                    SwipeableGroupItem(
                        group = group,
                        isSelected = selected,
                        shapes = GroupedListDefaults.shapes(index, groups.size),
                        onGroupClick = onGroupClick,
                        onGroupLongClick = onGroupLongClick,
                        onDeleteGroup = onDeleteGroup,
                        onNotificationToggle = onGroupNotificationToggle,
                        // The swipe actions show through the item's own shape
                        modifier = Modifier
                            .groupedItemGap(index)
                            .clip(GroupedListDefaults.currentShape(index, groups.size, selected))
                    )
                }
            }

            // Group 3: Individual Feeds
            if (feeds.isNotEmpty()) {
                item(key = "feeds_header", contentType = "header") {
                    ListSectionHeader(text = "Subscriptions", modifier = Modifier.padding(top = 24.dp))
                }
                items(
                    items = feeds.withIndex().toList(),
                    key = { "feed_${it.value.id}" },
                    contentType = { "feed" }
                ) { (index, feed) ->
                    val selected = selectedFeedId == feed.id
                    SwipeableFeedItem(
                        feed = feed,
                        isSelected = selected,
                        shapes = GroupedListDefaults.shapes(index, feeds.size),
                        onFeedClick = onFeedClick,
                        onFeedLongClick = onFeedLongClick,
                        onDeleteFeed = onDeleteFeed,
                        onNotificationToggle = onFeedNotificationToggle,
                        modifier = Modifier
                            .groupedItemGap(index)
                            .clip(GroupedListDefaults.currentShape(index, feeds.size, selected))
                    )
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

/** Every article, in a group of its own above the groups and feeds. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AllArticlesItem(isSelected: Boolean, onAllFeedsClick: () -> Unit) {
    SegmentedListItem(
        selected = isSelected,
        onClick = onAllFeedsClick,
        shapes = GroupedListDefaults.shapes(index = 0, count = 1),
        colors = GroupedListDefaults.colors(),
        leadingContent = {
            IconBadge(
                icon = Icons.Default.RssFeed,
                // The selected row takes a strong tone; a surface-colored badge stays clear on it
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
        }
    ) {
        Text("All Articles")
    }
}
