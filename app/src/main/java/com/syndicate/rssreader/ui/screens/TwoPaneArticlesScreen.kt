package com.syndicate.rssreader.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.syndicate.rssreader.ui.common.LayoutConstants
import com.syndicate.rssreader.ui.common.LayoutUtils
import com.syndicate.rssreader.ui.components.ArticleContentArea
import com.syndicate.rssreader.ui.navigation.rememberNavigationState
import com.syndicate.rssreader.ui.viewmodel.ArticleListViewModel

@Composable
fun TwoPaneArticlesScreen(
    themeViewModel: com.syndicate.rssreader.ui.viewmodel.ThemeViewModel? = null,
    onNavigateToGroupManagement: () -> Unit = {},
    notificationData: com.syndicate.rssreader.ui.NotificationData? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val navigationState = rememberNavigationState()
    val articleViewModel: ArticleListViewModel = hiltViewModel()
    val feedListViewModel: com.syndicate.rssreader.ui.viewmodel.FeedListViewModel = hiltViewModel()
    
    // Load default group on startup if no specific selection and not forcing all articles
    LaunchedEffect(Unit) {
        if (navigationState.selectedFeedId == null && 
            navigationState.selectedGroupId == null && 
            !navigationState.forceAllArticles) {
            val defaultGroup = feedListViewModel.getDefaultGroup()
            if (defaultGroup != null) {
                navigationState.onGroupSelected(defaultGroup.id)
            }
        }
    }
    
    LaunchedEffect(navigationState.selectedFeedId, navigationState.selectedGroupId, navigationState.forceAllArticles) {
        when {
            navigationState.selectedFeedId != null -> articleViewModel.setFeedId(navigationState.selectedFeedId)
            navigationState.selectedGroupId != null -> articleViewModel.setGroupId(navigationState.selectedGroupId)
            navigationState.forceAllArticles -> articleViewModel.setFeedId(null)
            else -> articleViewModel.setFeedId(null)
        }
    }
    
    // Handle notification data
    LaunchedEffect(notificationData) {
        notificationData?.let { data ->
            when (data) {
                is com.syndicate.rssreader.ui.NotificationData.Article -> {
                    // Navigate to the specific feed and article
                    navigationState.onFeedSelected(data.feedId)
                    navigationState.onArticleSelected(data.articleId)
                }
                is com.syndicate.rssreader.ui.NotificationData.Group -> {
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
                    onDeleteGroup = navigationState.onGroupDeleted,
                    onNavigateToGroupManagement = onNavigateToGroupManagement
                )
            }
        }
        
        // Invisible spacer for separation without visible divider
        Spacer(modifier = Modifier.width(0.dp))
        
        // Right pane content - using shared ArticleContentArea
        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.background
        ) {
            ArticleContentArea(
                navigationState = navigationState,
                themeViewModel = themeViewModel,
                isSidebarMode = true,
                showTopBar = true
            )
        }
    }
}