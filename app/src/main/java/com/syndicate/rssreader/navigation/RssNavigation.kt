package com.syndicate.rssreader.navigation

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
import com.syndicate.rssreader.ui.common.LayoutConstants
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
import com.syndicate.rssreader.ui.components.ArticleContentArea
import com.syndicate.rssreader.ui.navigation.rememberNavigationState
import com.syndicate.rssreader.ui.screens.ArticleDetailScreen
import com.syndicate.rssreader.ui.screens.ArticleListScreen
import com.syndicate.rssreader.ui.screens.FeedListScreen
import com.syndicate.rssreader.ui.screens.GroupManagementScreen
import com.syndicate.rssreader.ui.screens.SettingsScreen
import com.syndicate.rssreader.ui.screens.TwoPaneArticlesScreen

@Composable
fun RssNavigation(
    navController: NavHostController = rememberNavController(),
    themeViewModel: com.syndicate.rssreader.ui.viewmodel.ThemeViewModel
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
                    onNavigateToGroupManagement = {
                        navController.navigate(Screen.GroupManagement.route)
                    }
                )
            }
            
            composable(
                route = "articles/{feedId}",
                arguments = listOf(navArgument("feedId") { type = NavType.LongType })
            ) { 
                // Redirect to main articles view in wide screen mode
                TwoPaneArticlesScreen(
                    themeViewModel = themeViewModel,
                    onNavigateToGroupManagement = {
                        navController.navigate(Screen.GroupManagement.route)
                    }
                )
            }
            
            composable(
                route = "articles/group/{groupId}",
                arguments = listOf(navArgument("groupId") { type = NavType.LongType })
            ) { 
                // Redirect to main articles view in wide screen mode
                TwoPaneArticlesScreen(
                    themeViewModel = themeViewModel,
                    onNavigateToGroupManagement = {
                        navController.navigate(Screen.GroupManagement.route)
                    }
                )
            }
            
            composable(Screen.Feeds.route) {
                // Redirect to main articles view in wide screen mode
                TwoPaneArticlesScreen(
                    themeViewModel = themeViewModel,
                    onNavigateToGroupManagement = {
                        navController.navigate(Screen.GroupManagement.route)
                    }
                )
            }
            
            composable(Screen.Settings.route) {
                SettingsScreen(themeViewModel = themeViewModel)
            }
            
            composable(Screen.GroupManagement.route) {
                GroupManagementScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
            
        }
    } else {
        Log.d("RssNavigation", "Using NARROW SCREEN layout with bottom navigation")
        // Narrow screen: use animated content instead of navigation
        NarrowScreenWithAnimation(
            themeViewModel = themeViewModel
        )
    }
}

sealed class Screen(val route: String) {
    object Articles : Screen("articles")
    object Feeds : Screen("feeds")
    object Settings : Screen("settings")
    object GroupManagement : Screen("group_management")
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
    themeViewModel: com.syndicate.rssreader.ui.viewmodel.ThemeViewModel
) {
    val navigationState = rememberNavigationState()
    
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
                    onNavigateToGroupManagement = {
                        // Could add group management as another animated state
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
                    useSystemBarInsets = false,
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