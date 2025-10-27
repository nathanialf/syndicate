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
import com.syndicate.rssreader.R
import com.syndicate.rssreader.ui.common.AppTopBar
import com.syndicate.rssreader.ui.common.LayoutConstants
import com.syndicate.rssreader.ui.theme.CormorantGaramond

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TwoPaneArticlesScreen(
    onScrollDirectionChanged: (Boolean) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    themeViewModel: com.syndicate.rssreader.ui.viewmodel.ThemeViewModel? = null
) {
    var selectedFeedId by remember { mutableStateOf<Long?>(null) }
    var topBarVisible by remember { mutableStateOf(true) }
    var showSettings by remember { mutableStateOf(false) }
    
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Feeds sidebar
        Surface(
            modifier = Modifier
                .width(LayoutConstants.SidebarWidth)
                .fillMaxHeight(),
            color = MaterialTheme.colorScheme.background
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
                        showSettings = false
                    },
                    onScrollDirectionChanged = { /* Feeds don't control top bar in this layout */ },
                    isSidebarMode = true,
                    selectedFeedId = selectedFeedId,
                    showAllFeedsOption = true,
                    onAllFeedsClick = { 
                        selectedFeedId = null
                        showSettings = false
                    },
                    onDeleteFeed = { deletedFeedId ->
                        // If the currently selected feed is deleted, go back to "All Feeds"
                        if (selectedFeedId == deletedFeedId) {
                            selectedFeedId = null
                        }
                        showSettings = false
                    }
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
                            title = if (showSettings) "Settings" else stringResource(R.string.app_name),
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