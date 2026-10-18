package com.defnf.syndicate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.components.SwipeableArticleCard
import com.defnf.syndicate.ui.viewmodel.ArticleListViewModel
import com.defnf.syndicate.ui.viewmodel.ArticleShowFilter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListScreen(
    feedId: Long? = null,
    groupId: Long? = null,
    forceAllArticles: Boolean = false,
    onArticleClick: (com.defnf.syndicate.data.models.Article) -> Unit = {},
    onBackClick: (() -> Unit)? = null,
    onSettingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val viewModel: ArticleListViewModel = hiltViewModel()
    val articles by viewModel.articles.collectAsState()
    val currentFeed by viewModel.currentFeed.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshingOrStartup.collectAsState()
    val shouldScrollToTop by viewModel.shouldScrollToTop.collectAsState()
    val showFilter by viewModel.showFilter.collectAsState()

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val scrollToTop: () -> Unit = {
        coroutineScope.launch { listState.animateScrollToItem(0) }
    }
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 50
        }
    }

    LaunchedEffect(feedId, groupId, forceAllArticles) {
        when {
            feedId != null -> viewModel.setFeedId(feedId)
            groupId != null -> viewModel.setGroupId(groupId)
            else -> {
                viewModel.setFeedId(null)
                viewModel.setGroupId(null)
            }
        }
    }
    
    // Handle scroll to top after refresh
    LaunchedEffect(shouldScrollToTop) {
        if (shouldScrollToTop) {
            listState.animateScrollToItem(0)
            viewModel.onScrollToTopHandled()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentFeed?.title ?: "All Articles",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = scrollToTop
                        )
                    )
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.markAllAsRead() }) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark all as read"
                        )
                    }
                    if (onSettingsClick != null) {
                        IconButton(onClick = onSettingsClick) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings"
                            )
                        }
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            if (!isAtTop && articles.isNotEmpty()) {
                SmallFloatingActionButton(
                    onClick = scrollToTop,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Scroll to top"
                    )
                }
            }
        }
    ) { innerPadding ->
        ArticleListContent(
            articles = articles,
            isLoading = isLoading,
            isRefreshing = isRefreshing,
            feedId = feedId,
            showFilter = showFilter,
            onFilterChange = viewModel::setShowFilter,
            onArticleClick = onArticleClick,
            onToggleReadState = { article ->
                if (article.isRead) {
                    viewModel.markAsUnread(article.id)
                } else {
                    viewModel.markAsRead(article.id)
                }
            },
            listState = listState,
            contentPadding = innerPadding,
            onRefresh = { viewModel.refreshFeeds() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleListContent(
    articles: List<com.defnf.syndicate.data.models.Article>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    feedId: Long?,
    showFilter: ArticleShowFilter,
    onFilterChange: (ArticleShowFilter) -> Unit,
    onArticleClick: (com.defnf.syndicate.data.models.Article) -> Unit,
    onToggleReadState: (com.defnf.syndicate.data.models.Article) -> Unit,
    listState: LazyListState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit = {}
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Segmented buttons for filtering
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    onClick = { onFilterChange(ArticleShowFilter.UNREAD) },
                    selected = showFilter == ArticleShowFilter.UNREAD
                ) {
                    Text("Unread")
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    onClick = { onFilterChange(ArticleShowFilter.ALL) },
                    selected = showFilter == ArticleShowFilter.ALL
                ) {
                    Text("All")
                }
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    onClick = { onFilterChange(ArticleShowFilter.READ) },
                    selected = showFilter == ArticleShowFilter.READ
                ) {
                    Text("Read")
                }
            }

            // Articles list
            if (articles.isEmpty() && !isLoading) {
                // Use LazyColumn for empty state to enable pull-to-refresh
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Article,
                                contentDescription = null,
                                modifier = Modifier.padding(16.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = when (showFilter) {
                                    ArticleShowFilter.UNREAD -> "No unread articles"
                                    ArticleShowFilter.READ -> "No read articles"
                                    ArticleShowFilter.ALL -> if (feedId != null) "No articles in this feed" else "No articles yet"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = when (showFilter) {
                                    ArticleShowFilter.UNREAD -> "All articles have been read"
                                    ArticleShowFilter.READ -> "No articles have been read yet"
                                    ArticleShowFilter.ALL -> if (feedId != null) "This feed doesn't have any articles yet" else "Add some feeds to see articles here"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    // Scroll behind the system navigation bar, leaving room for the scroll to top button
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = contentPadding.calculateBottomPadding() + LayoutConstants.ContentBottomPaddingForFab
                    )
                ) {
                    items(
                        items = articles,
                        key = { article -> article.id }
                    ) { article ->
                        SwipeableArticleCard(
                            article = article,
                            onArticleClick = onArticleClick,
                            onToggleReadState = onToggleReadState,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    
    calendar.time = date
    val articleYear = calendar.get(Calendar.YEAR)
    
    val dateFormat = if (articleYear == currentYear) {
        SimpleDateFormat("MMM dd", Locale.getDefault())
    } else {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }
    
    return dateFormat.format(date)
}

private fun parseHtmlText(html: String): String {
    return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
}