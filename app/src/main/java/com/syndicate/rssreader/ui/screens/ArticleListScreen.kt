package com.syndicate.rssreader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.syndicate.rssreader.R
import com.syndicate.rssreader.ui.common.AppTopBar
import com.syndicate.rssreader.ui.common.LayoutConstants
import com.syndicate.rssreader.ui.components.SwipeableArticleCard
import com.syndicate.rssreader.ui.theme.CormorantGaramond
import com.syndicate.rssreader.ui.viewmodel.ArticleListViewModel
import com.syndicate.rssreader.ui.viewmodel.ArticleShowFilter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ArticleListScreen(
    feedId: Long? = null,
    groupId: Long? = null,
    forceAllArticles: Boolean = false,
    onBackClick: () -> Unit = {},
    onArticleClick: (com.syndicate.rssreader.data.models.Article) -> Unit = {},
    isSidebarMode: Boolean = false,
    additionalTopPadding: androidx.compose.ui.unit.Dp = 0.dp,
    externalListState: LazyListState? = null
) {
    val viewModel: ArticleListViewModel = hiltViewModel()
    val articles by viewModel.articles.collectAsState()
    val currentFeed by viewModel.currentFeed.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val shouldScrollToTop by viewModel.shouldScrollToTop.collectAsState()
    val showFilter by viewModel.showFilter.collectAsState()

    val listState = externalListState ?: rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(feedId, groupId, forceAllArticles) {
        when {
            feedId != null -> viewModel.setFeedId(feedId)
            groupId != null -> viewModel.setGroupId(groupId)
            forceAllArticles -> {
                viewModel.setFeedId(null)
                viewModel.setGroupId(null)
            }
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

    if (!isSidebarMode) {
        Scaffold(
            topBar = {
                AppTopBar(
                    title = "Syndicate",
                    subtitle = currentFeed?.title ?: "All Articles",
                    showBackButton = feedId != null || groupId != null || forceAllArticles,
                    onBackClick = onBackClick,
                    showMarkAllAsReadButton = true,
                    onMarkAllAsReadClick = { viewModel.markAllAsRead() },
                    onTitleClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(0)
                        }
                    }
                )
            }
        ) { paddingValues ->
            ArticleListContent(
                articles = articles,
                currentFeed = currentFeed,
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
                paddingValues = paddingValues,
                onRefresh = { viewModel.refreshFeeds() },
                isSidebarMode = isSidebarMode
            )
        }
    } else {
        ArticleListContent(
            articles = articles,
            currentFeed = currentFeed,
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
            paddingValues = androidx.compose.foundation.layout.PaddingValues(
                top = additionalTopPadding
            ),
            onRefresh = { viewModel.refreshFeeds() },
            isSidebarMode = isSidebarMode
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleListContent(
    articles: List<com.syndicate.rssreader.data.models.Article>,
    currentFeed: com.syndicate.rssreader.data.models.Feed?,
    isLoading: Boolean,
    isRefreshing: Boolean,
    feedId: Long?,
    showFilter: ArticleShowFilter,
    onFilterChange: (ArticleShowFilter) -> Unit,
    onArticleClick: (com.syndicate.rssreader.data.models.Article) -> Unit,
    onToggleReadState: (com.syndicate.rssreader.data.models.Article) -> Unit,
    listState: LazyListState,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    onRefresh: () -> Unit = {},
    isSidebarMode: Boolean = false
) {
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Filter chips
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                onClick = { onFilterChange(ArticleShowFilter.UNREAD) },
                label = { Text("Unread") },
                selected = showFilter == ArticleShowFilter.UNREAD
            )
            FilterChip(
                onClick = { onFilterChange(ArticleShowFilter.ALL) },
                label = { Text("All") },
                selected = showFilter == ArticleShowFilter.ALL
            )
            FilterChip(
                onClick = { onFilterChange(ArticleShowFilter.READ) },
                label = { Text("Read") },
                selected = showFilter == ArticleShowFilter.READ
            )
        }
        
        // Articles list
        if (articles.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
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
                
                // Empty spacer item at the end (only in single pane mode)
                if (!isSidebarMode) {
                    item {
                        Spacer(modifier = Modifier.height(160.dp))
                    }
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