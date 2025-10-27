package com.syndicate.rssreader.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.syndicate.rssreader.R
import com.syndicate.rssreader.ui.common.AppTopBar
import com.syndicate.rssreader.ui.common.LayoutConstants
import com.syndicate.rssreader.ui.theme.CormorantGaramond
import com.syndicate.rssreader.ui.viewmodel.ArticleListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoPaneArticlesScreen(
    onScrollDirectionChanged: (Boolean) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    themeViewModel: com.syndicate.rssreader.ui.viewmodel.ThemeViewModel? = null,
    onNavigateToGroupManagement: () -> Unit = {}
) {
    var selectedFeedId by remember { mutableStateOf<Long?>(null) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var forceAllArticles by remember { mutableStateOf(false) }
    var topBarVisible by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    
    val articleViewModel: ArticleListViewModel = hiltViewModel()
    val feedListViewModel: com.syndicate.rssreader.ui.viewmodel.FeedListViewModel = hiltViewModel()
    val currentFeed by articleViewModel.currentFeed.collectAsState()
    
    // Load default group on startup if no specific selection and not forcing all articles
    LaunchedEffect(Unit) {
        if (selectedFeedId == null && selectedGroupId == null && !forceAllArticles) {
            val defaultGroup = feedListViewModel.getDefaultGroup()
            if (defaultGroup != null) {
                selectedGroupId = defaultGroup.id
            }
        }
    }
    
    LaunchedEffect(selectedFeedId, selectedGroupId, forceAllArticles) {
        when {
            selectedFeedId != null -> articleViewModel.setFeedId(selectedFeedId)
            selectedGroupId != null -> articleViewModel.setGroupId(selectedGroupId)
            forceAllArticles -> articleViewModel.setFeedId(null) // Explicitly show all articles
            else -> articleViewModel.setFeedId(null) // Show all articles
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
                Spacer(
                    modifier = Modifier.padding(
                        top = with(androidx.compose.ui.platform.LocalDensity.current) {
                            WindowInsets.systemBars.getTop(this).toDp()
                        }
                    )
                )
                
                FeedListScreen(
                    onFeedClick = { feedId ->
                        selectedFeedId = feedId
                        selectedGroupId = null
                        forceAllArticles = false
                        showSettings = false
                    },
                    onScrollDirectionChanged = { /* Feeds don't control top bar in this layout */ },
                    isSidebarMode = true,
                    selectedFeedId = selectedFeedId,
                    selectedGroupId = selectedGroupId,
                    onAllFeedsClick = { 
                        selectedFeedId = null
                        selectedGroupId = null
                        forceAllArticles = true
                        showSettings = false
                    },
                    onDeleteFeed = { deletedFeedId ->
                        // If the currently selected feed is deleted, go back to "All Feeds"
                        if (selectedFeedId == deletedFeedId) {
                            selectedFeedId = null
                        }
                        showSettings = false
                    },
                    onGroupClick = { groupId ->
                        selectedGroupId = groupId
                        selectedFeedId = null
                        forceAllArticles = false
                        showSettings = false
                    },
                    onDeleteGroup = { deletedGroupId ->
                        // If the currently selected group is deleted, go back to "All Feeds"
                        if (selectedGroupId == deletedGroupId) {
                            selectedGroupId = null
                        }
                        showSettings = false
                    },
                    onNavigateToGroupManagement = onNavigateToGroupManagement
                )
            }
        }
        
        // Invisible spacer for separation without visible divider
        Spacer(modifier = Modifier.width(0.dp))
        
        // Right pane content (Articles or Settings)
        Surface(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showSettings && themeViewModel != null) {
                // Settings content
                Column {
                    // Top padding for system bar  
                    Spacer(
                        modifier = Modifier.padding(
                            top = with(androidx.compose.ui.platform.LocalDensity.current) {
                                WindowInsets.systemBars.getTop(this).toDp()
                            }
                        )
                    )
                    
                    SettingsScreen(themeViewModel = themeViewModel)
                }
            } else {
                // Articles content
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Articles content that fills the entire space
                    val systemBarPadding = with(LocalDensity.current) {
                        WindowInsets.systemBars.getTop(this).toDp()
                    }
                    
                    ArticleListScreen(
                        feedId = selectedFeedId,
                        groupId = selectedGroupId,
                        forceAllArticles = forceAllArticles,
                        onScrollDirectionChanged = { isScrollingDown ->
                            topBarVisible = !isScrollingDown
                            onScrollDirectionChanged(isScrollingDown)
                        },
                        isSidebarMode = true,
                        topBarVisible = topBarVisible,
                        additionalTopPadding = systemBarPadding
                    )
                    
                    // Top app bar for articles positioned at the top
                    androidx.compose.animation.AnimatedVisibility(
                        visible = topBarVisible,
                        enter = slideInVertically(initialOffsetY = { -it }),
                        exit = slideOutVertically(targetOffsetY = { -it }),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(
                                top = with(LocalDensity.current) {
                                    WindowInsets.systemBars.getTop(this).toDp()
                                }
                            )
                    ) {
                        AppTopBar(
                            title = "Syndicate",
                            subtitle = if (showSettings) {
                                "Settings"
                            } else {
                                currentFeed?.title ?: "All Articles"
                            },
                            showSettingsButton = !showSettings,
                            onSettingsClick = { showSettings = true },
                            showBackButton = showSettings,
                            onBackClick = { showSettings = false }
                        )
                    }
                }
            }
        }
    }
}