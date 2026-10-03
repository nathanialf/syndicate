package com.defnf.syndicate.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.navigation.compose.hiltViewModel
import com.defnf.syndicate.ui.common.Animations
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.navigation.NavigationState
import com.defnf.syndicate.ui.navigation.TopLevelDestination
import com.defnf.syndicate.ui.screens.ArticleDetailScreen
import com.defnf.syndicate.ui.screens.ArticleListScreen
import com.defnf.syndicate.ui.screens.SettingsScreen
import com.defnf.syndicate.ui.viewmodel.ArticleListViewModel
import kotlinx.coroutines.launch

/**
 * Reusable article content area that switches between article list, article detail and settings.
 * Used by the single-pane layouts and by the content pane of the two-pane layout.
 */
@Composable
fun ArticleContentArea(
    navigationState: NavigationState,
    themeViewModel: com.defnf.syndicate.ui.viewmodel.ThemeViewModel? = null,
    isSidebarMode: Boolean = false,
    hasBottomNavigation: Boolean = true,
    modifier: Modifier = Modifier
) {
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
                    SettingsScreen(
                        themeViewModel = themeViewModel,
                        isSidebarMode = isSidebarMode,
                        onBackClick = { navigationState.navigateTo(TopLevelDestination.ARTICLES) }
                    )
                }
            }
            "article_detail" -> {
                navigationState.selectedArticleId?.let { articleId ->
                    ArticleDetailScreen(
                        articleId = articleId,
                        onBackClick = navigationState::closeArticle,
                        isSidebarMode = isSidebarMode
                    )
                }
            }
            else -> {
                if (isSidebarMode) {
                    // Multi-pane: list with its own top bar (title, mark all as read, settings)
                    ArticleListPane(navigationState = navigationState)
                } else {
                    // Single-pane: normal article list
                    ArticleListScreen(
                        feedId = navigationState.selectedFeedId,
                        groupId = navigationState.selectedGroupId,
                        forceAllArticles = navigationState.forceAllArticles,
                        onArticleClick = { article ->
                            navigationState.openArticle(article.id)
                        },
                        onBackClick = navigationState::backFromSelection,
                        hasBottomNavigation = hasBottomNavigation
                    )
                }
            }
        }
    }
}

/**
 * Article list pane for multi-pane layouts. Draws its own top app bar below the status bar
 * with the feed title (tap to scroll to top), mark all as read and settings actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleListPane(
    navigationState: NavigationState,
    modifier: Modifier = Modifier
) {
    val articleViewModel: ArticleListViewModel = hiltViewModel()
    val currentFeed by articleViewModel.currentFeed.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val systemBarTopPadding = WindowInsets.systemBars.asPaddingValues().calculateTopPadding()

    Box(modifier = modifier.fillMaxSize()) {
        ArticleListScreen(
            feedId = navigationState.selectedFeedId,
            groupId = navigationState.selectedGroupId,
            forceAllArticles = navigationState.forceAllArticles,
            onArticleClick = { article ->
                navigationState.openArticle(article.id)
            },
            isSidebarMode = true,
            additionalTopPadding = systemBarTopPadding + LayoutConstants.TopBarHeight,
            externalListState = listState
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = systemBarTopPadding)
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = currentFeed?.title ?: "All Articles",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        }
                    )
                },
                actions = {
                    IconButton(onClick = { articleViewModel.markAllAsRead() }) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark all as read"
                        )
                    }
                    IconButton(onClick = { navigationState.navigateTo(TopLevelDestination.SETTINGS) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        }
    }
}
