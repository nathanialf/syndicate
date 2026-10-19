package com.defnf.syndicate.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.defnf.syndicate.ui.NotificationData
import com.defnf.syndicate.ui.layout.AdaptiveLayout
import com.defnf.syndicate.ui.layout.AppLayoutType
import com.defnf.syndicate.ui.layout.rememberArticlePaneNavigator
import com.defnf.syndicate.ui.layout.rememberArticlePaneScaffoldNavigator
import com.defnf.syndicate.ui.navigation.rememberNavigationState

/**
 * Root of the app UI. Picks a layout from the window size class and renders it from a single
 * hoisted [com.defnf.syndicate.ui.navigation.NavigationState], so the selection is preserved when
 * the window changes size (rotation, fold/unfold, split-screen, freeform windows).
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun RssNavigation(
    notificationData: NotificationData? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)
    val layoutType = AppLayoutType.fromWindowSizeClass(windowAdaptiveInfo.windowSizeClass)
    val articlePaneScaffoldNavigator = rememberArticlePaneScaffoldNavigator(windowAdaptiveInfo, layoutType)
    val navigationState = rememberNavigationState(rememberArticlePaneNavigator(articlePaneScaffoldNavigator))

    LaunchedEffect(layoutType) {
        Log.d("RssNavigation", "Window size class: ${windowAdaptiveInfo.windowSizeClass}, layout: $layoutType")
    }

    // Handle notification data
    LaunchedEffect(notificationData) {
        notificationData?.let { data ->
            when (data) {
                is NotificationData.Article -> navigationState.openArticleFromNotification(data.feedId, data.articleId)
                is NotificationData.Feed -> navigationState.openFeedFromNotification(data.feedId)
                is NotificationData.Group -> navigationState.openGroupFromNotification(data.groupId)
            }
            onNotificationHandled()
        }
    }

    // System back for destinations and selections. Composed before the article panes, whose own
    // predictive back handler takes precedence while an article is open.
    BackHandler(enabled = navigationState.canNavigateBack(layoutType)) {
        navigationState.navigateBack(layoutType)
    }

    // Keep content clear of side cutouts and side-mounted navigation bars (e.g. landscape phones)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        AdaptiveLayout(
            layoutType = layoutType,
            navigationState = navigationState,
            articlePaneNavigator = articlePaneScaffoldNavigator
        )
    }
}
