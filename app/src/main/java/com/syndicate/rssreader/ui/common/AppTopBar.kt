package com.syndicate.rssreader.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.syndicate.rssreader.ui.theme.CormorantGaramond

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    onBackClick: () -> Unit = {},
    showSettingsButton: Boolean = false,
    onSettingsClick: () -> Unit = {},
    useSystemBarInsets: Boolean = false,
    onTitleClick: (() -> Unit)? = null,
    showMarkAllAsReadButton: Boolean = false,
    onMarkAllAsReadClick: () -> Unit = {},
    customActions: (@Composable () -> Unit)? = null
) {
    CenterAlignedTopAppBar(
        title = { 
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = if (onTitleClick != null) {
                    Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onTitleClick() }
                } else {
                    Modifier
                }
            ) {
                Text(
                    text = "Syndicate",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = CormorantGaramond,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        navigationIcon = if (showBackButton) {
            {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        } else {
            {}
        },
        actions = {
            customActions?.invoke()
            if (showMarkAllAsReadButton) {
                IconButton(onClick = onMarkAllAsReadClick) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Mark all as read"
                    )
                }
            }
            if (showSettingsButton) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        },
        windowInsets = if (useSystemBarInsets) {
            androidx.compose.foundation.layout.WindowInsets.systemBars
        } else {
            androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        }
    )
}