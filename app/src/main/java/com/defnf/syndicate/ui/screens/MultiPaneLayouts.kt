package com.defnf.syndicate.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.components.ArticleContentArea
import com.defnf.syndicate.ui.components.ArticleListPane
import com.defnf.syndicate.ui.components.FeedsSidebar
import com.defnf.syndicate.ui.navigation.NavigationState
import com.defnf.syndicate.ui.navigation.TopLevelDestination
import com.defnf.syndicate.ui.viewmodel.ThemeViewModel

/**
 * Expanded width layout: persistent feeds sidebar + a content pane that switches
 * between the article list, article detail and settings.
 */
@Composable
fun TwoPaneLayout(
    navigationState: NavigationState,
    themeViewModel: ThemeViewModel
) {
    Row(modifier = Modifier.fillMaxSize()) {
        FeedsSidebar(navigationState = navigationState)

        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.background
        ) {
            ArticleContentArea(
                navigationState = navigationState,
                themeViewModel = themeViewModel,
                isSidebarMode = true
            )
        }
    }
}

/**
 * Large width layout: feeds sidebar + article list + article detail side by side.
 * Settings replace the list and detail panes while open.
 */
@Composable
fun ThreePaneLayout(
    navigationState: NavigationState,
    themeViewModel: ThemeViewModel
) {
    Row(modifier = Modifier.fillMaxSize()) {
        FeedsSidebar(navigationState = navigationState)

        if (navigationState.showSettings) {
            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.background
            ) {
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    isSidebarMode = true,
                    onBackClick = { navigationState.navigateTo(TopLevelDestination.ARTICLES) }
                )
            }
        } else {
            Surface(
                modifier = Modifier
                    .width(LayoutConstants.ArticleListPaneWidth)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.background
            ) {
                ArticleListPane(navigationState = navigationState)
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.background
            ) {
                val articleId = navigationState.selectedArticleId
                if (articleId != null) {
                    key(articleId) {
                        ArticleDetailScreen(
                            articleId = articleId,
                            onBackClick = navigationState::closeArticle,
                            isSidebarMode = true
                        )
                    }
                } else {
                    NoArticleSelected()
                }
            }
        }
    }
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
