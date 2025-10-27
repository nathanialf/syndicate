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
import com.syndicate.rssreader.data.models.Feed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableFeedItem(
    feed: Feed,
    isSelected: Boolean,
    onFeedClick: (Long) -> Unit,
    onDeleteFeed: (Long) -> Unit,
    onAddToGroup: (Long) -> Unit,
    isSidebarMode: Boolean = false,
    resetSwipe: Boolean = false,
    modifier: Modifier = Modifier
) {
    val swipeState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Delete action (swipe right) - don't dismiss yet, wait for confirmation
                    onDeleteFeed(feed.id)
                    false // Don't dismiss, wait for dialog result
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Add to group action (swipe left)
                    onAddToGroup(feed.id)
                    false // Don't dismiss, just trigger action
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )
    
    // Reset swipe state when requested
    LaunchedEffect(resetSwipe) {
        if (resetSwipe) {
            swipeState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    
    // Reset swipe state after add to group action
    LaunchedEffect(swipeState.targetValue) {
        if (swipeState.targetValue == SwipeToDismissBoxValue.EndToStart) {
            swipeState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    
    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            SwipeBackground(
                swipeDirection = swipeState.dismissDirection
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