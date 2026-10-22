package com.defnf.syndicate.ui.sheets

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.data.models.Feed
import com.defnf.syndicate.data.models.Group
import com.defnf.syndicate.ui.components.FaviconIcon
import com.defnf.syndicate.ui.components.GroupedListDefaults
import com.defnf.syndicate.ui.components.groupedItemGap
import kotlinx.coroutines.launch

/** Name, notifications, default and feeds of one group, all in one place. */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GroupOptionsSheet(
    group: Group,
    feeds: List<Feed>,
    feedIds: Set<Long>,
    appNotificationsEnabled: Boolean,
    onRename: (String) -> Unit,
    onDefaultChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onFeedChange: (feedId: Long, inGroup: Boolean) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberOptionsSheetState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var name by rememberSaveable(group.id) { mutableStateOf(group.name) }
    val saveName = {
        if (name.isNotBlank() && name.trim() != group.name) onRename(name)
    }

    ModalBottomSheet(
        onDismissRequest = {
            // The name is saved when the sheet closes, as well as from the keyboard
            saveName()
            onDismiss()
        },
        sheetState = sheetState
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            item(key = "name") {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group name") },
                    singleLine = true,
                    isError = name.isBlank(),
                    supportingText = if (name.isBlank()) {
                        { Text("A group needs a name") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        saveName()
                        focusManager.clearFocus()
                    }),
                    textStyle = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
            }

            item(key = "notifications") {
                SheetSwitchItem(
                    index = 0,
                    count = 2,
                    title = "Notifications",
                    subtitle = notificationsSubtitle(
                        appNotificationsEnabled,
                        "A summary when its feeds have new articles"
                    ),
                    icon = Icons.Default.Notifications,
                    checked = group.notificationsEnabled,
                    onCheckedChange = onNotificationsChange
                )
            }
            item(key = "default") {
                SheetSwitchItem(
                    index = 1,
                    count = 2,
                    title = "Open on launch",
                    subtitle = "Start in this group instead of all articles",
                    icon = Icons.Default.Star,
                    checked = group.isDefault,
                    onCheckedChange = onDefaultChange
                )
            }

            item(key = "feeds_title") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    SheetSectionTitle("Feeds", modifier = Modifier.weight(1f))
                    if (feeds.isNotEmpty()) {
                        Text(
                            text = "${feedIds.size} of ${feeds.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
            }
            if (feeds.isEmpty()) {
                item(key = "no_feeds") {
                    Text(
                        text = "Add some feeds, then choose which belong in this group.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
            }
            itemsIndexed(feeds, key = { _, feed -> "feed_${feed.id}" }) { index, feed ->
                val inGroup = feed.id in feedIds
                SegmentedListItem(
                    checked = inGroup,
                    onCheckedChange = { onFeedChange(feed.id, it) },
                    shapes = GroupedListDefaults.shapes(index, feeds.size),
                    colors = GroupedListDefaults.toggleColors(sheetItemColor()),
                    leadingContent = {
                        FaviconIcon(
                            feed = feed,
                            isSelected = false,
                            isAvailable = feed.isAvailable,
                            size = 32.dp
                        )
                    },
                    trailingContent = { Checkbox(checked = inGroup, onCheckedChange = null) },
                    modifier = Modifier.groupedItemGap(index)
                ) {
                    Text(text = feed.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            item(key = "delete") {
                Spacer(modifier = Modifier.height(16.dp))
                SheetDeleteButton(
                    text = "Delete group",
                    onClick = {
                        // Keep a rename even if the deletion is then cancelled
                        saveName()
                        // Close the sheet first so the confirmation isn't shown on top of it
                        scope.launch { sheetState.hide() }.invokeOnCompletion { onDelete() }
                    }
                )
            }
        }
    }
}
