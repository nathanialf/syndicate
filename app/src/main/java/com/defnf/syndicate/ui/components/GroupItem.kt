package com.defnf.syndicate.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.data.models.Group

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupItem(
    group: Group,
    onGroupClick: (Long) -> Unit,
    onGroupLongClick: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    ListItem(
        headlineContent = {
            Text(
                text = group.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        },
        leadingContent = {
            IconBadge(
                icon = Icons.Default.Folder,
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
                shape = MaterialShapes.Cookie4Sided.toShape()
            )
        },
        supportingContent = if (group.isDefault) {
            {
                Text(
                    text = "Default group",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    }
                )
            }
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
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = GroupedListDefaults.itemColor(isSelected)
        ),
        modifier = modifier
            .combinedClickable(
                onClick = { onGroupClick(group.id) },
                onLongClickLabel = if (onGroupLongClick != null) "Options" else null,
                onLongClick = onGroupLongClick?.let { onLong ->
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLong(group.id)
                    }
                }
            )
            .fillMaxWidth()
    )
}
