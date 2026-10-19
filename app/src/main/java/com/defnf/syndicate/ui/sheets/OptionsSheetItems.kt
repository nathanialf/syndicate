package com.defnf.syndicate.ui.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.ui.components.SegmentedItem

/*
 * Building blocks shared by the feed and group options sheets. Everything in the sheets is saved as
 * soon as it changes, so there are no save or cancel buttons.
 */

/** Grouped items inside a sheet sit on a slightly raised color so they stand out from the sheet. */
@Composable
internal fun sheetItemColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

@Composable
internal fun SheetSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
    )
}

/** A segmented row with a switch; the whole row toggles it. */
@Composable
internal fun SheetSwitchItem(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SegmentedItem(index = index, count = count, color = sheetItemColor()) {
        ListItem(
            headlineContent = { Text(text = title) },
            supportingContent = { Text(text = subtitle) },
            leadingContent = {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingContent = {
                Switch(
                    checked = checked,
                    onCheckedChange = null,
                    thumbContent = if (checked) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize)
                            )
                        }
                    } else {
                        null
                    }
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            modifier = Modifier.toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
        )
    }
}

@Composable
internal fun SheetDeleteButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.IconSize)
        )
        Text(text = text, modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
    }
}

/** Explains why a notification switch won't do anything yet, or what it does. */
internal fun notificationsSubtitle(appNotificationsEnabled: Boolean, description: String): String =
    if (appNotificationsEnabled) description else "Notifications are turned off in Settings"
