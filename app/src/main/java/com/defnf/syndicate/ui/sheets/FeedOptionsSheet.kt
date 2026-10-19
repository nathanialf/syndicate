package com.defnf.syndicate.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.defnf.syndicate.data.models.Feed
import com.defnf.syndicate.data.models.Group
import com.defnf.syndicate.ui.components.FaviconIcon
import kotlinx.coroutines.launch

/** Notifications, groups and deletion for one feed, all in one place. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedOptionsSheet(
    feed: Feed,
    groups: List<Group>,
    groupIds: Set<Long>,
    appNotificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    onGroupChange: (groupId: Long, inGroup: Boolean) -> Unit,
    onCreateGroup: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                FaviconIcon(feed = feed, isSelected = false, isAvailable = feed.isAvailable)
                Column {
                    Text(
                        text = feed.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = feed.siteUrl ?: feed.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            SheetSwitchItem(
                index = 0,
                count = 1,
                title = "Notifications",
                subtitle = notificationsSubtitle(appNotificationsEnabled, "Notify me about new articles"),
                icon = Icons.Default.Notifications,
                checked = feed.notificationsEnabled,
                onCheckedChange = onNotificationsChange
            )

            Column {
                SheetSectionTitle("Groups")
                if (groups.isEmpty()) {
                    Text(
                        text = "Groups collect feeds so you can read them together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    groups.forEach { group ->
                        val selected = group.id in groupIds
                        FilterChip(
                            selected = selected,
                            onClick = { onGroupChange(group.id, !selected) },
                            label = { Text(group.name) },
                            leadingIcon = if (selected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                                    )
                                }
                            } else {
                                null
                            }
                        )
                    }
                    NewGroupChip(onCreateGroup = onCreateGroup)
                }
            }

            SheetDeleteButton(
                text = "Delete feed",
                onClick = {
                    // Close the sheet first so the confirmation isn't shown on top of it
                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDelete() }
                }
            )
        }
    }
}

/** A chip that turns into a name field, creating a group with this feed already in it. */
@Composable
private fun NewGroupChip(onCreateGroup: (String) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }

    if (!editing) {
        AssistChip(
            onClick = { editing = true },
            label = { Text("New group") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                )
            }
        )
    } else {
        val focusRequester = remember { FocusRequester() }
        val create = {
            if (name.isNotBlank()) onCreateGroup(name)
            name = ""
            editing = false
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("New group") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { create() }),
            trailingIcon = {
                IconButton(onClick = create, enabled = name.isNotBlank()) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Create group")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .focusRequester(focusRequester)
        )
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}
