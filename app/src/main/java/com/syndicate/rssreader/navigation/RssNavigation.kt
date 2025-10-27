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
    
    var bottomBarVisible by remember { mutableStateOf(true) }
    var topBarVisible by remember { mutableStateOf(true) }
    
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
                    onScrollDirectionChanged = { isScrollingDown ->
                        bottomBarVisible = !isScrollingDown
                        topBarVisible = !isScrollingDown
                    },
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
                    onScrollDirectionChanged = { isScrollingDown ->
                        bottomBarVisible = !isScrollingDown
                        topBarVisible = !isScrollingDown
                    },
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
                    onScrollDirectionChanged = { isScrollingDown ->
                        bottomBarVisible = !isScrollingDown
                        topBarVisible = !isScrollingDown
                    },
                    themeViewModel = themeViewModel,
                    onNavigateToGroupManagement = {
                        navController.navigate(Screen.GroupManagement.route)
                    }
                )
            }
            
            composable(Screen.Feeds.route) {
                // Redirect to main articles view in wide screen mode
                TwoPaneArticlesScreen(
                    onScrollDirectionChanged = { isScrollingDown ->
                        bottomBarVisible = !isScrollingDown
                        topBarVisible = !isScrollingDown
                    },
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
        // Narrow screen: use traditional navigation with bottom bar
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = bottomBarVisible,
                    enter = slideInVertically(initialOffsetY = { it }),
                    exit = slideOutVertically(targetOffsetY = { it })
                ) {
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
                                selected = when (item.route) {
                                    Screen.Feeds.route -> {
                                        // Feeds tab is selected when on feeds screen OR viewing a specific feed
                                        currentDestination?.route == Screen.Feeds.route ||
                                        currentDestination?.route?.startsWith("articles/") == true
                                    }
                                    else -> currentDestination?.hierarchy?.any { it.route == item.route } == true
                                },
                                onClick = {
                                    // Special handling for Feeds tab - always navigate to feeds list
                                    if (item.route == Screen.Feeds.route) {
                                        // Always navigate to feeds, even if already on a feed-related screen
                                        navController.navigate(Screen.Feeds.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = false // Never restore state for feeds
                                        }
                                    } else if (currentDestination?.route != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = (item.route == Screen.Articles.route)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screen.Articles.route,
                modifier = Modifier.padding(top = paddingValues.calculateTopPadding())
            ) {
                composable(Screen.Articles.route) {
                    ArticleListScreen(
                        onScrollDirectionChanged = { isScrollingDown ->
                            bottomBarVisible = !isScrollingDown
                            topBarVisible = !isScrollingDown
                        }
                    )
                }
                
                composable("articles/all") {
                    ArticleListScreen(
                        forceAllArticles = true,
                        onScrollDirectionChanged = { isScrollingDown ->
                            bottomBarVisible = !isScrollingDown
                            topBarVisible = !isScrollingDown
                        }
                    )
                }
                
                composable(
                    route = "articles/{feedId}",
                    arguments = listOf(navArgument("feedId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val feedId = backStackEntry.arguments?.getLong("feedId") ?: 0L
                    ArticleListScreen(
                        feedId = feedId,
                        onBackClick = { navController.popBackStack() },
                        onScrollDirectionChanged = { isScrollingDown ->
                            bottomBarVisible = !isScrollingDown
                            topBarVisible = !isScrollingDown
                        }
                    )
                }
                
                composable(
                    route = "articles/group/{groupId}",
                    arguments = listOf(navArgument("groupId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getLong("groupId") ?: 0L
                    ArticleListScreen(
                        groupId = groupId,
                        onBackClick = { navController.popBackStack() },
                        onScrollDirectionChanged = { isScrollingDown ->
                            bottomBarVisible = !isScrollingDown
                            topBarVisible = !isScrollingDown
                        }
                    )
                }
                
                composable(Screen.Feeds.route) {
                    Log.d("RssNavigation", "Navigating to Feeds screen - narrow mode, isSidebarMode should be false")
                    
                    val feedListViewModel: com.syndicate.rssreader.ui.viewmodel.FeedListViewModel = hiltViewModel()
                    
                    FeedListScreen(
                        onFeedClick = { feedId ->
                            navController.navigate("articles/$feedId")
                        },
                        onScrollDirectionChanged = { isScrollingDown ->
                            bottomBarVisible = !isScrollingDown
                            topBarVisible = !isScrollingDown
                        },
                        onNavigateToGroupManagement = {
                            navController.navigate(Screen.GroupManagement.route)
                        },
                        onGroupClick = { groupId ->
                            navController.navigate("articles/group/$groupId")
                        },
                        bottomBarVisible = bottomBarVisible,
                        onDeleteGroup = { deletedGroupId ->
                            // No special handling needed for narrow screen
                        },
                        onAllFeedsClick = {
                            navController.navigate("articles/all")
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
        }
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