package com.defnf.syndicate.ui.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.ui.navigation.NavigationState
import com.defnf.syndicate.ui.navigation.TopLevelDestination
import com.defnf.syndicate.ui.screens.ArticleDetailScreen
import com.defnf.syndicate.ui.screens.ArticleListScreen
import com.defnf.syndicate.ui.screens.FeedListScreen
import com.defnf.syndicate.ui.screens.SettingsScreen

/**
 * Shows the screen for [pane] and connects it to [navigationState], animating content changes.
 *
 * Screens only expose the actions they are given, so the actions offered here depend on what the
 * rest of the layout already provides (e.g. no back button on the article list when the feeds
 * sidebar is visible, and a settings action when there is no navigation bar or rail).
 */
@Composable
internal fun PaneHost(
    pane: Pane,
    layoutType: AppLayoutType,
    navigationState: NavigationState,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = pane.content,
        transitionSpec = { paneTransition(initialState, targetState) },
        modifier = modifier,
        label = "pane_content"
    ) { content ->
        when (content) {
            PaneContent.Feeds -> FeedsPane(
                navigationState = navigationState,
                // The sidebar's selection is shown beside it, so highlight it there
                highlightSelection = pane.role == PaneRole.SIDEBAR
            )

            PaneContent.ArticleList -> ArticleListPane(
                navigationState = navigationState,
                layoutType = layoutType
            )

            is PaneContent.ArticleDetail -> ArticleDetailScreen(
                articleId = content.articleId,
                onBackClick = navigationState::closeArticle
            )

            PaneContent.NoArticleSelected -> NoArticleSelected()

            PaneContent.Settings -> SettingsScreen(
                onBackClick = if (layoutType.hasNavigationChrome) {
                    null
                } else {
                    { navigationState.navigateTo(TopLevelDestination.ARTICLES) }
                }
            )
        }
    }
}

@Composable
private fun FeedsPane(
    navigationState: NavigationState,
    highlightSelection: Boolean
) {
    FeedListScreen(
        onFeedClick = navigationState::selectFeed,
        onGroupClick = navigationState::selectGroup,
        onAllFeedsClick = navigationState::selectAllArticles,
        onDeleteFeed = navigationState::onFeedDeleted,
        onDeleteGroup = navigationState::onGroupDeleted,
        selectedFeedId = if (highlightSelection) navigationState.selectedFeedId else null,
        selectedGroupId = if (highlightSelection) navigationState.selectedGroupId else null,
        isAllArticlesSelected = highlightSelection &&
            navigationState.selectedFeedId == null &&
            navigationState.selectedGroupId == null
    )
}

@Composable
private fun ArticleListPane(
    navigationState: NavigationState,
    layoutType: AppLayoutType
) {
    // With a sidebar the feeds list is already on screen, so there is nothing to go back to
    val canGoBack = !layoutType.hasFeedsSidebar && navigationState.hasSelection

    ArticleListScreen(
        feedId = navigationState.selectedFeedId,
        groupId = navigationState.selectedGroupId,
        forceAllArticles = navigationState.forceAllArticles,
        onArticleClick = { article -> navigationState.openArticle(article.id) },
        onBackClick = if (canGoBack) navigationState::backFromSelection else null,
        onSettingsClick = if (layoutType.hasNavigationChrome) {
            null
        } else {
            { navigationState.navigateTo(TopLevelDestination.SETTINGS) }
        }
    )
}

@Composable
private fun NoArticleSelected() {
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
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "Select an article to read",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

private fun paneTransition(initial: PaneContent, target: PaneContent): ContentTransform = when {
    // Opening an article from the list in the same pane slides it in over the list, and back out again
    initial is PaneContent.ArticleList && target is PaneContent.ArticleDetail ->
        slideInHorizontally(initialOffsetX = { fullWidth -> fullWidth }) togetherWith
            slideOutHorizontally(targetOffsetX = { fullWidth -> -fullWidth })

    initial is PaneContent.ArticleDetail && target is PaneContent.ArticleList ->
        slideInHorizontally(initialOffsetX = { fullWidth -> -fullWidth }) togetherWith
            slideOutHorizontally(targetOffsetX = { fullWidth -> fullWidth })

    // Picking another article from the list beside the detail pane swaps it immediately
    initial is PaneContent.ArticleDetail && target is PaneContent.ArticleDetail ->
        EnterTransition.None togetherWith ExitTransition.None

    else -> fadeIn() togetherWith fadeOut()
}
