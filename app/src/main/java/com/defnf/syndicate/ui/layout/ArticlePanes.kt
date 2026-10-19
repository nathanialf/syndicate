package com.defnf.syndicate.ui.layout

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.navigation.ArticlePaneNavigator
import com.defnf.syndicate.ui.navigation.NavigationState
import com.defnf.syndicate.ui.screens.ArticleDetailScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

/**
 * Remembers the navigator of the article list-detail pane scaffold. Its destination history holds
 * the open article and is saved across configuration changes.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberArticlePaneScaffoldNavigator(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    layoutType: AppLayoutType
): ThreePaneScaffoldNavigator<String> = rememberListDetailPaneScaffoldNavigator<String>(
    scaffoldDirective = articlePaneDirective(calculatePaneScaffoldDirective(windowAdaptiveInfo), layoutType)
)

/** Remembers the [ArticlePaneNavigator] that opens and closes articles through [navigator]. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberArticlePaneNavigator(navigator: ThreePaneScaffoldNavigator<String>): ArticlePaneNavigator {
    val scope = rememberCoroutineScope()
    return remember(navigator, scope) { ListDetailArticlePaneNavigator(navigator, scope) }
}

/**
 * Adapts the window-based [windowDirective] to the space left for the article panes in [layoutType].
 *
 * The feeds sidebar takes part of the window, so the number of panes follows the app layout rather
 * than the window size class. Hinge avoidance ([PaneScaffoldDirective.excludedBounds]) is kept.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
internal fun articlePaneDirective(
    windowDirective: PaneScaffoldDirective,
    layoutType: AppLayoutType
): PaneScaffoldDirective = windowDirective.copy(
    maxHorizontalPartitions = if (layoutType.hasArticleListBesideDetail) 2 else 1,
    horizontalPartitionSpacerSize = 0.dp,
    // The article list and the article are never stacked, e.g. on tall windows or in tabletop posture
    maxVerticalPartitions = 1,
    verticalPartitionSpacerSize = 0.dp,
    defaultPanePreferredWidth = LayoutConstants.ArticleListPaneWidth
)

/**
 * [ArticlePaneNavigator] backed by the list-detail scaffold's [navigator].
 *
 * Articles replace each other instead of stacking up, so the destination history is at most
 * `[List, Detail(article)]` and back always returns to the list.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Stable
internal class ListDetailArticlePaneNavigator(
    private val navigator: ThreePaneScaffoldNavigator<String>,
    private val scope: CoroutineScope
) : ArticlePaneNavigator {

    override val openArticleId: String?
        get() = navigator.currentDestination
            ?.takeIf { it.pane == ListDetailPaneScaffoldRole.Detail }
            ?.contentKey

    override fun openArticle(articleId: String) {
        if (openArticleId == articleId) return
        closeArticle()
        navigate { navigateTo(ListDetailPaneScaffoldRole.Detail, articleId) }
    }

    override fun closeArticle() {
        // navigateBack() clears the whole history when there is nothing to go back to
        if (openArticleId != null && navigator.canNavigateBack(BackNavigationBehavior.PopLatest)) {
            navigate { navigateBack(BackNavigationBehavior.PopLatest) }
        }
    }

    /**
     * Starts [action] undispatched: the navigator updates its destination history before its first
     * suspension, so the change is visible immediately and only the pane animation runs
     * asynchronously. A newer animation cancels one still in progress.
     */
    private fun navigate(action: suspend ThreePaneScaffoldNavigator<String>.() -> Unit) {
        scope.launch(start = CoroutineStart.UNDISPATCHED) { navigator.action() }
    }
}

/**
 * The article list and the article being read, laid out by the Material list-detail pane scaffold:
 * side by side when [AppLayoutType.hasArticleListBesideDetail], otherwise one at a time with the
 * standard pane motion and predictive back.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
internal fun ArticleListDetail(
    navigator: ThreePaneScaffoldNavigator<String>,
    layoutType: AppLayoutType,
    navigationState: NavigationState,
    modifier: Modifier = Modifier
) {
    NavigableListDetailPaneScaffold(
        navigator = navigator,
        listPane = {
            AnimatedPane {
                ArticleListPane(
                    navigationState = navigationState,
                    layoutType = layoutType
                )
            }
        },
        detailPane = {
            AnimatedPane {
                ArticleDetailPane(
                    navigator = navigator,
                    navigationState = navigationState
                )
            }
        },
        modifier = modifier,
        // Beside the list, closing an article leaves the pane layout unchanged, so back pops
        // on content changes rather than only on layout changes
        defaultBackBehavior = BackNavigationBehavior.PopUntilContentChange
    )

    // The navigator only moves the scaffold to its current pane layout when it navigates, and only
    // while the scaffold is composed. Catch up when the panes appear after an article was opened
    // elsewhere (e.g. from a notification on another tab) or when resizing changes the pane count.
    LaunchedEffect(navigator, navigator.scaffoldValue) {
        if (navigator.scaffoldState.targetState != navigator.scaffoldValue) {
            // Seeking back with no progress animates the scaffold to the current scaffold value
            navigator.seekBack(fraction = 0f)
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
private fun ArticleDetailPane(
    navigator: ThreePaneScaffoldNavigator<String>,
    navigationState: NavigationState
) {
    val openArticleId = navigationState.openArticleId
    val isLeaving = navigator.scaffoldValue[ListDetailPaneScaffoldRole.Detail] == PaneAdaptedValue.Hidden
    // While the pane animates out after closing an article, keep showing that article. Only what
    // was on screen counts: an article closed while the pane stayed visible is not brought back.
    val previous = remember { ShownArticle() }
    val articleId = openArticleId ?: previous.id.takeIf { isLeaving }
    SideEffect { previous.id = articleId }

    if (articleId != null) {
        key(articleId) {
            ArticleDetailScreen(
                articleId = articleId,
                onBackClick = navigationState::closeArticle
            )
        }
    } else {
        NoArticleSelected()
    }
}

/** The article shown by the last committed composition of [ArticleDetailPane]. */
private class ShownArticle {
    var id: String? = null
}
