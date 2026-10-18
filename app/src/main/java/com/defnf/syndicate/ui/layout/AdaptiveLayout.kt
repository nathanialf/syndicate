package com.defnf.syndicate.ui.layout

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.navigation.NavigationState

/**
 * Renders the app for [layoutType]: the panes resolved by [resolvePanes], side by side, inside the
 * layout's navigation chrome. Every layout goes through this one code path.
 */
@Composable
fun AdaptiveLayout(
    layoutType: AppLayoutType,
    navigationState: NavigationState
) {
    val panes = resolvePanes(
        layoutType = layoutType,
        destination = navigationState.currentDestination,
        openArticleId = navigationState.selectedArticleId
    )

    TopLevelNavigationScaffold(
        chrome = layoutType.navigationChrome,
        currentDestination = navigationState.currentDestination,
        onDestinationClick = navigationState::onTopLevelDestinationClick,
        // Give an article being read the full height of the window
        bottomBarVisible = panes.last().content !is PaneContent.ArticleDetail
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            panes.forEach { pane ->
                key(pane.role) {
                    val sizeModifier = when (pane.role) {
                        PaneRole.SIDEBAR -> Modifier.width(LayoutConstants.SidebarWidth)
                        PaneRole.LIST -> Modifier.width(LayoutConstants.ArticleListPaneWidth)
                        PaneRole.MAIN -> Modifier.weight(1f)
                    }
                    PaneHost(
                        pane = pane,
                        layoutType = layoutType,
                        navigationState = navigationState,
                        modifier = sizeModifier.fillMaxHeight()
                    )
                }
            }
        }
    }
}
