package com.defnf.syndicate.ui.layout

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
import com.defnf.syndicate.ui.screens.ArticleListScreen
import com.defnf.syndicate.ui.screens.FeedListScreen
import com.defnf.syndicate.ui.screens.SettingsScreen

/*
 * Connects the screens to NavigationState. Screens only expose the actions they are given, so the
 * actions offered here depend on what the rest of the layout already provides.
 */

@Composable
internal fun FeedsPane(
    navigationState: NavigationState,
    highlightSelection: Boolean,
    modifier: Modifier = Modifier
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
            navigationState.selectedGroupId == null,
        modifier = modifier
    )
}

@Composable
internal fun ArticleListPane(
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
        // Without a navigation bar or rail, settings are opened from the article list
        onSettingsClick = if (layoutType.hasNavigationChrome) {
            null
        } else {
            { navigationState.navigateTo(TopLevelDestination.SETTINGS) }
        }
    )
}

@Composable
internal fun SettingsPane(
    navigationState: NavigationState,
    layoutType: AppLayoutType
) {
    SettingsScreen(
        // Settings are a destination of the navigation bar or rail when there is one
        onBackClick = if (layoutType.hasNavigationChrome) {
            null
        } else {
            { navigationState.navigateTo(TopLevelDestination.ARTICLES) }
        }
    )
}

/** Placeholder for the detail pane while the article list is shown beside it and nothing is open. */
@Composable
internal fun NoArticleSelected() {
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
