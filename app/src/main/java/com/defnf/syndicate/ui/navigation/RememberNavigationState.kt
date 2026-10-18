package com.defnf.syndicate.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.navigation.compose.hiltViewModel
import com.defnf.syndicate.ui.viewmodel.FeedListViewModel

/**
 * Remembers the app-wide [NavigationState] and, on first launch, selects the default group.
 *
 * [articlePane] must itself be remembered, since the state keeps the instance it was created with.
 */
@Composable
fun rememberNavigationState(articlePane: ArticlePaneNavigator): NavigationState {
    val navigationState = rememberSaveable(saver = NavigationState.saver(articlePane)) {
        NavigationState(articlePane)
    }

    val feedListViewModel: FeedListViewModel = hiltViewModel()

    // Load default group on first launch if no specific selection was made
    LaunchedEffect(navigationState) {
        if (!navigationState.defaultSelectionResolved) {
            val defaultGroup = feedListViewModel.getDefaultGroup()
            Log.d("NavigationState", "Default group found: ${defaultGroup?.name} (id: ${defaultGroup?.id})")
            navigationState.applyDefaultGroup(defaultGroup?.id)
        }
    }

    return navigationState
}
