package com.defnf.syndicate.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import android.util.Log
import com.defnf.syndicate.ui.common.LayoutConstants
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.hilt.navigation.compose.hiltViewModel
import java.net.URLEncoder
import java.net.URLDecoder
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.compose.BackHandler
import com.defnf.syndicate.ui.components.ArticleContentArea
import com.defnf.syndicate.ui.navigation.rememberNavigationState
import com.defnf.syndicate.ui.screens.ArticleDetailScreen
import com.defnf.syndicate.ui.screens.ArticleListScreen
import com.defnf.syndicate.ui.screens.FeedListScreen
import com.defnf.syndicate.ui.screens.SettingsScreen
import com.defnf.syndicate.ui.screens.TwoPaneArticlesScreen

@Composable
fun RssNavigation(
    navController: NavHostController = rememberNavController(),
    themeViewModel: com.defnf.syndicate.ui.viewmodel.ThemeViewModel,
    notificationData: com.defnf.syndicate.ui.NotificationData? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp.dp >= LayoutConstants.WideScreenBreakpoint
    
    Log.d("RssNavigation", "Screen width: ${configuration.screenWidthDp}dp, isWideScreen: $isWideScreen")
    
    
    if (isWideScreen) {
        Log.d("RssNavigation", "Using WIDE SCREEN layout")
        // Wide screen: use two-pane layout, no bottom navigation
        NavHost(
            navController = navController,
            startDestination = Screen.Articles.route,
            modifier = Modifier
        ) {
            composable(Screen.Articles.route) {
                TwoPaneArticlesScreen(
                    themeViewModel = themeViewModel,
                    notificationData = notificationData,
                    onNotificationHandled = onNotificationHandled
                )
            }
            
            composable(
                route = "articles/{feedId}",
                arguments = listOf(navArgument("feedId") { type = NavType.LongType })
            ) { 
                // Redirect to main articles view in wide screen mode
                TwoPaneArticlesScreen(
                    themeViewModel = themeViewModel
                )
            }
            
            composable(
                route = "articles/group/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { 
                // Redirect to main articles view in wide screen mode
                TwoPaneArticlesScreen(
                    themeViewModel = themeViewModel
                )
            }
            
            composable(Screen.Feeds.route) {
                // Redirect to main articles view in wide screen mode
                TwoPaneArticlesScreen(
                    themeViewModel = themeViewModel
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(themeViewModel = themeViewModel)
            }
            
            
        }
    } else {
        Log.d("RssNavigation", "Using NARROW SCREEN layout with bottom navigation")
        // Narrow screen: use animated content instead of navigation
        NarrowScreenWithAnimation(
            themeViewModel = themeViewModel,
            notificationData = notificationData,
            onNotificationHandled = onNotificationHandled
        )
    }
}

sealed class Screen(val route: String) {
    object Articles : Screen("articles")
    object Feeds : Screen("feeds")
    object Settings : Screen("settings")
}

data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(
        route = Screen.Articles.route,
        icon = Icons.AutoMirrored.Filled.Article,
        label = "Articles"
    ),
    BottomNavItem(
        route = Screen.Feeds.route,
        icon = Icons.Default.RssFeed,
        label = "Feeds"
    ),
    BottomNavItem(
        route = Screen.Settings.route,
        icon = Icons.Default.Settings,
        label = "Settings"
    )
)

@Composable
fun NarrowScreenWithAnimation(
    themeViewModel: com.defnf.syndicate.ui.viewmodel.ThemeViewModel,
    notificationData: com.defnf.syndicate.ui.NotificationData? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val navigationState = rememberNavigationState()
    
    // Handle system back gesture
    BackHandler(
        enabled = navigationState.selectedArticleId != null || 
                  navigationState.selectedFeedId != null || 
                  navigationState.selectedGroupId != null || 
                  navigationState.forceAllArticles ||
                  navigationState.showSettings ||
                  navigationState.currentScreen != Screen.Articles.route
    ) {
        when {
            navigationState.showSettings -> navigationState.onShowSettings(false)
            navigationState.selectedArticleId != null -> navigationState.onBackFromArticle()
            navigationState.selectedFeedId != null || 
            navigationState.selectedGroupId != null || 
            navigationState.forceAllArticles -> navigationState.onBackFromSelection()
            navigationState.currentScreen != Screen.Articles.route -> {
                // Navigate back to Articles screen from any other screen
                navigationState.onScreenChanged(Screen.Articles.route)
            }
        }
    }
    
    // Handle notification data
    LaunchedEffect(notificationData) {
        notificationData?.let { data ->
            when (data) {
                is com.defnf.syndicate.ui.NotificationData.Article -> {
                    // Navigate to the specific feed and article
                    navigationState.onFeedSelected(data.feedId)
                    navigationState.onArticleSelected(data.articleId)
                    navigationState.onScreenChanged(Screen.Articles.route)
                }
                is com.defnf.syndicate.ui.NotificationData.Group -> {
                    // Navigate to the specific group
                    navigationState.onGroupSelected(data.groupId)
                    navigationState.onScreenChanged(Screen.Articles.route)
                }
            }
            onNotificationHandled()
        }
    }
    
    Scaffold(
        bottomBar = {
            if (navigationState.selectedArticleId == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { 
                                Icon(
                                    imageVector = item.icon, 
                                    contentDescription = item.label
                                ) 
                            },
                            label = { Text(item.label) },
                            selected = navigationState.currentScreen == item.route,
                            onClick = {
                                if (navigationState.currentScreen != item.route) {
                                    navigationState.onScreenChanged(item.route)
                                    // Special handling for Articles button - always go to default view
                                    if (item.route == Screen.Articles.route) {
                                        navigationState.onNavigateToDefault()
                                    }
                                } else if (item.route == Screen.Articles.route) {
                                    // If already on Articles screen, also reset to default
                                    navigationState.onNavigateToDefault()
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when (navigationState.currentScreen) {
            Screen.Articles.route -> {
                ArticleContentArea(
                    navigationState = navigationState,
                    themeViewModel = themeViewModel,
                    isSidebarMode = false,
                    showTopBar = false,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
            Screen.Feeds.route -> {
                FeedListScreen(
                    onFeedClick = { feedId ->
                        navigationState.onFeedSelected(feedId)
                        navigationState.onScreenChanged(Screen.Articles.route)
                    },
                    onGroupClick = { groupId ->
                        navigationState.onGroupSelected(groupId)
                        navigationState.onScreenChanged(Screen.Articles.route)
                    },
                    onDeleteGroup = navigationState.onGroupDeleted,
                    onAllFeedsClick = {
                        navigationState.onAllFeedsSelected()
                        navigationState.onScreenChanged(Screen.Articles.route)
                    },
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
            Screen.Settings.route -> {
                SettingsScreen(
                    themeViewModel = themeViewModel,
                    modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
                )
            }
        }
    }
}