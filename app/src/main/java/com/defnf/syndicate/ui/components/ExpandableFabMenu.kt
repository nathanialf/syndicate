package com.defnf.syndicate.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex

private data class FabMenuAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

/**
 * The feeds screen's Material 3 Expressive FAB menu: a toggle FAB that morphs into a close button
 * and reveals the add actions above it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpandableFabMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddFeed: () -> Unit,
    onImportFeed: () -> Unit,
    onAddGroup: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Back closes the menu before it leaves the screen
    BackHandler(enabled = expanded, onBack = onToggle)

    val actions = listOf(
        FabMenuAction(Icons.Default.Folder, "Add Group", onAddGroup),
        FabMenuAction(Icons.Default.Download, "Import Feeds", onImportFeed),
        FabMenuAction(Icons.Default.RssFeed, "Add Feed", onAddFeed)
    )

    FloatingActionButtonMenu(
        expanded = expanded,
        modifier = modifier,
        button = {
            ToggleFloatingActionButton(
                checked = expanded,
                onCheckedChange = { onToggle() },
                modifier = Modifier.semantics {
                    // Reached before the menu items, which are laid out above it
                    traversalIndex = -1f
                    stateDescription = if (expanded) "Expanded" else "Collapsed"
                    contentDescription = "Add menu"
                }
            ) {
                Icon(
                    imageVector = if (checkedProgress > 0.5f) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.animateIcon({ checkedProgress })
                )
            }
        }
    ) {
        actions.forEach { action ->
            FloatingActionButtonMenuItem(
                onClick = action.onClick,
                icon = { Icon(imageVector = action.icon, contentDescription = null) },
                text = { Text(text = action.label) }
            )
        }
    }
}
