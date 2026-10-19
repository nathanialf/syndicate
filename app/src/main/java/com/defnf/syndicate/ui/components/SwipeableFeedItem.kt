package com.defnf.syndicate.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.defnf.syndicate.data.models.Feed

/** A feed row: swipe towards the end to delete, towards the start to toggle notifications, long press for options. */
@Composable
fun SwipeableFeedItem(
    feed: Feed,
    isSelected: Boolean,
    onFeedClick: (Long) -> Unit,
    onFeedLongClick: (Long) -> Unit,
    onDeleteFeed: (Long) -> Unit,
    onNotificationToggle: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    SwipeActionBox(
        startAction = deleteSwipeAction { onDeleteFeed(feed.id) },
        endAction = notificationsSwipeAction(feed.notificationsEnabled) { onNotificationToggle(feed.id) },
        modifier = modifier
    ) {
        FeedListItem(
            feed = feed,
            isSelected = isSelected,
            onFeedClick = onFeedClick,
            onFeedLongClick = onFeedLongClick
        )
    }
}

@Composable
internal fun deleteSwipeAction(onDelete: () -> Unit) = SwipeAction(
    icon = Icons.Default.Delete,
    label = "Delete",
    containerColor = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.onErrorContainer,
    onSwipe = onDelete
)

/** Toggles notifications, described by what the swipe will do rather than the current state. */
@Composable
internal fun notificationsSwipeAction(enabled: Boolean, onToggle: () -> Unit) = SwipeAction(
    icon = if (enabled) Icons.Default.NotificationsOff else Icons.Default.Notifications,
    label = if (enabled) "Mute" else "Notify",
    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    onSwipe = onToggle
)
