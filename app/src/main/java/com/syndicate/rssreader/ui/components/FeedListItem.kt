package com.syndicate.rssreader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.syndicate.rssreader.data.models.Feed

@Composable
fun FeedListItem(
    feed: Feed,
    isSelected: Boolean,
    onFeedClick: (Long) -> Unit,
    isSidebarMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Pre-calculate expensive values to avoid recomputation during scroll
    val truncatedDescription = remember(feed.description) {
        feed.description?.takeIf { it != feed.title && it.isNotBlank() }?.let { description ->
            if (description.length > 80) {
                description.take(80) + "..."
            } else {
                description
            }
        }
    }
    
    val headlineColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    
    val supportingColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    ListItem(
        headlineContent = {
            Text(
                text = feed.title,
                style = MaterialTheme.typography.titleMedium,
                color = headlineColor
            )
        },
        supportingContent = truncatedDescription?.let { description ->
            {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = supportingColor
                )
            }
        },
        leadingContent = {
            FaviconIcon(
                feed = feed,
                isSelected = isSelected,
                isAvailable = feed.isAvailable
            )
        },
        trailingContent = if (feed.notificationsEnabled) {
            {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notifications enabled",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        } else null,
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = containerColor
        ),
        modifier = modifier
            .clickable { onFeedClick(feed.id) }
            .fillMaxWidth()
    )
}