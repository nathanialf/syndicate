package com.syndicate.rssreader.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syndicate.rssreader.data.export.OpmlExporter
import com.syndicate.rssreader.data.preferences.NotificationPreferences
import com.syndicate.rssreader.data.repository.RssRepository
import com.syndicate.rssreader.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: RssRepository,
    private val opmlExporter: OpmlExporter,
    private val notificationPreferences: NotificationPreferences,
    private val syncScheduler: SyncScheduler
) : ViewModel() {
    
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()
    
    val notificationsEnabled: StateFlow<Boolean> = notificationPreferences.notificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )
    
    fun exportOpml(context: Context) {
        viewModelScope.launch {
            _exportState.value = ExportState.Loading
            
            try {
                // Collect all feeds and groups
                val feeds = repository.getAllFeeds().first()
                val groups = repository.getAllGroups().first()
                
                // Build map of feed ID to groups
                val feedGroupMap = mutableMapOf<Long, List<com.syndicate.rssreader.data.models.Group>>()
                feeds.forEach { feed ->
                    val feedGroups = repository.getGroupsForFeed(feed.id).first()
                    feedGroupMap[feed.id] = feedGroups
                }
                
                // Export to OPML
                val result = opmlExporter.exportToOpml(context, feeds, groups, feedGroupMap)
                
                if (result.isSuccess) {
                    val file = result.getOrThrow()
                    _exportState.value = ExportState.Success(file)
                    
                    // Share the file
                    opmlExporter.shareOpmlFile(context, file)
                } else {
                    _exportState.value = ExportState.Error(
                        result.exceptionOrNull()?.message ?: "Unknown error occurred"
                    )
                }
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    fun clearExportState() {
        _exportState.value = ExportState.Idle
    }
    
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            notificationPreferences.setNotificationsEnabled(enabled)
            if (enabled) {
                syncScheduler.schedulePeriodSync()
            } else {
                syncScheduler.cancelSync()
            }
        }
    }
}

sealed class ExportState {
    object Idle : ExportState()
    object Loading : ExportState()
    data class Success(val file: File) : ExportState()
    data class Error(val message: String) : ExportState()
}