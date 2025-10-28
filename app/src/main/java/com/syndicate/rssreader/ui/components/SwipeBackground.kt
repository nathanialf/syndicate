package com.syndicate.rssreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SwipeBackground(
    swipeDirection: SwipeToDismissBoxValue,
    modifier: Modifier = Modifier,
    isGroupMode: Boolean = false,
    isNotificationEnabled: Boolean = false
) {
    val backgroundColor = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    
    val iconColor = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.onErrorContainer
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val icon = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Delete
        SwipeToDismissBoxValue.EndToStart -> if (isGroupMode) {
            Icons.Default.Edit
        } else {
            // For feeds: show opposite of current state
            if (isNotificationEnabled) Icons.Default.NotificationsOff else Icons.Default.Notifications
        }
        else -> null
    }
    
    val alignment = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        else -> Alignment.Center
    }

    // Show icon and colors instantly when swiping
    val showIcon = swipeDirection != SwipeToDismissBoxValue.Settled

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment
    ) {
        if (showIcon) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = when (swipeDirection) {
                        SwipeToDismissBoxValue.StartToEnd -> if (isGroupMode) "Delete group" else "Delete feed"
                        SwipeToDismissBoxValue.EndToStart -> if (isGroupMode) {
                            "Edit group"
                        } else {
                            if (isNotificationEnabled) "Disable notifications" else "Enable notifications"
                        }
                        else -> null
                    },
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}