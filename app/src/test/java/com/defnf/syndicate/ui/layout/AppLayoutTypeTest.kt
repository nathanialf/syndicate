package com.defnf.syndicate.ui.layout

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppLayoutTypeTest {

    @Test
    fun `layout follows the material window size class breakpoints`() {
        assertEquals(AppLayoutType.BOTTOM_NAVIGATION, AppLayoutType.fromWindowWidth(360.dp))
        assertEquals(AppLayoutType.BOTTOM_NAVIGATION, AppLayoutType.fromWindowWidth(599.dp))
        assertEquals(AppLayoutType.NAVIGATION_RAIL, AppLayoutType.fromWindowWidth(600.dp))
        assertEquals(AppLayoutType.NAVIGATION_RAIL, AppLayoutType.fromWindowWidth(839.dp))
        assertEquals(AppLayoutType.TWO_PANE, AppLayoutType.fromWindowWidth(840.dp))
        assertEquals(AppLayoutType.TWO_PANE, AppLayoutType.fromWindowWidth(1199.dp))
        assertEquals(AppLayoutType.THREE_PANE, AppLayoutType.fromWindowWidth(1200.dp))
    }

    @Test
    fun `layouts with a feeds sidebar have no navigation chrome`() {
        // Without a bar or rail, the sidebar is the only way to reach the feeds
        AppLayoutType.entries.forEach { layoutType ->
            assertEquals(layoutType.hasFeedsSidebar, !layoutType.hasNavigationChrome)
        }
    }

    @Test
    fun `article list is only kept beside the detail when there is a sidebar`() {
        AppLayoutType.entries
            .filter { it.hasArticleListBesideDetail }
            .forEach { layoutType -> assertEquals(true, layoutType.hasFeedsSidebar) }
    }
}
