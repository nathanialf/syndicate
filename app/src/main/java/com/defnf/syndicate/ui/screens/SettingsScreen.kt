package com.defnf.syndicate.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
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
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.defnf.syndicate.BuildConfig
import com.defnf.syndicate.data.models.ThemeMode
import com.defnf.syndicate.ui.components.AppIcon
import com.defnf.syndicate.ui.components.ConnectedToggleButtonGroup
import com.defnf.syndicate.ui.components.GroupedListDefaults
import com.defnf.syndicate.ui.components.IconBadge
import com.defnf.syndicate.ui.components.ListSectionHeader
import com.defnf.syndicate.ui.components.ToggleOption
import com.defnf.syndicate.ui.components.groupedItemGap
import com.defnf.syndicate.ui.components.switchRole
import com.defnf.syndicate.ui.viewmodel.ExportState
import com.defnf.syndicate.ui.viewmodel.SettingsViewModel
import com.defnf.syndicate.ui.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
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
                title = { Text(text = "Settings") },
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
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SettingsSection(title = "Notifications") {
                SettingsSwitchItem(
                    title = "New articles",
                    subtitle = "Get notified when your feeds have new articles",
                    icon = Icons.Default.Notifications,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
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
                    },
                    shapes = GroupedListDefaults.toggleShapes(index = 0, count = 2)
                )
                SettingsLinkItem(
                    title = "System notification settings",
                    subtitle = "Sound, vibration and how notifications appear",
                    icon = Icons.Default.Tune,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    onClick = {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        }
                        context.startActivity(intent)
                    },
                    shapes = GroupedListDefaults.shapes(index = 1, count = 2),
                    modifier = Modifier.groupedItemGap(1)
                )
            }

            SettingsSection(title = "Appearance") {
                SettingsChoiceItem(
                    title = "Theme",
                    subtitle = "Choose your preferred color scheme",
                    icon = Icons.Default.Palette,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selected = currentThemeMode,
                    options = listOf(
                        ToggleOption(ThemeMode.SYSTEM, "System", Icons.Default.BrightnessAuto),
                        ToggleOption(ThemeMode.LIGHT, "Light", Icons.Default.LightMode),
                        ToggleOption(ThemeMode.DARK, "Dark", Icons.Default.DarkMode)
                    ),
                    onSelect = themeViewModel::setThemeMode,
                    shapes = GroupedListDefaults.shapes(index = 0, count = 1)
                )
            }

            SettingsSection(title = "Data") {
                val exporting = exportState is ExportState.Loading
                SegmentedListItem(
                    onClick = { settingsViewModel.exportOpml(context) },
                    enabled = !exporting,
                    shapes = GroupedListDefaults.shapes(index = 0, count = 1),
                    colors = GroupedListDefaults.colors(),
                    leadingContent = {
                        val failed = exportState is ExportState.Error
                        IconBadge(
                            icon = when (exportState) {
                                is ExportState.Success -> Icons.Default.CheckCircle
                                is ExportState.Error -> Icons.Default.Error
                                else -> Icons.Default.FileDownload
                            },
                            containerColor = if (failed) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            },
                            contentColor = if (failed) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    },
                    supportingContent = {
                        Text(
                            when (val state = exportState) {
                                is ExportState.Loading -> "Exporting…"
                                is ExportState.Success -> "Export successful"
                                is ExportState.Error -> "Export failed: ${state.message}"
                                else -> "Save your subscriptions to an OPML file"
                            }
                        )
                    },
                    trailingContent = if (exporting) {
                        { LoadingIndicator(modifier = Modifier.size(32.dp)) }
                    } else {
                        null
                    }
                ) {
                    Text("Export OPML")
                }
            }

            SettingsSection(title = "About") {
                AboutCard(
                    onOpenWebsite = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://defnf.com")))
                    }
                )
            }
        }
    }
}

/** A titled group of settings. Wide windows keep the list at a readable width. */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = SettingsMaxWidth)
            .fillMaxWidth()
            .padding(top = 24.dp)
    ) {
        ListSectionHeader(text = title)
        content()
    }
}

private val SettingsMaxWidth = 640.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconContentColor: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier
) {
    // The whole row toggles; the switch only mirrors the state
    SegmentedListItem(
        checked = checked,
        onCheckedChange = onCheckedChange,
        shapes = shapes,
        colors = GroupedListDefaults.toggleColors(),
        leadingContent = { IconBadge(icon = icon, containerColor = iconContainerColor, contentColor = iconContentColor) },
        supportingContent = { Text(subtitle) },
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
        modifier = modifier.switchRole()
    ) {
        Text(title)
    }
}

/** A row that leads outside the app, such as a website or the system settings. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SettingsLinkItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconContentColor: Color,
    onClick: () -> Unit,
    shapes: ListItemShapes,
    modifier: Modifier = Modifier
) {
    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        colors = GroupedListDefaults.colors(),
        leadingContent = { IconBadge(icon = icon, containerColor = iconContainerColor, contentColor = iconContentColor) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Opens outside the app")
        },
        modifier = modifier
    ) {
        Text(title)
    }
}

/**
 * A settings row whose value is picked from a connected button group laid out under the title,
 * so every option is visible and one tap away.
 */
@Composable
private fun <T> SettingsChoiceItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconContainerColor: Color,
    iconContentColor: Color,
    selected: T,
    options: List<ToggleOption<T>>,
    onSelect: (T) -> Unit,
    shapes: ListItemShapes
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = shapes.shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = icon, containerColor = iconContainerColor, contentColor = iconContentColor)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ConnectedToggleButtonGroup(
                options = options,
                selected = selected,
                onSelect = onSelect,
                // Unchecked buttons would take the item's own tone; lift them so they read as buttons
                uncheckedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** The app's icon, name and version, with the developer's link below. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AboutCard(onOpenWebsite: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(GroupedListDefaults.ItemGap)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = GroupedListDefaults.shapes(index = 0, count = 2).shape,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Row(
                modifier = Modifier.padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The real launcher icon, in the expressive squircle
                AppIcon(shape = MaterialShapes.Square.toShape(), size = 64.dp)

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "Syndicate",
                        style = MaterialTheme.typography.headlineMediumEmphasized
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        SettingsLinkItem(
            title = "Developed by Nathanial Fine",
            subtitle = "defnf.com",
            icon = Icons.Default.Person,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            onClick = onOpenWebsite,
            shapes = GroupedListDefaults.shapes(index = 1, count = 2)
        )
    }
}
