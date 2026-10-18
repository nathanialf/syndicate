package com.defnf.syndicate.ui.navigation

import androidx.compose.runtime.saveable.SaverScope
import com.defnf.syndicate.ui.layout.AppLayoutType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateTest {

    private val articlePane = FakeArticlePaneNavigator()
    private val state = NavigationState(articlePane)

    private val singlePaneLayouts = listOf(AppLayoutType.BOTTOM_NAVIGATION, AppLayoutType.NAVIGATION_RAIL)
    private val sidebarLayouts = listOf(AppLayoutType.TWO_PANE, AppLayoutType.THREE_PANE)

    @Test
    fun `starts on the articles with nothing selected`() {
        assertEquals(TopLevelDestination.ARTICLES, state.currentDestination)
        assertFalse(state.hasSelection)
        assertNull(state.openArticleId)
        AppLayoutType.entries.forEach { assertFalse(state.canNavigateBack(it)) }
    }

    @Test
    fun `selecting a feed shows its articles and closes the open article`() {
        state.openArticle("a1")

        state.selectFeed(1)

        assertEquals(TopLevelDestination.ARTICLES, state.currentDestination)
        assertEquals(1L, state.selectedFeedId)
        assertNull(state.selectedGroupId)
        assertNull(state.openArticleId)
    }

    @Test
    fun `selecting a group replaces the selected feed`() {
        state.selectFeed(1)

        state.selectGroup(7)

        assertNull(state.selectedFeedId)
        assertEquals(7L, state.selectedGroupId)
    }

    @Test
    fun `back from a selection made on the feeds tab returns to the feeds tab`() {
        state.navigateTo(TopLevelDestination.FEEDS)
        state.selectFeed(1)

        singlePaneLayouts.forEach { assertTrue(state.canNavigateBack(it)) }
        state.navigateBack(AppLayoutType.BOTTOM_NAVIGATION)

        assertEquals(TopLevelDestination.FEEDS, state.currentDestination)
        assertFalse(state.hasSelection)
    }

    @Test
    fun `back from a selection made elsewhere stays on the articles`() {
        state.selectFeed(1)

        state.navigateBack(AppLayoutType.NAVIGATION_RAIL)

        assertEquals(TopLevelDestination.ARTICLES, state.currentDestination)
        assertFalse(state.hasSelection)
    }

    @Test
    fun `with a sidebar only settings can be backed out of`() {
        state.selectFeed(1)
        sidebarLayouts.forEach { assertFalse(state.canNavigateBack(it)) }

        state.navigateTo(TopLevelDestination.SETTINGS)
        sidebarLayouts.forEach { assertTrue(state.canNavigateBack(it)) }
        state.navigateBack(AppLayoutType.THREE_PANE)

        assertEquals(TopLevelDestination.ARTICLES, state.currentDestination)
        // The selection is still shown beside the sidebar
        assertEquals(1L, state.selectedFeedId)
    }

    @Test
    fun `back from another tab returns to the articles`() {
        state.navigateTo(TopLevelDestination.SETTINGS)

        singlePaneLayouts.forEach { assertTrue(state.canNavigateBack(it)) }
        state.navigateBack(AppLayoutType.BOTTOM_NAVIGATION)

        assertEquals(TopLevelDestination.ARTICLES, state.currentDestination)
    }

    @Test
    fun `the open article is left to the article panes on back`() {
        state.openArticle("a1")

        // Nothing else to go back from: the panes' own (predictive) back handler closes the article
        AppLayoutType.entries.forEach { assertFalse(state.canNavigateBack(it)) }
    }

    @Test
    fun `changing destination closes the open article`() {
        state.openArticle("a1")

        state.navigateTo(TopLevelDestination.SETTINGS)

        assertNull(state.openArticleId)
    }

    @Test
    fun `navigating to the current destination keeps the open article`() {
        state.openArticle("a1")

        state.navigateTo(TopLevelDestination.ARTICLES)

        assertEquals("a1", state.openArticleId)
    }

    @Test
    fun `tapping the articles destination resets to all articles`() {
        state.selectFeed(1)
        state.navigateTo(TopLevelDestination.FEEDS)

        state.onTopLevelDestinationClick(TopLevelDestination.ARTICLES)

        assertEquals(TopLevelDestination.ARTICLES, state.currentDestination)
        assertTrue(state.forceAllArticles)
        assertNull(state.selectedFeedId)
        // All articles were picked from the navigation, not from the feeds tab
        state.navigateBack(AppLayoutType.BOTTOM_NAVIGATION)
        assertEquals(TopLevelDestination.ARTICLES, state.currentDestination)
    }

    @Test
    fun `deleting the selected feed clears the selection and closes its article`() {
        state.selectFeed(1)
        state.openArticle("a1")

        state.onFeedDeleted(2)
        assertEquals(1L, state.selectedFeedId)
        assertEquals("a1", state.openArticleId)

        state.onFeedDeleted(1)
        assertNull(state.selectedFeedId)
        assertNull(state.openArticleId)
    }

    @Test
    fun `deleting the selected group clears the selection and closes its article`() {
        state.selectGroup(7)
        state.openArticle("a1")

        state.onGroupDeleted(7)

        assertNull(state.selectedGroupId)
        assertNull(state.openArticleId)
    }

    @Test
    fun `a notification opens its article in its feed`() {
        state.navigateTo(TopLevelDestination.FEEDS)

        state.openArticleFromNotification(feedId = 1, articleId = "a1")

        assertEquals(TopLevelDestination.ARTICLES, state.currentDestination)
        assertEquals(1L, state.selectedFeedId)
        assertEquals("a1", state.openArticleId)
        // Going back from a notification does not return to the feeds tab
        state.closeArticle()
        state.navigateBack(AppLayoutType.BOTTOM_NAVIGATION)
        assertEquals(TopLevelDestination.ARTICLES, state.currentDestination)
    }

    @Test
    fun `the default group is applied once and never overrides a selection`() {
        state.selectFeed(1)
        state.applyDefaultGroup(7)
        assertNull(state.selectedGroupId)

        val fresh = NavigationState(FakeArticlePaneNavigator())
        fresh.applyDefaultGroup(7)
        fresh.applyDefaultGroup(8)
        assertEquals(7L, fresh.selectedGroupId)
        assertTrue(fresh.defaultSelectionResolved)
    }

    @Test
    fun `saved state restores everything but the open article`() {
        state.navigateTo(TopLevelDestination.FEEDS)
        state.selectGroup(7)
        state.applyDefaultGroup(null)
        state.openArticle("a1")

        val saver = NavigationState.saver(articlePane)
        val saved = with(saver) { AnySaverScope.save(state) }!!
        val restored = saver.restore(saved)!!

        assertEquals(TopLevelDestination.ARTICLES, restored.currentDestination)
        assertEquals(7L, restored.selectedGroupId)
        assertTrue(restored.defaultSelectionResolved)
        // The article pane saves its own state; the restored state reads it from there
        assertEquals("a1", restored.openArticleId)
        // The selection was made on the feeds tab, so back still returns there
        restored.navigateBack(AppLayoutType.BOTTOM_NAVIGATION)
        assertEquals(TopLevelDestination.FEEDS, restored.currentDestination)
    }

    private object AnySaverScope : SaverScope {
        override fun canBeSaved(value: Any) = true
    }

    private class FakeArticlePaneNavigator : ArticlePaneNavigator {
        override var openArticleId: String? = null
            private set

        override fun openArticle(articleId: String) {
            openArticleId = articleId
        }

        override fun closeArticle() {
            openArticleId = null
        }
    }
}
