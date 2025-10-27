package com.syndicate.rssreader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.syndicate.rssreader.data.models.Feed

@Composable
fun FeedListItem(
    feed: Feed,
    isSelected: Boolean,
    onFeedClick: (Long) -> Unit,
    isSidebarMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = feed.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        supportingContent = feed.description?.takeIf { it != feed.title && it.isNotBlank() }?.let { description ->
            {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
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
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = modifier
            .clickable { onFeedClick(feed.id) }
            .fillMaxWidth()
    )
}