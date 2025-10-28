package com.syndicate.rssreader.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Shared navigation state for article and feed selection across both narrow and wide screen layouts
 */
@Composable
fun rememberNavigationState(): NavigationState {
    var selectedFeedId by remember { mutableStateOf<Long?>(null) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var forceAllArticles by remember { mutableStateOf(false) }
    var selectedArticleId by remember { mutableStateOf<String?>(null) }
    var currentScreen by remember { mutableStateOf("articles") }
    var showSettings by remember { mutableStateOf(false) }

    return NavigationState(
        selectedFeedId = selectedFeedId,
        selectedGroupId = selectedGroupId,
        forceAllArticles = forceAllArticles,
        selectedArticleId = selectedArticleId,
        currentScreen = currentScreen,
        showSettings = showSettings,
        onFeedSelected = { feedId ->
            selectedFeedId = feedId
            selectedGroupId = null
            forceAllArticles = false
            showSettings = false
            selectedArticleId = null
        },
        onGroupSelected = { groupId ->
            selectedGroupId = groupId
            selectedFeedId = null
            forceAllArticles = false
            showSettings = false
            selectedArticleId = null
        },
        onAllFeedsSelected = {
            selectedFeedId = null
            selectedGroupId = null
            forceAllArticles = true
            showSettings = false
            selectedArticleId = null
        },
        onArticleSelected = { articleId ->
            selectedArticleId = articleId
        },
        onScreenChanged = { screen ->
            currentScreen = screen
            selectedArticleId = null
            // Don't reset feed/group selection when navigating to articles
            // Only reset when navigating away from articles
            if (screen != "articles") {
                selectedFeedId = null
                selectedGroupId = null
                forceAllArticles = false
            }
        },
        onShowSettings = { show ->
            showSettings = show
            if (show) selectedArticleId = null
        },
        onBackFromArticle = {
            selectedArticleId = null
        },
        onBackFromSelection = {
            selectedFeedId = null
            selectedGroupId = null
            forceAllArticles = false
        },
        onFeedDeleted = { deletedFeedId ->
            if (selectedFeedId == deletedFeedId) {
                selectedFeedId = null
            }
            showSettings = false
        },
        onGroupDeleted = { deletedGroupId ->
            if (selectedGroupId == deletedGroupId) {
                selectedGroupId = null
            }
            showSettings = false
        }
    )
}

data class NavigationState(
    val selectedFeedId: Long?,
    val selectedGroupId: Long?,
    val forceAllArticles: Boolean,
    val selectedArticleId: String?,
    val currentScreen: String,
    val showSettings: Boolean,
    val onFeedSelected: (Long) -> Unit,
    val onGroupSelected: (Long) -> Unit,
    val onAllFeedsSelected: () -> Unit,
    val onArticleSelected: (String) -> Unit,
    val onScreenChanged: (String) -> Unit,
    val onShowSettings: (Boolean) -> Unit,
    val onBackFromArticle: () -> Unit,
    val onBackFromSelection: () -> Unit,
    val onFeedDeleted: (Long) -> Unit,
    val onGroupDeleted: (Long) -> Unit
)