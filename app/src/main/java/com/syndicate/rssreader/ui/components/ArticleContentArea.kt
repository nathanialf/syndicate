package com.syndicate.rssreader.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.hilt.navigation.compose.hiltViewModel
import com.syndicate.rssreader.ui.common.Animations
import com.syndicate.rssreader.ui.navigation.NavigationState
import com.syndicate.rssreader.ui.screens.ArticleDetailScreen
import com.syndicate.rssreader.ui.screens.ArticleListScreen
import com.syndicate.rssreader.ui.screens.SettingsScreen
import com.syndicate.rssreader.ui.viewmodel.ArticleListViewModel

/**
 * Reusable article content area that handles article list, article detail, and settings views
 * Can be used in both single-pane and dual-pane layouts with consistent behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleContentArea(
    navigationState: NavigationState,
    themeViewModel: com.syndicate.rssreader.ui.viewmodel.ThemeViewModel? = null,
    isSidebarMode: Boolean = false,
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier,
    // Override parameters for single-pane mode
    overrideFeedId: Long? = null,
    overrideGroupId: Long? = null,
    overrideForceAllArticles: Boolean? = null,
    onBackClick: (() -> Unit)? = null
) {
    val articleViewModel: ArticleListViewModel = hiltViewModel()
    val currentFeed by articleViewModel.currentFeed.collectAsState()
    
    val contentState = when {
        navigationState.showSettings && themeViewModel != null -> "settings"
        navigationState.selectedArticleId != null -> "article_detail"
        else -> "articles"
    }
    
    AnimatedContent(
        targetState = contentState,
        transitionSpec = {
            Animations.contentTransition(targetState, initialState)
        },
        modifier = modifier,
        label = "article_content_transition"
    ) { state ->
        when (state) {
            "settings" -> {
                if (themeViewModel != null) {
                    if (isSidebarMode) {
                        // Dual-pane: AppTopBar handles system bar padding automatically
                        SettingsScreen(themeViewModel = themeViewModel)
                    } else {
                        // Single-pane: settings screen handles its own padding
                        SettingsScreen(themeViewModel = themeViewModel)
                    }
                }
            }
            "article_detail" -> {
                navigationState.selectedArticleId?.let { articleId ->
                    ArticleDetailScreen(
                        articleId = articleId,
                        onBackClick = navigationState.onBackFromArticle
                    )
                }
            }
            else -> {
                if (isSidebarMode) {
                    // Dual-pane: fill space without additional padding, but include system bar spacing
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Calculate padding for system bars and top bar
                        val systemBarPadding = androidx.compose.foundation.layout.WindowInsets.systemBars.asPaddingValues().calculateTopPadding()
                        val topBarPadding = if (showTopBar) com.syndicate.rssreader.ui.common.LayoutConstants.TopBarHeight else 0.dp
                        val totalTopPadding = systemBarPadding + topBarPadding
                        val listState = rememberLazyListState()
                        val coroutineScope = rememberCoroutineScope()
                        
                        ArticleListScreen(
                            feedId = overrideFeedId ?: navigationState.selectedFeedId,
                            groupId = overrideGroupId ?: navigationState.selectedGroupId,
                            forceAllArticles = overrideForceAllArticles ?: navigationState.forceAllArticles,
                            onArticleClick = { article ->
                                navigationState.onArticleSelected(article.id)
                            },
                            onBackClick = onBackClick ?: { /* Do nothing */ },
                            isSidebarMode = true,
                            additionalTopPadding = totalTopPadding,
                            externalListState = listState
                        )
                        
                        // Top app bar for dual-pane mode
                        if (showTopBar) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = androidx.compose.foundation.layout.WindowInsets.systemBars.asPaddingValues().calculateTopPadding())
                            ) {
                                androidx.compose.material3.CenterAlignedTopAppBar(
                                    title = {
                                        val titleText = when {
                                            navigationState.showSettings -> "Settings"
                                            navigationState.selectedArticleId != null -> "Article"
                                            else -> currentFeed?.title ?: "All Articles"
                                        }
                                        androidx.compose.material3.Text(
                                            text = titleText,
                                            modifier = if (!navigationState.showSettings && navigationState.selectedArticleId == null) {
                                                Modifier.clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) {
                                                    coroutineScope.launch {
                                                        listState.animateScrollToItem(0)
                                                    }
                                                }
                                            } else {
                                                Modifier
                                            }
                                        )
                                    },
                                    navigationIcon = {
                                        if (navigationState.showSettings || navigationState.selectedArticleId != null) {
                                            androidx.compose.material3.IconButton(
                                                onClick = { 
                                                    if (navigationState.showSettings) navigationState.onShowSettings(false)
                                                    if (navigationState.selectedArticleId != null) navigationState.onBackFromArticle()
                                                }
                                            ) {
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                                    contentDescription = "Back"
                                                )
                                            }
                                        }
                                    },
                                    actions = {
                                        if (!navigationState.showSettings && navigationState.selectedArticleId == null) {
                                            // Mark all as read button
                                            androidx.compose.material3.IconButton(
                                                onClick = { articleViewModel.markAllAsRead() }
                                            ) {
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.Default.DoneAll,
                                                    contentDescription = "Mark all as read"
                                                )
                                            }
                                            // Settings button
                                            androidx.compose.material3.IconButton(
                                                onClick = { navigationState.onShowSettings(true) }
                                            ) {
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.Default.Settings,
                                                    contentDescription = "Settings"
                                                )
                                            }
                                        }
                                    },
                                    windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
                                )
                            }
                        }
                    }
                } else {
                    // Single-pane: normal article list
                    ArticleListScreen(
                        feedId = overrideFeedId ?: navigationState.selectedFeedId,
                        groupId = overrideGroupId ?: navigationState.selectedGroupId,
                        forceAllArticles = overrideForceAllArticles ?: navigationState.forceAllArticles,
                        onArticleClick = { article ->
                            navigationState.onArticleSelected(article.id)
                        },
                        onBackClick = onBackClick ?: if (navigationState.selectedFeedId != null || 
                                         navigationState.selectedGroupId != null || 
                                         navigationState.forceAllArticles) {
                            navigationState.onBackFromSelection
                        } else {
                            { /* Do nothing */ }
                        }
                    )
                }
            }
        }
    }
}