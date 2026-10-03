package com.defnf.syndicate.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.defnf.syndicate.ui.viewmodel.FeedListViewModel

/**
 * Remembers the app-wide navigation state.
 *
 * The state is hoisted above the layout switch and saved across configuration changes, so the
 * current feed/group/article selection survives rotation, folding/unfolding, split-screen resizing
 * and switching between single-pane and multi-pane layouts.
 */
@Composable
fun rememberNavigationState(): NavigationState {
    val navigationState = rememberSaveable(saver = NavigationState.Saver) { NavigationState() }

    // Get FeedListViewModel to access default group
    val feedListViewModel: FeedListViewModel = hiltViewModel()

    // Load default group on first launch if no specific selection was made
    LaunchedEffect(navigationState) {
        if (!navigationState.defaultSelectionResolved) {
            val defaultGroup = feedListViewModel.getDefaultGroup()
            android.util.Log.d("NavigationState", "Default group found: ${defaultGroup?.name} (id: ${defaultGroup?.id})")
            navigationState.applyDefaultGroup(defaultGroup?.id)
        }
    }

    return navigationState
}

/**
 * Shared navigation state for article and feed selection across all layouts
 * (bottom navigation, navigation rail, two-pane and three-pane).
 */
@Stable
class NavigationState internal constructor(
    currentDestination: TopLevelDestination = TopLevelDestination.ARTICLES,
    selectedFeedId: Long? = null,
    selectedGroupId: Long? = null,
    forceAllArticles: Boolean = false,
    selectedArticleId: String? = null,
    returnToFeedsOnBack: Boolean = false,
    defaultSelectionResolved: Boolean = false
) {
    var currentDestination by mutableStateOf(currentDestination)
        private set
    var selectedFeedId by mutableStateOf(selectedFeedId)
        private set
    var selectedGroupId by mutableStateOf(selectedGroupId)
        private set
    var forceAllArticles by mutableStateOf(forceAllArticles)
        private set
    var selectedArticleId by mutableStateOf(selectedArticleId)
        private set

    /** Whether going back from the current selection returns to the feeds list (single-pane only). */
    private var returnToFeedsOnBack by mutableStateOf(returnToFeedsOnBack)

    /** Whether the default group has been looked up and applied once. */
    var defaultSelectionResolved by mutableStateOf(defaultSelectionResolved)
        private set

    val hasSelection: Boolean
        get() = selectedFeedId != null || selectedGroupId != null || forceAllArticles

    val showSettings: Boolean
        get() = currentDestination == TopLevelDestination.SETTINGS

    fun selectFeed(feedId: Long) = select(feedId = feedId)

    fun selectGroup(groupId: Long) = select(groupId = groupId)

    fun selectAllArticles() = select(allArticles = true)

    fun openArticle(articleId: String) {
        selectedArticleId = articleId
    }

    fun closeArticle() {
        selectedArticleId = null
    }

    fun openArticleFromNotification(feedId: Long, articleId: String) {
        select(feedId = feedId)
        returnToFeedsOnBack = false
        selectedArticleId = articleId
    }

    fun openGroupFromNotification(groupId: Long) {
        select(groupId = groupId)
        returnToFeedsOnBack = false
    }

    fun navigateTo(destination: TopLevelDestination) {
        if (currentDestination == destination) return
        currentDestination = destination
        selectedArticleId = null
        returnToFeedsOnBack = false
    }

    /** Handles a tap on a bottom navigation / rail item. The Articles item always resets to the default view. */
    fun onTopLevelDestinationClick(destination: TopLevelDestination) {
        navigateTo(destination)
        if (destination == TopLevelDestination.ARTICLES) {
            select(allArticles = true)
            returnToFeedsOnBack = false
        }
    }

    fun onFeedDeleted(feedId: Long) {
        if (selectedFeedId == feedId) {
            selectedFeedId = null
            selectedArticleId = null
        }
    }

    fun onGroupDeleted(groupId: Long) {
        if (selectedGroupId == groupId) {
            selectedGroupId = null
            selectedArticleId = null
        }
    }

    /** Clears the feed/group selection, returning to the feeds list if that is where it was made. */
    fun backFromSelection() {
        val returnToFeeds = returnToFeedsOnBack
        selectedFeedId = null
        selectedGroupId = null
        forceAllArticles = false
        selectedArticleId = null
        returnToFeedsOnBack = false
        if (returnToFeeds) {
            currentDestination = TopLevelDestination.FEEDS
        }
    }

    fun canNavigateBack(layoutType: AppLayoutType): Boolean = when {
        selectedArticleId != null -> true
        // Feeds are always visible in the sidebar, so only settings can be backed out of
        layoutType.isMultiPane -> currentDestination == TopLevelDestination.SETTINGS
        else -> currentDestination != TopLevelDestination.ARTICLES || hasSelection
    }

    fun navigateBack(layoutType: AppLayoutType) {
        when {
            selectedArticleId != null -> closeArticle()
            layoutType.isMultiPane -> navigateTo(TopLevelDestination.ARTICLES)
            currentDestination != TopLevelDestination.ARTICLES -> navigateTo(TopLevelDestination.ARTICLES)
            hasSelection -> backFromSelection()
        }
    }

    internal fun applyDefaultGroup(groupId: Long?) {
        if (defaultSelectionResolved) return
        defaultSelectionResolved = true
        if (groupId != null && !hasSelection) {
            selectedGroupId = groupId
            android.util.Log.d("NavigationState", "Auto-selected default group: $groupId")
        }
    }

    private fun select(feedId: Long? = null, groupId: Long? = null, allArticles: Boolean = false) {
        returnToFeedsOnBack = currentDestination == TopLevelDestination.FEEDS
        selectedFeedId = feedId
        selectedGroupId = groupId
        forceAllArticles = allArticles
        selectedArticleId = null
        currentDestination = TopLevelDestination.ARTICLES
    }

    companion object {
        val Saver: Saver<NavigationState, Any> = listSaver<NavigationState, Any?>(
            save = { state ->
                listOf(
                    state.currentDestination.name,
                    state.selectedFeedId,
                    state.selectedGroupId,
                    state.forceAllArticles,
                    state.selectedArticleId,
                    state.returnToFeedsOnBack,
                    state.defaultSelectionResolved
                )
            },
            restore = { saved ->
                NavigationState(
                    currentDestination = TopLevelDestination.valueOf(saved[0] as String),
                    selectedFeedId = saved[1] as Long?,
                    selectedGroupId = saved[2] as Long?,
                    forceAllArticles = saved[3] as Boolean,
                    selectedArticleId = saved[4] as String?,
                    returnToFeedsOnBack = saved[5] as Boolean,
                    defaultSelectionResolved = saved[6] as Boolean
                )
            }
        )
    }
}
