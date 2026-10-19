package com.defnf.syndicate.ui.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import com.defnf.syndicate.ui.layout.AppLayoutType

/**
 * Navigation state shared by all layouts: the top-level destination and which feed or group the
 * article list shows. The open article is delegated to [articlePane] (see [ArticlePaneNavigator]).
 *
 * Hoisted above the layout and saved with [saver], so the selection survives rotation,
 * folding/unfolding, split-screen resizing and switching between single-pane and multi-pane layouts.
 */
@Stable
class NavigationState internal constructor(
    private val articlePane: ArticlePaneNavigator,
    currentDestination: TopLevelDestination = TopLevelDestination.ARTICLES,
    selectedFeedId: Long? = null,
    selectedGroupId: Long? = null,
    forceAllArticles: Boolean = false,
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

    /** Whether going back from the current selection returns to the feeds list (single-pane only). */
    private var returnToFeedsOnBack by mutableStateOf(returnToFeedsOnBack)

    /** Whether the default group has been looked up and applied once. */
    var defaultSelectionResolved by mutableStateOf(defaultSelectionResolved)
        private set

    val openArticleId: String?
        get() = articlePane.openArticleId

    val hasSelection: Boolean
        get() = selectedFeedId != null || selectedGroupId != null || forceAllArticles

    fun selectFeed(feedId: Long) = select(feedId = feedId)

    fun selectGroup(groupId: Long) = select(groupId = groupId)

    fun selectAllArticles() = select(allArticles = true)

    fun openArticle(articleId: String) {
        // Articles are only shown in the articles destination. With a sidebar the article list is
        // also on screen while the feeds tab is selected (e.g. the window was widened on it).
        currentDestination = TopLevelDestination.ARTICLES
        articlePane.openArticle(articleId)
    }

    fun closeArticle() = articlePane.closeArticle()

    fun openArticleFromNotification(feedId: Long, articleId: String) {
        select(feedId = feedId)
        returnToFeedsOnBack = false
        openArticle(articleId)
    }

    fun openFeedFromNotification(feedId: Long) {
        select(feedId = feedId)
        returnToFeedsOnBack = false
    }

    fun openGroupFromNotification(groupId: Long) {
        select(groupId = groupId)
        returnToFeedsOnBack = false
    }

    fun navigateTo(destination: TopLevelDestination) {
        if (currentDestination == destination) return
        currentDestination = destination
        closeArticle()
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
            closeArticle()
        }
    }

    fun onGroupDeleted(groupId: Long) {
        if (selectedGroupId == groupId) {
            selectedGroupId = null
            closeArticle()
        }
    }

    /** Clears the feed/group selection, returning to the feeds list if that is where it was made. */
    fun backFromSelection() {
        val returnToFeeds = returnToFeedsOnBack
        selectedFeedId = null
        selectedGroupId = null
        forceAllArticles = false
        closeArticle()
        returnToFeedsOnBack = false
        if (returnToFeeds) {
            currentDestination = TopLevelDestination.FEEDS
        }
    }

    /**
     * Whether system back has anything to do in [layoutType]. Closing the open article is not
     * included: the article panes handle that themselves, with a predictive back animation.
     */
    fun canNavigateBack(layoutType: AppLayoutType): Boolean = when {
        // Feeds are always visible in the sidebar, so only settings can be backed out of
        layoutType.hasFeedsSidebar -> currentDestination == TopLevelDestination.SETTINGS
        else -> currentDestination != TopLevelDestination.ARTICLES || hasSelection
    }

    fun navigateBack(layoutType: AppLayoutType) {
        when {
            layoutType.hasFeedsSidebar -> navigateTo(TopLevelDestination.ARTICLES)
            currentDestination != TopLevelDestination.ARTICLES -> navigateTo(TopLevelDestination.ARTICLES)
            hasSelection -> backFromSelection()
        }
    }

    internal fun applyDefaultGroup(groupId: Long?) {
        if (defaultSelectionResolved) return
        defaultSelectionResolved = true
        if (groupId != null && !hasSelection) {
            selectedGroupId = groupId
        }
    }

    private fun select(feedId: Long? = null, groupId: Long? = null, allArticles: Boolean = false) {
        returnToFeedsOnBack = currentDestination == TopLevelDestination.FEEDS
        selectedFeedId = feedId
        selectedGroupId = groupId
        forceAllArticles = allArticles
        closeArticle()
        currentDestination = TopLevelDestination.ARTICLES
    }

    companion object {
        /** Saves everything except the open article, which [articlePane] saves itself. */
        fun saver(articlePane: ArticlePaneNavigator): Saver<NavigationState, Any> = listSaver<NavigationState, Any?>(
            save = { state ->
                listOf(
                    state.currentDestination.name,
                    state.selectedFeedId,
                    state.selectedGroupId,
                    state.forceAllArticles,
                    state.returnToFeedsOnBack,
                    state.defaultSelectionResolved
                )
            },
            restore = { saved ->
                NavigationState(
                    articlePane = articlePane,
                    currentDestination = TopLevelDestination.valueOf(saved[0] as String),
                    selectedFeedId = saved[1] as Long?,
                    selectedGroupId = saved[2] as Long?,
                    forceAllArticles = saved[3] as Boolean,
                    returnToFeedsOnBack = saved[4] as Boolean,
                    defaultSelectionResolved = saved[5] as Boolean
                )
            }
        )
    }
}
