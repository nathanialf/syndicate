package com.defnf.syndicate.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.defnf.syndicate.BuildConfig
import com.defnf.syndicate.data.models.ThemeMode
import com.defnf.syndicate.ui.components.ConnectedToggleButtonGroup
import com.defnf.syndicate.ui.components.GroupedListDefaults
import com.defnf.syndicate.ui.components.IconBadge
import com.defnf.syndicate.ui.components.ListSectionHeader
import com.defnf.syndicate.ui.components.SegmentedItem
import com.defnf.syndicate.ui.viewmodel.ExportState
import com.defnf.syndicate.ui.viewmodel.SettingsViewModel
import com.defnf.syndicate.ui.viewmodel.ThemeViewModel

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
    // Like the Android Settings app, the bar keeps the page color when content scrolls under it
    val pageColor = GroupedListDefaults.containerColor

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = pageColor,
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageColor,
                    scrolledContainerColor = pageColor
                ),
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsSection(title = "Notifications") {
                SettingsToggleItem(
                    index = 0,
                    count = 2,
                    title = "New articles",
                    subtitle = "Get notified when your feeds have new articles",
                    icon = Icons.Default.Notifications,
                    tone = IconTone.TERTIARY,
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
                SettingsClickableItem(
                    index = 1,
                    count = 2,
                    title = "System notification settings",
                    subtitle = "Sound, vibration and how notifications appear",
                    icon = Icons.Default.Tune,
                    tone = IconTone.TERTIARY,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    },
                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew
                )
            }

            SettingsSection(title = "Appearance") {
                SettingsThemeItem(
                    index = 0,
                    count = 1,
                    selected = currentThemeMode,
                    onSelect = themeViewModel::setThemeMode
                )
            }

            SettingsSection(title = "Data") {
                SettingsListItem(
                    index = 0,
                    count = 1,
                    title = "Export OPML",
                    subtitle = when (val state = exportState) {
                        is ExportState.Loading -> "Exporting…"
                        is ExportState.Success -> "Export successful"
                        is ExportState.Error -> "Export failed: ${state.message}"
                        else -> "Save your subscriptions to an OPML file"
                    },
                    icon = when (exportState) {
                        is ExportState.Success -> Icons.Default.CheckCircle
                        is ExportState.Error -> Icons.Default.Error
                        else -> Icons.Default.FileDownload
                    },
                    tone = if (exportState is ExportState.Error) IconTone.ERROR else IconTone.SECONDARY,
                    modifier = Modifier.clickable(enabled = exportState !is ExportState.Loading) {
                        settingsViewModel.exportOpml(context)
                    },
                    trailingContent = if (exportState is ExportState.Loading) {
                        { LoadingIndicator(modifier = Modifier.size(32.dp)) }
                    } else {
                        null
                    }
                )
            }

            SettingsSection(title = "About") {
                SettingsClickableItem(
                    index = 0,
                    count = 2,
                    title = "Developed by Nathanial Fine",
                    subtitle = "defnf.com",
                    icon = Icons.Default.Person,
                    tone = IconTone.PRIMARY,
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://defnf.com"))
                        context.startActivity(intent)
                    },
                    trailingIcon = Icons.AutoMirrored.Filled.OpenInNew
                )

                SettingsListItem(
                    index = 1,
                    count = 2,
                    title = "Version",
                    subtitle = BuildConfig.VERSION_NAME,
                    icon = Icons.Default.Info,
                    tone = IconTone.PRIMARY
                )
            }
        }
    }
}

/** The tonal colors of an item's icon badge; each section has its own, as in the Android Settings app. */
private enum class IconTone { PRIMARY, SECONDARY, TERTIARY, ERROR }

@Composable
private fun SettingsIcon(icon: ImageVector, tone: IconTone) {
    val colors = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        IconTone.PRIMARY -> colors.primaryContainer to colors.onPrimaryContainer
        IconTone.SECONDARY -> colors.secondaryContainer to colors.onSecondaryContainer
        IconTone.TERTIARY -> colors.tertiaryContainer to colors.onTertiaryContainer
        IconTone.ERROR -> colors.errorContainer to colors.onErrorContainer
    }
    IconBadge(icon = icon, containerColor = container, contentColor = content)
}

/**
 * A titled group of settings, whose items are drawn as one segmented list. Wide windows keep the
 * list at a readable width.
 */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = SettingsMaxWidth)
            .fillMaxWidth()
    ) {
        ListSectionHeader(text = title)
        content()
    }
}

private val SettingsMaxWidth = 640.dp

@Composable
private fun SettingsListItem(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tone: IconTone,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    SegmentedItem(index = index, count = count) {
        SettingsListItemContent(
            title = title,
            subtitle = subtitle,
            icon = icon,
            tone = tone,
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
    tone: IconTone,
    modifier: Modifier = Modifier,
    trailingContent: (@Composable () -> Unit)? = null
) {
    ListItem(
        headlineContent = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        supportingContent = { Text(text = subtitle) },
        leadingContent = { SettingsIcon(icon = icon, tone = tone) },
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = modifier
    )
}

/** Theme choice as a connected button group under its title. */
@Composable
private fun SettingsThemeItem(index: Int, count: Int, selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    SegmentedItem(index = index, count = count) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            SettingsListItemContent(
                title = "Theme",
                subtitle = "Choose your preferred color scheme",
                icon = Icons.Default.Palette,
                tone = IconTone.PRIMARY
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

/** An item that opens something; [trailingIcon] shows where it leads, such as another app. */
@Composable
private fun SettingsClickableItem(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tone: IconTone,
    onClick: () -> Unit,
    trailingIcon: ImageVector? = null
) {
    SettingsListItem(
        index = index,
        count = count,
        title = title,
        subtitle = subtitle,
        icon = icon,
        tone = tone,
        modifier = Modifier.clickable(onClick = onClick),
        trailingContent = trailingIcon?.let {
            {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
private fun SettingsToggleItem(
    index: Int,
    count: Int,
    title: String,
    subtitle: String,
    icon: ImageVector,
    tone: IconTone,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsListItem(
        index = index,
        count = count,
        title = title,
        subtitle = subtitle,
        icon = icon,
        tone = tone,
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
