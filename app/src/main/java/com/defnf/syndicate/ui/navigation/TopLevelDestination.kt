package com.defnf.syndicate.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations shown in the bottom navigation bar / navigation rail.
 * In multi-pane layouts the feeds list is always visible as a sidebar, so only
 * [ARTICLES] and [SETTINGS] are reachable there.
 */
enum class TopLevelDestination(
    val label: String,
    val icon: ImageVector
) {
    ARTICLES("Articles", Icons.AutoMirrored.Filled.Article),
    FEEDS("Feeds", Icons.Default.RssFeed),
    SETTINGS("Settings", Icons.Default.Settings)
}
