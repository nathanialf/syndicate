package com.defnf.syndicate.ui.layout

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.rememberNavigationSuiteScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.navigation.NavigationState
import com.defnf.syndicate.ui.navigation.TopLevelDestination

/**
 * Renders the app for [layoutType]: the navigation bar or rail from [NavigationSuiteScaffold],
 * the feeds sidebar when the layout has one, and the main content beside it.
 *
 * [NavigationSuiteScaffold] consumes the window insets its bar or rail covers, so screens use the
 * default Material window insets in every layout.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AdaptiveLayout(
    layoutType: AppLayoutType,
    navigationState: NavigationState,
    articlePaneNavigator: ThreePaneScaffoldNavigator<String>
) {
    val navigationSuiteState = rememberNavigationSuiteScaffoldState()
    val hideNavigation = layoutType.hidesNavigationWhileReading && navigationState.openArticleId != null
    LaunchedEffect(hideNavigation) {
        if (hideNavigation) navigationSuiteState.hide() else navigationSuiteState.show()
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                item(
                    selected = navigationState.currentDestination == destination,
                    onClick = { navigationState.onTopLevelDestinationClick(destination) },
                    // The label already describes the item
                    icon = { Icon(imageVector = destination.icon, contentDescription = null) },
                    label = { Text(destination.label) }
                )
            }
        },
        layoutType = layoutType.navigationSuiteType,
        navigationSuiteColors = NavigationSuiteDefaults.colors(
            navigationBarContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            navigationRailContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        state = navigationSuiteState
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (layoutType.hasFeedsSidebar) {
                FeedsPane(
                    navigationState = navigationState,
                    // The selection's articles are shown beside the sidebar, so highlight it there
                    highlightSelection = true,
                    modifier = Modifier
                        .width(LayoutConstants.SidebarWidth)
                        .fillMaxHeight()
                )
            }

            AnimatedContent(
                targetState = layoutType.mainContentFor(navigationState.currentDestination),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                label = "main_content"
            ) { mainContent ->
                when (mainContent) {
                    MainContent.FEEDS -> FeedsPane(
                        navigationState = navigationState,
                        highlightSelection = false
                    )

                    MainContent.ARTICLES -> ArticleListDetail(
                        navigator = articlePaneNavigator,
                        layoutType = layoutType,
                        navigationState = navigationState
                    )

                    MainContent.SETTINGS -> SettingsPane(
                        navigationState = navigationState,
                        layoutType = layoutType
                    )
                }
            }
        }
    }
}
