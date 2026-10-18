package com.defnf.syndicate.ui.layout

import com.defnf.syndicate.ui.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class PaneLayoutTest {

    private val singlePaneLayouts = listOf(AppLayoutType.BOTTOM_NAVIGATION, AppLayoutType.NAVIGATION_RAIL)

    @Test
    fun `single pane shows the current destination`() {
        singlePaneLayouts.forEach { layoutType ->
            assertEquals(
                listOf(Pane(PaneRole.MAIN, PaneContent.ArticleList)),
                resolvePanes(layoutType, TopLevelDestination.ARTICLES, openArticleId = null)
            )
            assertEquals(
                listOf(Pane(PaneRole.MAIN, PaneContent.Feeds)),
                resolvePanes(layoutType, TopLevelDestination.FEEDS, openArticleId = null)
            )
            assertEquals(
                listOf(Pane(PaneRole.MAIN, PaneContent.Settings)),
                resolvePanes(layoutType, TopLevelDestination.SETTINGS, openArticleId = null)
            )
        }
    }

    @Test
    fun `single pane replaces the article list with the open article`() {
        singlePaneLayouts.forEach { layoutType ->
            assertEquals(
                listOf(Pane(PaneRole.MAIN, PaneContent.ArticleDetail("a1"))),
                resolvePanes(layoutType, TopLevelDestination.ARTICLES, openArticleId = "a1")
            )
        }
    }

    @Test
    fun `two pane shows the feeds sidebar beside the article content`() {
        assertEquals(
            listOf(
                Pane(PaneRole.SIDEBAR, PaneContent.Feeds),
                Pane(PaneRole.MAIN, PaneContent.ArticleList)
            ),
            resolvePanes(AppLayoutType.TWO_PANE, TopLevelDestination.ARTICLES, openArticleId = null)
        )
        assertEquals(
            listOf(
                Pane(PaneRole.SIDEBAR, PaneContent.Feeds),
                Pane(PaneRole.MAIN, PaneContent.ArticleDetail("a1"))
            ),
            resolvePanes(AppLayoutType.TWO_PANE, TopLevelDestination.ARTICLES, openArticleId = "a1")
        )
        assertEquals(
            listOf(
                Pane(PaneRole.SIDEBAR, PaneContent.Feeds),
                Pane(PaneRole.MAIN, PaneContent.Settings)
            ),
            resolvePanes(AppLayoutType.TWO_PANE, TopLevelDestination.SETTINGS, openArticleId = null)
        )
    }

    @Test
    fun `three pane keeps the article list beside the detail pane`() {
        assertEquals(
            listOf(
                Pane(PaneRole.SIDEBAR, PaneContent.Feeds),
                Pane(PaneRole.LIST, PaneContent.ArticleList),
                Pane(PaneRole.MAIN, PaneContent.NoArticleSelected)
            ),
            resolvePanes(AppLayoutType.THREE_PANE, TopLevelDestination.ARTICLES, openArticleId = null)
        )
        assertEquals(
            listOf(
                Pane(PaneRole.SIDEBAR, PaneContent.Feeds),
                Pane(PaneRole.LIST, PaneContent.ArticleList),
                Pane(PaneRole.MAIN, PaneContent.ArticleDetail("a1"))
            ),
            resolvePanes(AppLayoutType.THREE_PANE, TopLevelDestination.ARTICLES, openArticleId = "a1")
        )
    }

    @Test
    fun `three pane settings replace the list and detail panes`() {
        assertEquals(
            listOf(
                Pane(PaneRole.SIDEBAR, PaneContent.Feeds),
                Pane(PaneRole.MAIN, PaneContent.Settings)
            ),
            resolvePanes(AppLayoutType.THREE_PANE, TopLevelDestination.SETTINGS, openArticleId = null)
        )
    }

    @Test
    fun `feeds destination falls back to articles when the sidebar shows feeds`() {
        // e.g. the window was widened while the feeds tab was selected
        assertEquals(
            listOf(
                Pane(PaneRole.SIDEBAR, PaneContent.Feeds),
                Pane(PaneRole.MAIN, PaneContent.ArticleList)
            ),
            resolvePanes(AppLayoutType.TWO_PANE, TopLevelDestination.FEEDS, openArticleId = null)
        )
        assertEquals(
            listOf(
                Pane(PaneRole.SIDEBAR, PaneContent.Feeds),
                Pane(PaneRole.LIST, PaneContent.ArticleList),
                Pane(PaneRole.MAIN, PaneContent.NoArticleSelected)
            ),
            resolvePanes(AppLayoutType.THREE_PANE, TopLevelDestination.FEEDS, openArticleId = null)
        )
    }

    @Test
    fun `main pane is always present and last`() {
        AppLayoutType.entries.forEach { layoutType ->
            TopLevelDestination.entries.forEach { destination ->
                listOf(null, "a1").forEach { articleId ->
                    val panes = resolvePanes(layoutType, destination, articleId)
                    assertEquals(PaneRole.MAIN, panes.last().role)
                    assertEquals(1, panes.count { it.role == PaneRole.MAIN })
                    assertEquals(panes.sortedBy { it.role }, panes)
                }
            }
        }
    }
}
