package com.defnf.syndicate.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.common.LayoutUtils
import com.defnf.syndicate.ui.navigation.NavigationState
import com.defnf.syndicate.ui.screens.FeedListScreen

/**
 * Persistent feeds sidebar used by the multi-pane layouts, highlighting the current selection.
 */
@Composable
fun FeedsSidebar(
    navigationState: NavigationState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(LayoutConstants.SidebarWidth)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Top padding for system bar
            LayoutUtils.SystemBarTopSpacer()

            FeedListScreen(
                onFeedClick = navigationState::selectFeed,
                isSidebarMode = true,
                selectedFeedId = navigationState.selectedFeedId,
                selectedGroupId = navigationState.selectedGroupId,
                onAllFeedsClick = navigationState::selectAllArticles,
                onDeleteFeed = navigationState::onFeedDeleted,
                onGroupClick = navigationState::selectGroup,
                onDeleteGroup = navigationState::onGroupDeleted
            )
        }
    }
}
