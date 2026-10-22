package com.defnf.syndicate.ui.sheets

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.ui.components.GroupedListDefaults
import com.defnf.syndicate.ui.components.IconBadge
import com.defnf.syndicate.ui.components.ListSectionHeader
import com.defnf.syndicate.ui.components.groupedItemGap
import com.defnf.syndicate.ui.components.switchRole

/*
 * Building blocks shared by the feed and group options sheets. Everything in the sheets is saved as
 * soon as it changes, so there are no save or cancel buttons.
 */

/** Grouped items inside a sheet sit on a slightly raised color so they stand out from the sheet. */
@Composable
internal fun sheetItemColor(): Color = MaterialTheme.colorScheme.surfaceContainerHigh

@Composable
internal fun SheetSectionTitle(text: String, modifier: Modifier = Modifier) {
    ListSectionHeader(text = text, modifier = modifier.padding(top = 8.dp))
}

/** Sheets open fully expanded and close in one step. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberOptionsSheetState(): SheetState =
    rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded))

/** A segmented row with a switch; the whole row toggles it. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
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
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = GroupedListDefaults.toggleShapes(index, count),
        colors = GroupedListDefaults.toggleColors(sheetItemColor()),
        leadingContent = {
            IconBadge(
                icon = icon,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        },
        supportingContent = { Text(text = subtitle) },
        trailingContent = {
            Switch(
                checked = checked,
                // The whole row toggles the switch
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
        modifier = Modifier
            .groupedItemGap(index)
            .switchRole()
    ) {
        Text(text = title)
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
