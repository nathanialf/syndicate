package com.syndicate.rssreader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.navigation.compose.hiltViewModel
import android.util.Log
import com.syndicate.rssreader.ui.common.AppTopBar
import com.syndicate.rssreader.ui.common.LazyListScrollHandler
import com.syndicate.rssreader.ui.common.LayoutConstants
import com.syndicate.rssreader.ui.theme.CormorantGaramond
import com.syndicate.rssreader.ui.viewmodel.FeedListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedListScreen(
    onFeedClick: (Long) -> Unit = {},
    onScrollDirectionChanged: (Boolean) -> Unit = {},
    isSidebarMode: Boolean = false,
    selectedFeedId: Long? = null,
    showAllFeedsOption: Boolean = false,
    onAllFeedsClick: () -> Unit = {},
    onDeleteFeed: (Long) -> Unit = {}
) {
    val viewModel: FeedListViewModel = hiltViewModel()
    val feeds by viewModel.feeds.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val showAddFeedDialog by viewModel.showAddFeedDialog.collectAsState()
    
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    var urlInput by remember { mutableStateOf("") }
    var fabExpanded by remember { mutableStateOf(false) }
    
    // Calculate precise positioning using system bars
    val density = LocalDensity.current
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
    val bottomNavHeight = 56.dp // Standard Material 3 bottom nav height
    
    val fabBottomPadding = if (isSidebarMode) {
        LayoutConstants.FabBottomPadding
    } else {
        // For phone mode: system nav bar + bottom nav + generous spacing for full visibility
        systemBarsPadding.calculateBottomPadding() + bottomNavHeight + 48.dp
    }
    
    LazyListScrollHandler(
        listState = listState,
        onScrollDirectionChanged = onScrollDirectionChanged
    )
    
    // Debug logging
    Log.d("FeedListScreen", "isSidebarMode: $isSidebarMode, feeds.size: ${feeds.size}, isLoading: $isLoading")
    Log.d("FeedListScreen", "Calculated FAB bottom padding: $fabBottomPadding (systemBottom: ${systemBarsPadding.calculateBottomPadding()})")
    
    LaunchedEffect(errorMessage) {
        errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }
    
    // Use a unified approach with Box and overlay FAB for both modes
    Box(modifier = Modifier.fillMaxSize()) {
        // Content area - use weight to prevent overlap with FAB
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar only for full screen mode
            if (!isSidebarMode) {
                AppTopBar(title = "Syndicate", subtitle = "Feeds")
            }
            
            // Feed list content
            FeedListContent(
                feeds = feeds,
                isLoading = isLoading,
                onFeedClick = onFeedClick,
                onDeleteFeed = { feedId ->
                    viewModel.deleteFeed(feedId)
                    onDeleteFeed(feedId) // Also call the provided callback
                },
                listState = listState,
                paddingValues = androidx.compose.foundation.layout.PaddingValues(
                    start = 0.dp,
                    end = 0.dp,
                    top = 0.dp,
                    bottom = fabBottomPadding + 56.dp // FAB height + extra space
                ),
                selectedFeedId = selectedFeedId,
                showAllFeedsOption = showAllFeedsOption,
                onAllFeedsClick = onAllFeedsClick,
                isSidebarMode = isSidebarMode
            )
        }
        
        // Expandable FAB Menu (place before snackbar for proper z-ordering)
        ExpandableFabMenu(
            expanded = fabExpanded,
            onToggle = { fabExpanded = !fabExpanded },
            onAddFeed = { 
                fabExpanded = false
                viewModel.showAddFeedDialog() 
            },
            onImportFeed = {
                fabExpanded = false
                // TODO: Implement import feed functionality
                Log.d("FeedListScreen", "Import feed clicked")
            },
            onAddGroup = {
                fabExpanded = false
                // TODO: Implement add group functionality
                Log.d("FeedListScreen", "Add group clicked")
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    bottom = fabBottomPadding,
                    end = LayoutConstants.FabEndPadding
                )
        )
        
        // Snackbar host for error messages (only in full screen mode)
        if (!isSidebarMode) {
            Log.d("FeedListScreen", "Creating SnackbarHost for phone mode")
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        } else {
            Log.d("FeedListScreen", "Skipping SnackbarHost for sidebar mode")
        }
    }
    
    if (showAddFeedDialog) {
        AddFeedDialog(
            urlInput = urlInput,
            onUrlChange = { urlInput = it },
            onConfirm = {
                viewModel.addFeed(urlInput)
                urlInput = ""
            },
            onDismiss = { 
                viewModel.hideAddFeedDialog()
                urlInput = ""
            }
        )
    }
}

@Composable
private fun FeedListContent(
    feeds: List<com.syndicate.rssreader.data.models.Feed>,
    isLoading: Boolean,
    onFeedClick: (Long) -> Unit,
    onDeleteFeed: (Long) -> Unit,
    listState: LazyListState,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    selectedFeedId: Long? = null,
    showAllFeedsOption: Boolean = false,
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
            // Add "All Feeds" option if requested
            if (showAllFeedsOption) {
                item {
                    val isAllFeedsSelected = selectedFeedId == null
                    
                    if (isSidebarMode) {
                        // Sidebar mode: Individual clickable item with padding and rounded corners
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAllFeedsClick() },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAllFeedsSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            tonalElevation = if (isAllFeedsSelected) 2.dp else 0.dp
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(
                                        text = "All Feeds",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isAllFeedsSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                },
                                leadingContent = {
                                    Icon(
                                        imageVector = Icons.Default.RssFeed,
                                        contentDescription = null,
                                        tint = if (isAllFeedsSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                colors = androidx.compose.material3.ListItemDefaults.colors(
                                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                                )
                            )
                        }
                    } else {
                        // Regular mode: Standard list item
                        ListItem(
                            headlineContent = {
                                Text(
                                    text = "All Feeds",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isAllFeedsSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            },
                            leadingContent = {
                                Icon(
                                    imageVector = Icons.Default.RssFeed,
                                    contentDescription = null,
                                    tint = if (isAllFeedsSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            colors = androidx.compose.material3.ListItemDefaults.colors(
                                containerColor = if (isAllFeedsSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            modifier = Modifier
                                .clickable { onAllFeedsClick() }
                                .fillMaxWidth()
                        )
                    }
                }
            }
            
            // Add divider between "All Feeds" and individual feeds in two pane mode
            if (showAllFeedsOption) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            
            items(feeds, key = { it.id }) { feed ->
                SwipeToDeleteFeedItem(
                    feed = feed,
                    isSelected = selectedFeedId == feed.id,
                    onFeedClick = onFeedClick,
                    onDeleteFeed = onDeleteFeed,
                    isSidebarMode = isSidebarMode
                )
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

@Composable
private fun AddFeedDialog(
    urlInput: String,
    onUrlChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add RSS Feed")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Enter the URL of the RSS feed you want to add:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = onUrlChange,
                    label = { Text("Feed URL") },
                    placeholder = { Text("https://example.com/feed.xml") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = urlInput.isNotBlank()
            ) {
                Text("Add")
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
private fun ExpandableFabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddFeed: () -> Unit,
    onImportFeed: () -> Unit,
    onAddGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = tween(300),
        label = "fab_rotation"
    )
    
    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) 28.dp else 16.dp, // 28dp for circle, 16dp for default FAB
        animationSpec = tween<androidx.compose.ui.unit.Dp>(300),
        label = "fab_corner_radius"
    )
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Menu items (shown when expanded)
        AnimatedVisibility(
            visible = expanded,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeIn(animationSpec = tween(300)),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300))
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add Group
                FabMenuItem(
                    icon = Icons.Default.Folder,
                    label = "Add Group",
                    onClick = onAddGroup
                )
                
                // Import Feed
                FabMenuItem(
                    icon = Icons.Default.Download,
                    label = "Import Feed",
                    onClick = onImportFeed
                )
                
                // Add Feed
                FabMenuItem(
                    icon = Icons.Default.RssFeed,
                    label = "Add Feed",
                    onClick = onAddFeed
                )
            }
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = onToggle,
            containerColor = if (expanded) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = if (expanded) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer,
            shape = RoundedCornerShape(cornerRadius),
            elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (expanded) "Close menu" else "Open menu",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
private fun FabMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(50),
        elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 12.dp
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteFeedItem(
    feed: com.syndicate.rssreader.data.models.Feed,
    isSelected: Boolean,
    onFeedClick: (Long) -> Unit,
    onDeleteFeed: (Long) -> Unit,
    isSidebarMode: Boolean = false
) {
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Swipe right - Delete
                    onDeleteFeed(feed.id)
                    true
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Swipe left - Add to group (placeholder for now)
                    false // Don't dismiss yet since we haven't implemented this
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            SwipeBackground(
                swipeDirection = swipeState.targetValue
            )
        },
        content = {
            if (isSidebarMode) {
                // Sidebar mode: Individual clickable item with padding and rounded corners
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onFeedClick(feed.id) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    tonalElevation = if (isSelected) 2.dp else 0.dp
                ) {
                    ListItem(
                        headlineContent = {
                            Text(
                                text = feed.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        },
                        supportingContent = feed.description?.takeIf { it != feed.title && it.isNotBlank() }?.let { description ->
                            {
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        },
                        leadingContent = {
                            FaviconIcon(
                                feed = feed,
                                isSelected = isSelected,
                                isAvailable = feed.isAvailable
                            )
                        },
                        colors = androidx.compose.material3.ListItemDefaults.colors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent
                        )
                    )
                }
            } else {
                // Regular mode: Standard list item
                ListItem(
                    headlineContent = {
                        Text(
                            text = feed.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    },
                    supportingContent = feed.description?.takeIf { it != feed.title && it.isNotBlank() }?.let { description ->
                        {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    },
                    leadingContent = {
                        FaviconIcon(
                            feed = feed,
                            isSelected = isSelected,
                            isAvailable = feed.isAvailable
                        )
                    },
                    colors = androidx.compose.material3.ListItemDefaults.colors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ),
                    modifier = Modifier
                        .clickable { onFeedClick(feed.id) }
                        .fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun SwipeBackground(
    swipeDirection: SwipeToDismissBoxValue
) {
    val backgroundColor = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    val iconColor = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onErrorContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val icon = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.CreateNewFolder
        else -> null
    }
    
    val alignment = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    // Animate the background color and icon visibility
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(300),
        label = "swipe_background_color"
    )
    
    // Animate icon scale and alpha for smooth appearance
    val iconScale by animateFloatAsState(
        targetValue = if (swipeDirection != SwipeToDismissBoxValue.Settled) 1f else 0f,
        animationSpec = tween(300),
        label = "swipe_icon_scale"
    )
    
    val iconAlpha by animateFloatAsState(
        targetValue = if (swipeDirection != SwipeToDismissBoxValue.Settled) 1f else 0f,
        animationSpec = tween(300),
        label = "swipe_icon_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBackgroundColor)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = when (swipeDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> "Delete feed"
                    SwipeToDismissBoxValue.EndToStart -> "Add to group"
                    else -> null
                },
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .scale(iconScale)
                    .alpha(iconAlpha)
            )
        }
    }
}

@Composable
private fun FaviconIcon(
    feed: com.syndicate.rssreader.data.models.Feed,
    isSelected: Boolean,
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else if (isAvailable) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.outline
    }

    var imageLoadSuccess by remember { mutableStateOf(true) }
    
    // Generate favicon URL from siteUrl if faviconUrl is not available
    val effectiveFaviconUrl = feed.faviconUrl ?: run {
        val baseUrl = feed.siteUrl ?: run {
            // Extract domain from RSS feed URL as fallback
            try {
                val feedUrl = java.net.URL(feed.url)
                "${feedUrl.protocol}://${feedUrl.host}"
            } catch (e: Exception) {
                null
            }
        }
        baseUrl?.let { "$it/favicon.ico" }
    }

    if (!effectiveFaviconUrl.isNullOrBlank() && imageLoadSuccess) {
        AsyncImage(
            model = effectiveFaviconUrl,
            contentDescription = null,
            modifier = modifier
                .size(24.dp)
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape = CircleShape
                ),
            onError = { 
                imageLoadSuccess = false
            }
        )
    } else {
        Icon(
            imageVector = Icons.Default.RssFeed,
            contentDescription = null,
            tint = iconTint,
            modifier = modifier.size(24.dp)
        )
    }
}