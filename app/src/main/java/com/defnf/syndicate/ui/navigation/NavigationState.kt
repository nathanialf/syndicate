package com.defnf.syndicate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel

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
    var previousScreen by remember { mutableStateOf<String?>(null) }
    
    // Get FeedListViewModel to access default group
    val feedListViewModel: com.defnf.syndicate.ui.viewmodel.FeedListViewModel = hiltViewModel()
    
    // Load default group on startup if no specific selection
    LaunchedEffect(Unit) {
        if (selectedFeedId == null && selectedGroupId == null && !forceAllArticles) {
            val defaultGroup = feedListViewModel.getDefaultGroup()
            android.util.Log.d("NavigationState", "Default group found: ${defaultGroup?.name} (id: ${defaultGroup?.id})")
            if (defaultGroup != null) {
                selectedGroupId = defaultGroup.id
                android.util.Log.d("NavigationState", "Auto-selected default group: ${defaultGroup.id}")
            } else {
                android.util.Log.d("NavigationState", "No default group found")
            }
        }
    }

    return NavigationState(
        selectedFeedId = selectedFeedId,
        selectedGroupId = selectedGroupId,
        forceAllArticles = forceAllArticles,
        selectedArticleId = selectedArticleId,
        currentScreen = currentScreen,
        showSettings = showSettings,
        previousScreen = previousScreen,
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
            if (currentScreen != screen) {
                previousScreen = currentScreen
                currentScreen = screen
                selectedArticleId = null
                // Don't reset feed/group selection when navigating to articles
                // Only reset when navigating away from articles
                if (screen != "articles") {
                    selectedFeedId = null
                    selectedGroupId = null
                    forceAllArticles = false
                }
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
            // Navigate back to the previous screen (feeds) and clear selection
            if (previousScreen != null) {
                currentScreen = previousScreen!!
                previousScreen = null
            }
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
        },
        onNavigateToDefault = {
            // Navigate to default group or all articles if no default group exists
            selectedFeedId = null
            selectedGroupId = null
            forceAllArticles = true // Start with all articles as default
            showSettings = false
            selectedArticleId = null
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
    val previousScreen: String?,
    val onFeedSelected: (Long) -> Unit,
    val onGroupSelected: (Long) -> Unit,
    val onAllFeedsSelected: () -> Unit,
    val onArticleSelected: (String) -> Unit,
    val onScreenChanged: (String) -> Unit,
    val onShowSettings: (Boolean) -> Unit,
    val onBackFromArticle: () -> Unit,
    val onBackFromSelection: () -> Unit,
    val onFeedDeleted: (Long) -> Unit,
    val onGroupDeleted: (Long) -> Unit,
    val onNavigateToDefault: () -> Unit
)