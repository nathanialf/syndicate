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
    var currentScreen by remember { mutableStateOf(Screen.Articles.route) }
    var selectedArticleId by remember { mutableStateOf<String?>(null) }
    var selectedFeedId by remember { mutableStateOf<Long?>(null) }
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    var forceAllArticles by remember { mutableStateOf(false) }
    var bottomBarVisible by remember { mutableStateOf(true) }
    var topBarVisible by remember { mutableStateOf(true) }
    
    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = bottomBarVisible && selectedArticleId == null,
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
                            selected = currentScreen == item.route,
                            onClick = {
                                if (currentScreen != item.route) {
                                    currentScreen = item.route
                                    selectedArticleId = null
                                    selectedFeedId = null
                                    selectedGroupId = null
                                    forceAllArticles = false
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        val contentState = when {
            selectedArticleId != null -> "article_detail"
            else -> currentScreen
        }
        
        AnimatedContent(
            targetState = contentState,
            transitionSpec = {
                when {
                    targetState == "article_detail" -> {
                        // Slide in from right when opening article
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> fullWidth }
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> -fullWidth }
                        )
                    }
                    initialState == "article_detail" -> {
                        // Slide in from left when going back from article
                        slideInHorizontally(
                            initialOffsetX = { fullWidth -> -fullWidth }
                        ) togetherWith slideOutHorizontally(
                            targetOffsetX = { fullWidth -> fullWidth }
                        )
                    }
                    else -> {
                        // Simple fade for tab switches
                        fadeIn() togetherWith fadeOut()
                    }
                }
            },
            modifier = Modifier.padding(top = paddingValues.calculateTopPadding()),
            label = "narrow_content_transition"
        ) { state ->
            when (state) {
                "article_detail" -> {
                    selectedArticleId?.let { articleId ->
                        ArticleDetailScreen(
                            articleId = articleId,
                            onBackClick = { selectedArticleId = null }
                        )
                    }
                }
                Screen.Articles.route -> {
                    ArticleListScreen(
                        feedId = selectedFeedId,
                        groupId = selectedGroupId,
                        forceAllArticles = forceAllArticles,
                        onScrollDirectionChanged = { isScrollingDown ->
                            bottomBarVisible = !isScrollingDown
                            topBarVisible = !isScrollingDown
                        },
                        onArticleClick = { article ->
                            selectedArticleId = article.id
                        },
                        onBackClick = if (selectedFeedId != null || selectedGroupId != null || forceAllArticles) {
                            {
                                selectedFeedId = null
                                selectedGroupId = null
                                forceAllArticles = false
                            }
                        } else {
                            { /* Do nothing */ }
                        }
                    )
                }
                Screen.Feeds.route -> {
                    val feedListViewModel: com.syndicate.rssreader.ui.viewmodel.FeedListViewModel = hiltViewModel()
                    
                    FeedListScreen(
                        onFeedClick = { feedId ->
                            selectedFeedId = feedId
                            currentScreen = Screen.Articles.route
                        },
                        onScrollDirectionChanged = { isScrollingDown ->
                            bottomBarVisible = !isScrollingDown
                            topBarVisible = !isScrollingDown
                        },
                        onNavigateToGroupManagement = {
                            // Could add group management as another animated state
                        },
                        onGroupClick = { groupId ->
                            selectedGroupId = groupId
                            currentScreen = Screen.Articles.route
                        },
                        bottomBarVisible = bottomBarVisible,
                        onDeleteGroup = { deletedGroupId ->
                            // No special handling needed
                        },
                        onAllFeedsClick = {
                            forceAllArticles = true
                            currentScreen = Screen.Articles.route
                        }
                    )
                }
                Screen.Settings.route -> {
                    SettingsScreen(themeViewModel = themeViewModel)
                }
            }
        }
    }
}