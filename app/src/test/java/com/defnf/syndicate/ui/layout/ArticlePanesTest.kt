package com.defnf.syndicate.ui.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.MutableThreePaneScaffoldState
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldDestinationItem
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldRole
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.ui.common.LayoutConstants
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
class ArticlePanesTest {

    @Test
    fun `article panes sit side by side only when the list stays beside the article`() {
        assertEquals(1, articlePaneDirective(windowDirective(), AppLayoutType.BOTTOM_NAVIGATION).maxHorizontalPartitions)
        assertEquals(1, articlePaneDirective(windowDirective(), AppLayoutType.NAVIGATION_RAIL).maxHorizontalPartitions)
        assertEquals(1, articlePaneDirective(windowDirective(), AppLayoutType.TWO_PANE).maxHorizontalPartitions)
        assertEquals(2, articlePaneDirective(windowDirective(), AppLayoutType.THREE_PANE).maxHorizontalPartitions)
    }

    @Test
    fun `article panes are flush, never stacked, and keep avoiding hinges`() {
        val hinge = Rect(left = 800f, top = 0f, right = 820f, bottom = 1000f)
        val directive = articlePaneDirective(
            windowDirective(maxVerticalPartitions = 2, excludedBounds = listOf(hinge)),
            AppLayoutType.THREE_PANE
        )

        assertEquals(1, directive.maxVerticalPartitions)
        assertEquals(0.dp, directive.horizontalPartitionSpacerSize)
        assertEquals(LayoutConstants.ArticleListPaneWidth, directive.defaultPanePreferredWidth)
        assertEquals(listOf(hinge), directive.excludedBounds)
    }

    @Test
    fun `opening an article shows it in the detail pane`() = runTest {
        val scaffoldNavigator = FakeScaffoldNavigator()
        val articlePane = ListDetailArticlePaneNavigator(scaffoldNavigator, this)

        articlePane.openArticle("a1")

        assertEquals("a1", articlePane.openArticleId)
        assertEquals(listOf(listDestination, detailDestination("a1")), scaffoldNavigator.history)
    }

    @Test
    fun `opening another article replaces the open one instead of stacking up`() = runTest {
        val scaffoldNavigator = FakeScaffoldNavigator()
        val articlePane = ListDetailArticlePaneNavigator(scaffoldNavigator, this)

        articlePane.openArticle("a1")
        articlePane.openArticle("a2")
        articlePane.openArticle("a2")

        assertEquals(listOf(listDestination, detailDestination("a2")), scaffoldNavigator.history)
    }

    @Test
    fun `closing an article returns to the list`() = runTest {
        val scaffoldNavigator = FakeScaffoldNavigator()
        val articlePane = ListDetailArticlePaneNavigator(scaffoldNavigator, this)

        articlePane.openArticle("a1")
        articlePane.closeArticle()

        assertNull(articlePane.openArticleId)
        assertEquals(listOf(listDestination), scaffoldNavigator.history)
    }

    @Test
    fun `closing without an open article keeps the list destination`() = runTest {
        val scaffoldNavigator = FakeScaffoldNavigator()
        val articlePane = ListDetailArticlePaneNavigator(scaffoldNavigator, this)

        articlePane.closeArticle()

        // The real navigator clears its whole history if asked to go back from the first destination
        assertEquals(listOf(listDestination), scaffoldNavigator.history)
    }

    private fun windowDirective(maxVerticalPartitions: Int = 1, excludedBounds: List<Rect> = emptyList()) =
        PaneScaffoldDirective.Default.copy(
            maxHorizontalPartitions = 3,
            horizontalPartitionSpacerSize = 24.dp,
            maxVerticalPartitions = maxVerticalPartitions,
            excludedBounds = excludedBounds
        )

    private val listDestination = ThreePaneScaffoldDestinationItem<String>(ListDetailPaneScaffoldRole.List)

    private fun detailDestination(articleId: String) = ThreePaneScaffoldDestinationItem(ListDetailPaneScaffoldRole.Detail, articleId)

    /** Destination history semantics of the real navigator, without the pane animations. */
    private class FakeScaffoldNavigator : ThreePaneScaffoldNavigator<String> {
        val history = mutableListOf(ThreePaneScaffoldDestinationItem<String>(ListDetailPaneScaffoldRole.List))

        private val anyValue = ThreePaneScaffoldValue(
            primary = PaneAdaptedValue.Expanded,
            secondary = PaneAdaptedValue.Hidden,
            tertiary = PaneAdaptedValue.Hidden
        )

        override val scaffoldDirective = PaneScaffoldDirective.Default
        override val scaffoldState = MutableThreePaneScaffoldState(anyValue)
        override val scaffoldValue = anyValue
        override val currentDestination get() = history.lastOrNull()
        override var isDestinationHistoryAware = true

        override fun peekPreviousScaffoldValue(backNavigationBehavior: BackNavigationBehavior) = anyValue

        override suspend fun navigateTo(pane: ThreePaneScaffoldRole, contentKey: String?) {
            history += ThreePaneScaffoldDestinationItem(pane, contentKey)
        }

        override fun canNavigateBack(backNavigationBehavior: BackNavigationBehavior): Boolean {
            check(backNavigationBehavior == BackNavigationBehavior.PopLatest)
            return history.size > 1
        }

        override suspend fun navigateBack(backNavigationBehavior: BackNavigationBehavior): Boolean {
            if (!canNavigateBack(backNavigationBehavior)) {
                history.clear()
                return false
            }
            history.removeAt(history.lastIndex)
            return true
        }

        override suspend fun seekBack(backNavigationBehavior: BackNavigationBehavior, fraction: Float) = Unit
    }
}
