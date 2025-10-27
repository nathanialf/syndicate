package com.syndicate.rssreader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.syndicate.rssreader.data.models.Group
import com.syndicate.rssreader.data.models.Feed
import com.syndicate.rssreader.data.repository.RssRepository
import javax.inject.Inject

@HiltViewModel
class GroupManagementViewModel @Inject constructor(
    private val repository: RssRepository
) : ViewModel() {
    
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()
    
    private val _feeds = MutableStateFlow<List<Feed>>(emptyList())
    val feeds: StateFlow<List<Feed>> = _feeds.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _showCreateGroupDialog = MutableStateFlow(false)
    val showCreateGroupDialog: StateFlow<Boolean> = _showCreateGroupDialog.asStateFlow()
    
    private val _showEditGroupDialog = MutableStateFlow(false)
    val showEditGroupDialog: StateFlow<Boolean> = _showEditGroupDialog.asStateFlow()
    
    private val _selectedGroup = MutableStateFlow<Group?>(null)
    val selectedGroup: StateFlow<Group?> = _selectedGroup.asStateFlow()
    
    private val _selectedFeedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedFeedIds: StateFlow<Set<Long>> = _selectedFeedIds.asStateFlow()
    
    init {
        loadGroups()
        loadFeeds()
    }
    
    private fun loadGroups() {
        viewModelScope.launch {
            repository.getAllGroups()
                .catch { e ->
                    _errorMessage.value = "Failed to load groups: ${e.message}"
                }
                .collect { groupEntities ->
                    _groups.value = groupEntities.map { entity ->
                        Group(
                            id = entity.id,
                            name = entity.name,
                            isDefault = entity.isDefault,
                            notificationsEnabled = entity.notificationsEnabled,
                            createdAt = entity.createdAt,
                            feedCount = 0, // Will be calculated below
                            unreadCount = 0 // Will be calculated below
                        )
                    }
                    
                    // Update feed counts for each group
                    _groups.value = _groups.value.map { group ->
                        val feedCount = try {
                            repository.getFeedCountForGroup(group.id)
                        } catch (e: Exception) {
                            0
                        }
                        group.copy(feedCount = feedCount)
                    }
                }
        }
    }
    
    private fun loadFeeds() {
        viewModelScope.launch {
            repository.getAllFeeds()
                .catch { e ->
                    _errorMessage.value = "Failed to load feeds: ${e.message}"
                }
                .collect { feeds ->
                    _feeds.value = feeds
                }
        }
    }
    
    fun showCreateGroupDialog() {
        _showCreateGroupDialog.value = true
        _selectedFeedIds.value = emptySet()
    }
    
    fun hideCreateGroupDialog() {
        _showCreateGroupDialog.value = false
        _selectedFeedIds.value = emptySet()
    }
    
    fun showEditGroupDialog(group: Group) {
        _selectedGroup.value = group
        loadFeedsForGroup(group.id) {
            _showEditGroupDialog.value = true
        }
    }
    
    fun hideEditGroupDialog() {
        _showEditGroupDialog.value = false
        _selectedGroup.value = null
        _selectedFeedIds.value = emptySet()
    }
    
    private fun loadFeedsForGroup(groupId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val feedsInGroup = repository.getFeedsForGroup(groupId)
                _selectedFeedIds.value = feedsInGroup.map { it.id }.toSet()
                onComplete?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load feeds for group: ${e.message}"
                onComplete?.invoke() // Still show dialog even if loading fails
            }
        }
    }
    
    fun toggleFeedSelection(feedId: Long) {
        val currentSelection = _selectedFeedIds.value.toMutableSet()
        if (currentSelection.contains(feedId)) {
            currentSelection.remove(feedId)
        } else {
            currentSelection.add(feedId)
        }
        _selectedFeedIds.value = currentSelection
    }
    
    fun createGroup(name: String, isDefault: Boolean = false, notificationsEnabled: Boolean = false) {
        if (name.isBlank()) {
            _errorMessage.value = "Group name cannot be empty"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val groupId = repository.createGroup(name, isDefault, notificationsEnabled)
                if (_selectedFeedIds.value.isNotEmpty()) {
                    repository.updateFeedGroups(
                        feedIds = _selectedFeedIds.value.toList(),
                        groupIds = listOf(groupId)
                    )
                }
                hideCreateGroupDialog()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create group: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateGroup(groupId: Long, name: String, isDefault: Boolean, notificationsEnabled: Boolean) {
        if (name.isBlank()) {
            _errorMessage.value = "Group name cannot be empty"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateGroup(groupId, name, isDefault, notificationsEnabled)
                repository.updateGroupFeeds(groupId, _selectedFeedIds.value.toList())
                hideEditGroupDialog()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update group: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteGroup(group: Group) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deleteGroup(group.id)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete group: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun setDefaultGroup(groupId: Long) {
        viewModelScope.launch {
            try {
                repository.setDefaultGroup(groupId)
            } catch (e: Exception) {
                _errorMessage.value = "Failed to set default group: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}