package com.syndicate.rssreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.syndicate.rssreader.R
import com.syndicate.rssreader.ui.common.AppTopBar
import com.syndicate.rssreader.ui.common.LazyListScrollHandler
import com.syndicate.rssreader.ui.common.LayoutConstants
import com.syndicate.rssreader.ui.theme.CormorantGaramond
import com.syndicate.rssreader.ui.viewmodel.ArticleListViewModel
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
    onBackClick: () -> Unit = {},
    onScrollDirectionChanged: (Boolean) -> Unit = {},
    isSidebarMode: Boolean = false,
    topBarVisible: Boolean = true,
    additionalTopPadding: androidx.compose.ui.unit.Dp = 0.dp
) {
    val viewModel: ArticleListViewModel = hiltViewModel()
    val articles by viewModel.articles.collectAsState()
    val currentFeed by viewModel.currentFeed.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    val listState = rememberLazyListState()
    var topBarVisible by remember { mutableStateOf(true) }
    
    LazyListScrollHandler(
        listState = listState,
        onScrollDirectionChanged = { isScrollingDown ->
            val newVisibility = !isScrollingDown
            if (topBarVisible != newVisibility) {
                topBarVisible = newVisibility
                onScrollDirectionChanged(isScrollingDown)
            }
        }
    )
    
    LaunchedEffect(feedId, groupId, forceAllArticles) {
        when {
            feedId != null -> viewModel.setFeedId(feedId)
            groupId != null -> viewModel.setGroupId(groupId)
            forceAllArticles -> viewModel.setFeedId(null) // Explicitly show all articles
            else -> {
                // When no specific feed or group is selected, check for default group
                val defaultGroup = viewModel.getDefaultGroup()
                if (defaultGroup != null) {
                    viewModel.setGroupId(defaultGroup.id)
                } else {
                    viewModel.setFeedId(null) // Show all articles
                }
            }
        }
    }
    
    if (isSidebarMode) {
        // Sidebar content without Scaffold
        val baseTopPadding = if (topBarVisible) LayoutConstants.TopBarWithPadding else LayoutConstants.StandardPadding
        val animatedTopPadding by animateDpAsState(
            targetValue = baseTopPadding + additionalTopPadding,
            label = "topPadding"
        )
        
        ArticleListContent(
            articles = articles,
            currentFeed = currentFeed,
            isLoading = isLoading,
            feedId = feedId,
            listState = listState,
            paddingValues = androidx.compose.foundation.layout.PaddingValues(
                start = LayoutConstants.StandardPadding,
                end = LayoutConstants.StandardPadding,
                bottom = LayoutConstants.StandardPadding * 2,
                top = animatedTopPadding
            )
        )
    } else {
        // Full screen with Scaffold
        Scaffold(
            topBar = {
                AnimatedVisibility(
                    visible = topBarVisible,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    AppTopBar(
                        title = "Syndicate",
                        subtitle = when {
                            feedId != null -> currentFeed?.title ?: "Feed Articles"
                            groupId != null -> currentFeed?.title ?: "Group Articles"
                            else -> currentFeed?.title ?: "All Articles"
                        },
                        showBackButton = feedId != null || groupId != null,
                        onBackClick = onBackClick
                    )
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            val contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = if (topBarVisible) paddingValues.calculateTopPadding() else 0.dp,
                bottom = LayoutConstants.StandardPadding * 2,
                start = 0.dp,
                end = 0.dp
            )
            ArticleListContent(
                articles = articles,
                currentFeed = currentFeed,
                isLoading = isLoading,
                feedId = feedId,
                listState = listState,
                paddingValues = contentPadding
            )
        }
    }
}

@Composable
private fun ArticleListContent(
    articles: List<com.syndicate.rssreader.data.models.Article>,
    currentFeed: com.syndicate.rssreader.data.models.Feed?,
    isLoading: Boolean,
    feedId: Long?,
    listState: LazyListState,
    paddingValues: androidx.compose.foundation.layout.PaddingValues
) {
    if (articles.isEmpty() && !isLoading) {
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
                    imageVector = Icons.AutoMirrored.Filled.Article,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = if (feedId != null) "No articles in this feed" else "No articles yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (feedId != null) "This feed doesn't have any articles yet" else "Add some feeds to see articles here",
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
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(articles) { article ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = article.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        article.description?.let { description ->
                            val cleanDescription = parseHtmlText(description).trim()
                            if (cleanDescription.isNotBlank()) {
                                SelectionContainer {
                                    Text(
                                        text = cleanDescription,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = buildString {
                                append(article.feedTitle)
                                article.publishedDate?.let { date ->
                                    append(" • ")
                                    append(formatDate(date))
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
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
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
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