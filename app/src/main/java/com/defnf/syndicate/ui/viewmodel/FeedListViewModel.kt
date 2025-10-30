package com.defnf.syndicate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defnf.syndicate.data.models.Feed
import com.defnf.syndicate.data.models.Group
import com.defnf.syndicate.data.repository.RssRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedListViewModel @Inject constructor(
    private val repository: RssRepository
) : ViewModel() {
    
    init {
        // Update favicon URLs for existing feeds on first load
        viewModelScope.launch {
            try {
                repository.updateFeedFaviconUrls()
            } catch (e: Exception) {
                // Silently handle favicon update errors
            }
        }
    }
    
    val feeds: StateFlow<List<Feed>> = repository.getAllFeeds().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    val groups: StateFlow<List<Group>> = repository.getAllGroups()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private val _showAddFeedDialog = MutableStateFlow(false)
    val showAddFeedDialog: StateFlow<Boolean> = _showAddFeedDialog.asStateFlow()
    
    private val _showAddGroupDialog = MutableStateFlow(false)
    val showAddGroupDialog: StateFlow<Boolean> = _showAddGroupDialog.asStateFlow()
    
    private val _showFeedGroupDialog = MutableStateFlow(false)
    val showFeedGroupDialog: StateFlow<Boolean> = _showFeedGroupDialog.asStateFlow()
    
    private val _selectedFeedForGrouping = MutableStateFlow<Long?>(null)
    val selectedFeedForGrouping: StateFlow<Long?> = _selectedFeedForGrouping.asStateFlow()
    
    private val _feedGroupAssignments = MutableStateFlow<Set<Long>>(emptySet())
    val feedGroupAssignments: StateFlow<Set<Long>> = _feedGroupAssignments.asStateFlow()
    
    private val _showDeleteFeedDialog = MutableStateFlow(false)
    val showDeleteFeedDialog: StateFlow<Boolean> = _showDeleteFeedDialog.asStateFlow()
    
    private val _feedToDelete = MutableStateFlow<Feed?>(null)
    val feedToDelete: StateFlow<Feed?> = _feedToDelete.asStateFlow()
    
    private val _resetSwipeState = MutableStateFlow(false)
    val resetSwipeState: StateFlow<Boolean> = _resetSwipeState.asStateFlow()
    
    private val _showDeleteGroupDialog = MutableStateFlow(false)
    val showDeleteGroupDialog: StateFlow<Boolean> = _showDeleteGroupDialog.asStateFlow()
    
    private val _groupToDelete = MutableStateFlow<Group?>(null)
    val groupToDelete: StateFlow<Group?> = _groupToDelete.asStateFlow()
    
    private val _showEditGroupDialog = MutableStateFlow(false)
    val showEditGroupDialog: StateFlow<Boolean> = _showEditGroupDialog.asStateFlow()
    
    private val _groupToEdit = MutableStateFlow<Group?>(null)
    val groupToEdit: StateFlow<Group?> = _groupToEdit.asStateFlow()
    
    private val _groupEditFeedAssignments = MutableStateFlow<Set<Long>>(emptySet())
    val groupEditFeedAssignments: StateFlow<Set<Long>> = _groupEditFeedAssignments.asStateFlow()
    
    fun showAddFeedDialog() {
        _showAddFeedDialog.value = true
    }
    
    fun hideAddFeedDialog() {
        _showAddFeedDialog.value = false
    }
    
    fun showAddGroupDialog() {
        _showAddGroupDialog.value = true
    }
    
    fun hideAddGroupDialog() {
        _showAddGroupDialog.value = false
    }
    
    fun showFeedGroupDialog(feedId: Long) {
        _selectedFeedForGrouping.value = feedId
        _showFeedGroupDialog.value = true
        loadFeedGroupAssignments(feedId)
    }
    
    fun hideFeedGroupDialog() {
        _showFeedGroupDialog.value = false
        _selectedFeedForGrouping.value = null
        _feedGroupAssignments.value = emptySet()
    }
    
    private fun loadFeedGroupAssignments(feedId: Long) {
        viewModelScope.launch {
            try {
                repository.getGroupsForFeed(feedId).collect { groups ->
                    _feedGroupAssignments.value = groups.map { it.id }.toSet()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load group assignments: ${e.message}"
            }
        }
    }
    
    fun toggleGroupAssignment(groupId: Long) {
        val currentAssignments = _feedGroupAssignments.value.toMutableSet()
        if (currentAssignments.contains(groupId)) {
            currentAssignments.remove(groupId)
        } else {
            currentAssignments.add(groupId)
        }
        _feedGroupAssignments.value = currentAssignments
    }
    
    fun addFeed(url: String) {
        if (url.isBlank()) {
            _errorMessage.value = "Please enter a valid URL"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _successMessage.value = null
            
            val result = repository.addFeedFromUrl(url.trim())
            
            if (result.isSuccess) {
                // Get the feed that was just added to show its title
                val feedId = result.getOrThrow()
                try {
                    val addedFeed = repository.getFeedById(feedId)
                    val message = "Successfully added: ${addedFeed?.title ?: "Feed"}"
                    android.util.Log.d("FeedListViewModel", "Setting success message: $message")
                    _successMessage.value = message
                } catch (e: Exception) {
                    android.util.Log.d("FeedListViewModel", "Setting fallback success message")
                    _successMessage.value = "Feed added successfully"
                }
                _showAddFeedDialog.value = false
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Failed to add feed"
            }
            
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearSuccess() {
        _successMessage.value = null
    }
    
    fun addGroup(name: String) {
        if (name.isBlank()) {
            _errorMessage.value = "Please enter a group name"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                repository.createGroup(name)
                _showAddGroupDialog.value = false
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create group: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    fun toggleFeedGroup(groupId: Long) {
        val currentAssignments = _feedGroupAssignments.value.toMutableSet()
        if (currentAssignments.contains(groupId)) {
            currentAssignments.remove(groupId)
        } else {
            currentAssignments.add(groupId)
        }
        _feedGroupAssignments.value = currentAssignments
    }
    
    fun confirmFeedGroupAssignment() {
        saveFeedGroupAssignments()
    }
    
    fun saveFeedGroupAssignments() {
        val feedId = _selectedFeedForGrouping.value ?: return
        val groupIds = _feedGroupAssignments.value.toList()
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                repository.updateFeedGroups(feedId, groupIds)
                _showFeedGroupDialog.value = false
                _selectedFeedForGrouping.value = null
                _feedGroupAssignments.value = emptySet()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update group assignments: ${e.message}"
            }
            
            _isLoading.value = false
        }
    }
    
    fun showDeleteFeedDialog(feedId: Long) {
        viewModelScope.launch {
            try {
                val feed = repository.getFeedById(feedId)
                if (feed != null) {
                    _feedToDelete.value = feed
                    _showDeleteFeedDialog.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load feed: ${e.message}"
            }
        }
    }
    
    fun hideDeleteFeedDialog() {
        _showDeleteFeedDialog.value = false
        _feedToDelete.value = null
        // Trigger swipe reset when dialog is dismissed (cancelled)
        _resetSwipeState.value = true
        // Reset the flag after a short delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            _resetSwipeState.value = false
        }
    }
    
    fun confirmDeleteFeed() {
        val feed = _feedToDelete.value
        if (feed != null) {
            deleteFeed(feed.id)
            hideDeleteFeedDialog()
        }
    }
    
    private fun deleteFeed(feedId: Long) {
        viewModelScope.launch {
            try {
                val feed = repository.getFeedById(feedId)
                if (feed != null) {
                    repository.deleteFeed(feed)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete feed: ${e.message}"
            }
        }
    }
    
    fun showDeleteGroupDialog(groupId: Long) {
        viewModelScope.launch {
            try {
                val group = repository.getGroupById(groupId)
                if (group != null) {
                    _groupToDelete.value = group
                    _showDeleteGroupDialog.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load group: ${e.message}"
            }
        }
    }
    
    fun hideDeleteGroupDialog() {
        _showDeleteGroupDialog.value = false
        _groupToDelete.value = null
        // Trigger swipe reset when dialog is dismissed (cancelled)
        _resetSwipeState.value = true
        // Reset the flag after a short delay
        viewModelScope.launch {
            kotlinx.coroutines.delay(100)
            _resetSwipeState.value = false
        }
    }
    
    fun confirmDeleteGroup() {
        val group = _groupToDelete.value
        if (group != null) {
            deleteGroup(group.id)
            hideDeleteGroupDialog()
        }
    }
    
    private fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            try {
                val group = repository.getGroupById(groupId)
                if (group != null) {
                    repository.deleteGroup(group)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete group: ${e.message}"
            }
        }
    }
    
    suspend fun getDefaultGroup(): Group? {
        return try {
            repository.getDefaultGroup()
        } catch (e: Exception) {
            _errorMessage.value = "Failed to load default group: ${e.message}"
            null
        }
    }
    
    fun showEditGroupDialog(groupId: Long) {
        viewModelScope.launch {
            try {
                val group = repository.getGroupById(groupId)
                if (group != null) {
                    _groupToEdit.value = group
                    loadGroupFeedAssignments(groupId) {
                        _showEditGroupDialog.value = true
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load group: ${e.message}"
            }
        }
    }
    
    fun hideEditGroupDialog() {
        _showEditGroupDialog.value = false
        _groupToEdit.value = null
        _groupEditFeedAssignments.value = emptySet()
    }
    
    private fun loadGroupFeedAssignments(groupId: Long, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val feedsInGroup = repository.getFeedsForGroup(groupId)
                _groupEditFeedAssignments.value = feedsInGroup.map { it.id }.toSet()
                onComplete?.invoke()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load feeds for group: ${e.message}"
                onComplete?.invoke()
            }
        }
    }
    
    fun toggleGroupEditFeedAssignment(feedId: Long) {
        val currentAssignments = _groupEditFeedAssignments.value.toMutableSet()
        if (currentAssignments.contains(feedId)) {
            currentAssignments.remove(feedId)
        } else {
            currentAssignments.add(feedId)
        }
        _groupEditFeedAssignments.value = currentAssignments
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
                repository.updateGroupFeeds(groupId, _groupEditFeedAssignments.value.toList())
                hideEditGroupDialog()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update group: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun toggleFeedNotifications(feedId: Long) {
        viewModelScope.launch {
            try {
                val feed = repository.getFeedById(feedId)
                if (feed != null) {
                    repository.updateFeedNotifications(feedId, !feed.notificationsEnabled)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update feed notifications: ${e.message}"
            }
        }
    }
}