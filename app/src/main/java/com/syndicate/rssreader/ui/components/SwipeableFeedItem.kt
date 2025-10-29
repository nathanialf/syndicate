package com.syndicate.rssreader.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.syndicate.rssreader.data.models.Feed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableFeedItem(
    feed: Feed,
    isSelected: Boolean,
    onFeedClick: (Long) -> Unit,
    onDeleteFeed: (Long) -> Unit,
    onNotificationToggle: (Long) -> Unit,
    isSidebarMode: Boolean = false,
    resetSwipe: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val swipeState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
    )
    
    // Handle swipe actions
    LaunchedEffect(swipeState.currentValue) {
        when (swipeState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd -> {
                if (swipeState.targetValue != SwipeToDismissBoxValue.Settled) {
                    // Delete action (swipe right) - don't dismiss yet, wait for confirmation
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDeleteFeed(feed.id)
                    swipeState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }
            SwipeToDismissBoxValue.EndToStart -> {
                if (swipeState.targetValue != SwipeToDismissBoxValue.Settled) {
                    // Notification toggle action (swipe left)
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNotificationToggle(feed.id)
                    swipeState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }
            SwipeToDismissBoxValue.Settled -> { /* No action needed */ }
        }
    }
    
    // Reset swipe state when requested
    LaunchedEffect(resetSwipe) {
        if (resetSwipe) {
            swipeState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    
    // Reset swipe state after notification toggle action
    LaunchedEffect(swipeState.targetValue) {
        if (swipeState.targetValue == SwipeToDismissBoxValue.EndToStart) {
            swipeState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    
    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            SwipeBackground(
                swipeDirection = swipeState.dismissDirection,
                isNotificationEnabled = feed.notificationsEnabled
            )
        },
        modifier = modifier
    ) {
        FeedListItem(
            feed = feed,
            isSelected = isSelected,
            onFeedClick = onFeedClick,
            isSidebarMode = isSidebarMode
        )
    }
}