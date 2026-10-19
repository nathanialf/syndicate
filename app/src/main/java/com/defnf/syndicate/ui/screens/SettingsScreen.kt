package com.defnf.syndicate.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.defnf.syndicate.ui.components.ConnectedToggleButtonGroup
import com.defnf.syndicate.ui.components.SegmentedItem
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.defnf.syndicate.data.models.ThemeMode
import com.defnf.syndicate.ui.viewmodel.ThemeViewModel
import com.defnf.syndicate.ui.viewmodel.SettingsViewModel
import com.defnf.syndicate.ui.viewmodel.ExportState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val currentThemeMode by themeViewModel.themeMode.collectAsState()
    val exportState by settingsViewModel.exportState.collectAsState()
    val notificationsEnabled by settingsViewModel.notificationsEnabled.collectAsState()
    val context = LocalContext.current
    val notificationManager = NotificationManagerCompat.from(context)
    
    // Launcher for requesting notification permission (API 33+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            settingsViewModel.toggleNotifications(true)
        }
    }
    
    // Clear export state after showing success/error for 3 seconds
    LaunchedEffect(exportState) {
        when (exportState) {
            is ExportState.Success, is ExportState.Error -> {
                kotlinx.coroutines.delay(3000)
                settingsViewModel.clearExportState()
            }
            else -> {}
        }
    }
    
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(text = "Settings")
                },
                navigationIcon = {
                    if (onBackClick != null) {
                        IconButton(onClick = onBackClick, shapes = IconButtonDefaults.shapes()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Notifications Section  
            SettingsSection(title = "Notifications") {
                SettingsToggleItem(
                    index = 0,
                    count = 1,
                    title = "Enable Notifications",
                    subtitle = "Receive notifications for new articles",
                    icon = Icons.Default.Notifications,
                    checked = notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && !notificationManager.areNotificationsEnabled()) {
                            // Request notification permission using system dialog
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                // For older versions, open app settings
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                        } else {
                            settingsViewModel.toggleNotifications(enabled)
                        }
                    }
                )
            }
            
            // Data Section
            SettingsSection(title = "Data") {
                SettingsClickableItem(
                    index = 0,
                    count = 1,
                    title = "Export OPML",
                    subtitle = when (val state = exportState) {
                        is ExportState.Loading -> "Exporting..."
                        is ExportState.Success -> "Export successful!"
                        is ExportState.Error -> "Export failed: ${state.message}"
                        else -> "Export your feeds to an OPML file"
                    },
                    icon = when (exportState) {
                        is ExportState.Loading -> Icons.Default.Sync
                        is ExportState.Success -> Icons.Default.CheckCircle
                        is ExportState.Error -> Icons.Default.Error
                        else -> Icons.Default.FileDownload
                    },
                    onClick = {
                        if (exportState !is ExportState.Loading) {
                            settingsViewModel.exportOpml(context)
                        }
                    }
                )
            }
            
            // Appearance Section
            SettingsSection(title = "Appearance") {
                SettingsThemeItem(
                    index = 0,
                    count = 1,
                    selected = currentThemeMode,
                    onSelect = themeViewModel::setThemeMode
                )
            }
            
            // About Section
            SettingsSection(title = "About") {
                SettingsClickableItem(
                    index = 0,
                    count = 2,
                    title = "Developed by Nathanial Fine",
                    subtitle = "defnf.com",
                    icon = Icons.Default.Person,
                    onClick = { 
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://defnf.com"))
                        context.startActivity(intent)
                    }
                )
                
                SettingsItem(
                    index = 1,
                    count = 2,
                    title = "Version",
                    subtitle = "1.0.0",
                    icon = Icons.Default.Info
                )
            }
        }
    }
}

/** A titled group of settings, whose items are drawn as one segmented list. */
@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingsListItem(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    SegmentedItem(index = index, count = count) {
        SettingsListItemContent(
            title = title,
            subtitle = subtitle,
            icon = icon,
            modifier = modifier,
            trailingContent = trailingContent
        )
    }
}

/** The title, subtitle and icon row shared by every settings item. */
@Composable
private fun SettingsListItemContent(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
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
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
    )
}

/** Theme choice as a connected button group under its title. */
@Composable
private fun SettingsThemeItem(
    index: Int,
    count: Int,
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    SegmentedItem(index = index, count = count) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            SettingsListItemContent(
                title = "Theme",
                subtitle = "Choose your preferred color scheme",
                icon = Icons.Default.Palette
            )
            ConnectedToggleButtonGroup(
                options = listOf(
                    ThemeMode.SYSTEM to "System",
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.DARK to "Dark"
                ),
                selected = selected,
                onSelect = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
private fun SettingsClickableItem(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    SettingsListItem(
        index = index,
        count = count,
        title = title,
        subtitle = subtitle,
        icon = icon,
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun SettingsItem(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    SettingsListItem(
        index = index,
        count = count,
        title = title,
        subtitle = subtitle,
        icon = icon
    )
}

@Composable
private fun SettingsToggleItem(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsListItem(
        index = index,
        count = count,
        title = title,
        subtitle = subtitle,
        icon = icon,
        modifier = Modifier.toggleable(
            value = checked,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ),
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
        }
    )
}
