package com.defnf.syndicate.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import com.defnf.syndicate.ui.components.ConnectedToggleButtonGroup
import com.defnf.syndicate.ui.components.EmptyState
import com.defnf.syndicate.ui.components.SwipeableArticleCard
import com.defnf.syndicate.ui.viewmodel.ArticleListViewModel
import com.defnf.syndicate.ui.viewmodel.ArticleShowFilter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val snackbarHostState = remember { SnackbarHostState() }
    val setReadState: (String, Boolean) -> Unit = { articleId, isRead ->
        if (isRead) viewModel.markAsRead(articleId) else viewModel.markAsUnread(articleId)
    }
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
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
                subtitle = {
                    Text(
                        // No count until the articles have loaded, rather than a misleading "0"
                        text = if (isLoading && articles.isEmpty()) "" else articleCountLabel(articles.size, showFilter),
                        maxLines = 1
                    )
                },
                titleHorizontalAlignment = Alignment.CenterHorizontally,
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick, shapes = IconButtonDefaults.shapes()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.markAllAsRead() },
                        shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark all as read"
                        )
                    }
                    if (onSettingsClick != null) {
                        IconButton(onClick = onSettingsClick, shapes = IconButtonDefaults.shapes()) {
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
            AnimatedVisibility(
                visible = !isAtTop && articles.isNotEmpty(),
                enter = scaleIn(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeIn(MaterialTheme.motionScheme.fastEffectsSpec()),
                exit = scaleOut(MaterialTheme.motionScheme.fastSpatialSpec()) +
                    fadeOut(MaterialTheme.motionScheme.fastEffectsSpec())
            ) {
                SmallFloatingActionButton(onClick = scrollToTop) {
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
                val markRead = !article.isRead
                setReadState(article.id, markRead)
                // The filter hides the article once it changes, so offer a way to bring it back
                val leavesList = (markRead && showFilter == ArticleShowFilter.UNREAD) ||
                    (!markRead && showFilter == ArticleShowFilter.READ)
                if (leavesList) {
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        val result = snackbarHostState.showSnackbar(
                            message = if (markRead) "Marked as read" else "Marked as unread",
                            actionLabel = "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) setReadState(article.id, !markRead)
                    }
                }
            },
            listState = listState,
            contentPadding = innerPadding,
            onRefresh = { viewModel.refreshFeeds() }
        )
    }
}

private fun articleCountLabel(count: Int, filter: ArticleShowFilter): String {
    val noun = if (count == 1) "article" else "articles"
    return when (filter) {
        ArticleShowFilter.UNREAD -> "$count unread"
        ArticleShowFilter.READ -> "$count read"
        ArticleShowFilter.ALL -> "$count $noun"
    }
}

/** Connected button group choosing which articles are listed. */
@Composable
private fun ArticleFilterButtonGroup(
    selected: ArticleShowFilter,
    onSelect: (ArticleShowFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    ConnectedToggleButtonGroup(
        options = listOf(
            ArticleShowFilter.UNREAD to "Unread",
            ArticleShowFilter.ALL to "All",
            ArticleShowFilter.READ to "Read"
        ),
        selected = selected,
        onSelect = onSelect,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding())
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            ArticleFilterButtonGroup(
                selected = showFilter,
                onSelect = onFilterChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

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
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.Article,
                            title = when (showFilter) {
                                ArticleShowFilter.UNREAD -> "No unread articles"
                                ArticleShowFilter.READ -> "No read articles"
                                ArticleShowFilter.ALL -> if (feedId != null) "No articles in this feed" else "No articles yet"
                            },
                            message = when (showFilter) {
                                ArticleShowFilter.UNREAD -> "All articles have been read"
                                ArticleShowFilter.READ -> "No articles have been read yet"
                                ArticleShowFilter.ALL -> if (feedId != null) "This feed doesn't have any articles yet" else "Add some feeds to see articles here"
                            }
                        )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItem()
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