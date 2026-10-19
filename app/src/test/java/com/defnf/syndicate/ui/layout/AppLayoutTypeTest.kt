package com.defnf.syndicate.ui.layout

import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.window.core.layout.WindowSizeClass
import com.defnf.syndicate.ui.navigation.TopLevelDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLayoutTypeTest {

    private fun layoutForWidth(widthDp: Int) =
        AppLayoutType.fromWindowSizeClass(WindowSizeClass(minWidthDp = widthDp, minHeightDp = 0))

    @Test
    fun `layout follows the material window size class breakpoints`() {
        assertEquals(AppLayoutType.BOTTOM_NAVIGATION, layoutForWidth(0))
        assertEquals(AppLayoutType.NAVIGATION_RAIL, layoutForWidth(600))
        assertEquals(AppLayoutType.TWO_PANE, layoutForWidth(840))
        assertEquals(AppLayoutType.THREE_PANE, layoutForWidth(1200))
        assertEquals(AppLayoutType.THREE_PANE, layoutForWidth(1600))
    }

    @Test
    fun `phones and medium windows reach destinations from a bar or rail`() {
        assertEquals(NavigationSuiteType.ShortNavigationBarCompact, AppLayoutType.BOTTOM_NAVIGATION.navigationSuiteType)
        assertEquals(NavigationSuiteType.WideNavigationRailCollapsed, AppLayoutType.NAVIGATION_RAIL.navigationSuiteType)
        assertEquals(NavigationSuiteType.None, AppLayoutType.TWO_PANE.navigationSuiteType)
        assertEquals(NavigationSuiteType.None, AppLayoutType.THREE_PANE.navigationSuiteType)
    }

    @Test
    fun `layouts with a feeds sidebar have no navigation chrome`() {
        // Without a bar or rail, the sidebar is the only way to reach the feeds
        AppLayoutType.entries.forEach { layoutType ->
            assertEquals(layoutType.hasFeedsSidebar, !layoutType.hasNavigationChrome)
        }
    }

    @Test
    fun `article list is only kept beside the article when there is a sidebar`() {
        AppLayoutType.entries
            .filter { it.hasArticleListBesideDetail }
            .forEach { layoutType -> assertTrue(layoutType.hasFeedsSidebar) }
    }

    @Test
    fun `only the bottom bar hides while reading`() {
        assertTrue(AppLayoutType.BOTTOM_NAVIGATION.hidesNavigationWhileReading)
        assertFalse(AppLayoutType.NAVIGATION_RAIL.hidesNavigationWhileReading)
        assertFalse(AppLayoutType.TWO_PANE.hidesNavigationWhileReading)
        assertFalse(AppLayoutType.THREE_PANE.hidesNavigationWhileReading)
    }

    @Test
    fun `main content follows the destination`() {
        AppLayoutType.entries.forEach { layoutType ->
            assertEquals(MainContent.ARTICLES, layoutType.mainContentFor(TopLevelDestination.ARTICLES))
            assertEquals(MainContent.SETTINGS, layoutType.mainContentFor(TopLevelDestination.SETTINGS))
        }
    }

    @Test
    fun `feeds are a destination of their own only without a sidebar`() {
        assertEquals(MainContent.FEEDS, AppLayoutType.BOTTOM_NAVIGATION.mainContentFor(TopLevelDestination.FEEDS))
        assertEquals(MainContent.FEEDS, AppLayoutType.NAVIGATION_RAIL.mainContentFor(TopLevelDestination.FEEDS))
        // e.g. the window was widened while the feeds tab was selected
        assertEquals(MainContent.ARTICLES, AppLayoutType.TWO_PANE.mainContentFor(TopLevelDestination.FEEDS))
        assertEquals(MainContent.ARTICLES, AppLayoutType.THREE_PANE.mainContentFor(TopLevelDestination.FEEDS))
    }
}
