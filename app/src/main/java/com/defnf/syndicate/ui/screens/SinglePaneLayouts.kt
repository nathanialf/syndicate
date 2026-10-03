package com.defnf.syndicate.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.defnf.syndicate.ui.common.LayoutUtils
import com.defnf.syndicate.ui.components.ArticleContentArea
import com.defnf.syndicate.ui.navigation.NavigationState
import com.defnf.syndicate.ui.navigation.TopLevelDestination
import com.defnf.syndicate.ui.viewmodel.ThemeViewModel

/**
 * Compact width layout: single pane with a bottom navigation bar.
 * The bottom bar is hidden while reading an article.
 */
@Composable
fun BottomNavigationLayout(
    navigationState: NavigationState,
    themeViewModel: ThemeViewModel
) {
    Scaffold(
        bottomBar = {
            if (navigationState.selectedArticleId == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) },
                            selected = navigationState.currentDestination == destination,
                            onClick = { navigationState.onTopLevelDestinationClick(destination) }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        SinglePaneContent(
            navigationState = navigationState,
            themeViewModel = themeViewModel,
            hasBottomNavigation = true,
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
        )
    }
}

/**
 * Medium width layout (portrait tablets, unfolded foldables, landscape phones):
 * single pane with a navigation rail along the start edge.
 */
@Composable
fun NavigationRailLayout(
    navigationState: NavigationState,
    themeViewModel: ThemeViewModel
) {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            TopLevelDestination.entries.forEach { destination ->
                NavigationRailItem(
                    icon = {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label
                        )
                    },
                    label = { Text(destination.label) },
                    selected = navigationState.currentDestination == destination,
                    onClick = { navigationState.onTopLevelDestinationClick(destination) }
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            SinglePaneContent(
                navigationState = navigationState,
                themeViewModel = themeViewModel,
                hasBottomNavigation = false,
                modifier = Modifier.padding(top = LayoutUtils.getSystemBarTopPadding())
            )
        }
    }
}

@Composable
private fun SinglePaneContent(
    navigationState: NavigationState,
    themeViewModel: ThemeViewModel,
    hasBottomNavigation: Boolean,
    modifier: Modifier = Modifier
) {
    when (navigationState.currentDestination) {
        TopLevelDestination.ARTICLES -> {
            ArticleContentArea(
                navigationState = navigationState,
                themeViewModel = themeViewModel,
                isSidebarMode = false,
                hasBottomNavigation = hasBottomNavigation,
                modifier = modifier
            )
        }
        TopLevelDestination.FEEDS -> {
            FeedListScreen(
                onFeedClick = navigationState::selectFeed,
                onGroupClick = navigationState::selectGroup,
                onAllFeedsClick = navigationState::selectAllArticles,
                onDeleteFeed = navigationState::onFeedDeleted,
                onDeleteGroup = navigationState::onGroupDeleted,
                hasBottomNavigation = hasBottomNavigation,
                modifier = modifier
            )
        }
        TopLevelDestination.SETTINGS -> {
            SettingsScreen(
                themeViewModel = themeViewModel,
                modifier = modifier,
                isSidebarMode = false
            )
        }
    }
}
