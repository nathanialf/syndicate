package com.defnf.syndicate.navigation

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.defnf.syndicate.ui.NotificationData
import com.defnf.syndicate.ui.navigation.AppLayoutType
import com.defnf.syndicate.ui.navigation.rememberNavigationState
import com.defnf.syndicate.ui.screens.BottomNavigationLayout
import com.defnf.syndicate.ui.screens.NavigationRailLayout
import com.defnf.syndicate.ui.screens.ThreePaneLayout
import com.defnf.syndicate.ui.screens.TwoPaneLayout
import com.defnf.syndicate.ui.viewmodel.ThemeViewModel

/**
 * Root of the app UI. Picks a layout from the current window width and renders it from a single
 * hoisted [com.defnf.syndicate.ui.navigation.NavigationState], so the selection is preserved when
 * the window changes size (rotation, fold/unfold, split-screen, freeform windows).
 */
@Composable
fun RssNavigation(
    themeViewModel: ThemeViewModel,
    notificationData: NotificationData? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val navigationState = rememberNavigationState()

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

    // Keep content clear of side cutouts and side-mounted navigation bars (e.g. landscape phones)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
    ) {
        val layoutType = AppLayoutType.fromWindowWidth(maxWidth)

        LaunchedEffect(layoutType) {
            Log.d("RssNavigation", "Window width: $maxWidth, layout: $layoutType")
        }

        // Handle system back gesture
        BackHandler(enabled = navigationState.canNavigateBack(layoutType)) {
            navigationState.navigateBack(layoutType)
        }

        when (layoutType) {
            AppLayoutType.BOTTOM_NAVIGATION -> BottomNavigationLayout(navigationState, themeViewModel)
            AppLayoutType.NAVIGATION_RAIL -> NavigationRailLayout(navigationState, themeViewModel)
            AppLayoutType.TWO_PANE -> TwoPaneLayout(navigationState, themeViewModel)
            AppLayoutType.THREE_PANE -> ThreePaneLayout(navigationState, themeViewModel)
        }
    }
}
