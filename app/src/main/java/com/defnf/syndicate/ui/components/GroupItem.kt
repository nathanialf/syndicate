package com.defnf.syndicate.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.data.models.Group

/** A group in a grouped list; a selected group takes the selected tone and shape. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupItem(
    group: Group,
    isSelected: Boolean,
    shapes: ListItemShapes,
    onGroupClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onGroupLongClick: ((Long) -> Unit)? = null
) {
    val haptics = LocalHapticFeedback.current
    SegmentedListItem(
        selected = isSelected,
        onClick = { onGroupClick(group.id) },
        shapes = shapes,
        colors = GroupedListDefaults.colors(),
        onLongClick = onGroupLongClick?.let { onLong ->
            {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLong(group.id)
            }
        },
        onLongClickLabel = if (onGroupLongClick != null) "Options" else null,
        leadingContent = {
            IconBadge(
                icon = Icons.Default.Folder,
                // The selected row takes a strong tone; a surface-colored badge stays clear on it
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                }
            )
        },
        supportingContent = if (group.isDefault) {
            { Text("Default group") }
        } else {
            null
        },
        trailingContent = if (group.notificationsEnabled) {
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
        modifier = modifier
    ) {
        Text(text = group.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
