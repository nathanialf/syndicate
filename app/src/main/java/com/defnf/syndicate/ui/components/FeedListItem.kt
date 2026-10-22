package com.defnf.syndicate.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.data.models.Feed

/** A feed in a grouped list; a selected feed takes the selected tone and shape. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeedListItem(
    feed: Feed,
    isSelected: Boolean,
    shapes: ListItemShapes,
    onFeedClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onFeedLongClick: ((Long) -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    // Pre-calculate expensive values to avoid recomputation during scroll
    val description = remember(feed.description, feed.title) {
        feed.description?.takeIf { it != feed.title && it.isNotBlank() }
    }

    SegmentedListItem(
        selected = isSelected,
        onClick = { onFeedClick(feed.id) },
        shapes = shapes,
        colors = GroupedListDefaults.colors(),
        onLongClick = onFeedLongClick?.let { onLong ->
            {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLong(feed.id)
            }
        },
        onLongClickLabel = if (onFeedLongClick != null) "Options" else null,
        leadingContent = {
            FaviconIcon(
                feed = feed,
                isSelected = isSelected,
                isAvailable = feed.isAvailable
            )
        },
        supportingContent = description?.let { text ->
            { Text(text = text, maxLines = 2, overflow = TextOverflow.Ellipsis) }
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
        } else {
            null
        },
        modifier = modifier.navigationItemRole()
    ) {
        Text(text = feed.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
