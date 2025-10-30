package com.defnf.syndicate.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.common.LayoutUtils
import com.defnf.syndicate.ui.components.ArticleContentArea
import com.defnf.syndicate.ui.navigation.rememberNavigationState

@Composable
fun TwoPaneArticlesScreen(
    themeViewModel: com.defnf.syndicate.ui.viewmodel.ThemeViewModel? = null,
    notificationData: com.defnf.syndicate.ui.NotificationData? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val navigationState = rememberNavigationState()
    
    // Handle notification data
    LaunchedEffect(notificationData) {
        notificationData?.let { data ->
            when (data) {
                is com.defnf.syndicate.ui.NotificationData.Article -> {
                    // Navigate to the specific feed and article
                    navigationState.onFeedSelected(data.feedId)
                    navigationState.onArticleSelected(data.articleId)
                }
                is com.defnf.syndicate.ui.NotificationData.Group -> {
                    // Navigate to the specific group
                    navigationState.onGroupSelected(data.groupId)
                }
            }
            onNotificationHandled()
        }
    }
    
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Feeds sidebar
        Surface(
            modifier = Modifier
                .width(LayoutConstants.SidebarWidth)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                // Top padding for system bar  
                LayoutUtils.SystemBarTopSpacer()
                
                FeedListScreen(
                    onFeedClick = navigationState.onFeedSelected,
                    isSidebarMode = true,
                    selectedFeedId = navigationState.selectedFeedId,
                    selectedGroupId = navigationState.selectedGroupId,
                    onAllFeedsClick = navigationState.onAllFeedsSelected,
                    onDeleteFeed = navigationState.onFeedDeleted,
                    onGroupClick = navigationState.onGroupSelected,
                    onDeleteGroup = navigationState.onGroupDeleted
                )
            }
        }
        
        // Invisible spacer for separation without visible divider
        Spacer(modifier = Modifier.width(0.dp))
        
        // Right pane content - using shared ArticleContentArea (single source of truth)
        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.background
        ) {
            ArticleContentArea(
                navigationState = navigationState,
                themeViewModel = themeViewModel,
                isSidebarMode = true,
                showTopBar = true  // Only difference: wide screen shows settings button
            )
        }
    }
}