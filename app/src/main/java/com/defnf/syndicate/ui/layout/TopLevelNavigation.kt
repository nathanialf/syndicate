package com.defnf.syndicate.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
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
import com.defnf.syndicate.ui.navigation.TopLevelDestination

/**
 * Places [content] next to the navigation [chrome] of the layout.
 *
 * Window insets covered by the chrome are consumed before reaching [content], so screens can always
 * use the default Material window insets without knowing which layout they are shown in.
 */
@Composable
internal fun TopLevelNavigationScaffold(
    chrome: NavigationChrome,
    currentDestination: TopLevelDestination,
    onDestinationClick: (TopLevelDestination) -> Unit,
    bottomBarVisible: Boolean,
    content: @Composable () -> Unit
) {
    when (chrome) {
        NavigationChrome.BOTTOM_BAR -> Scaffold(
            bottomBar = {
                if (bottomBarVisible) {
                    TopLevelNavigationBar(currentDestination, onDestinationClick)
                }
            },
            // Screens draw behind the status bar themselves; only the bar's space is passed down
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                content()
            }
        }

        NavigationChrome.RAIL -> Row(modifier = Modifier.fillMaxSize()) {
            TopLevelNavigationRail(currentDestination, onDestinationClick)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content()
            }
        }

        NavigationChrome.NONE -> content()
    }
}

@Composable
private fun TopLevelNavigationBar(
    currentDestination: TopLevelDestination,
    onDestinationClick: (TopLevelDestination) -> Unit
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                icon = { DestinationIcon(destination) },
                label = { Text(destination.label) },
                selected = currentDestination == destination,
                onClick = { onDestinationClick(destination) }
            )
        }
    }
}

@Composable
private fun TopLevelNavigationRail(
    currentDestination: TopLevelDestination,
    onDestinationClick: (TopLevelDestination) -> Unit
) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationRailItem(
                icon = { DestinationIcon(destination) },
                label = { Text(destination.label) },
                selected = currentDestination == destination,
                onClick = { onDestinationClick(destination) }
            )
        }
    }
}

@Composable
private fun DestinationIcon(destination: TopLevelDestination) {
    Icon(
        imageVector = destination.icon,
        contentDescription = destination.label
    )
}
