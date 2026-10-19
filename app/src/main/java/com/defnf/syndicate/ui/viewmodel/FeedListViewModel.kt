package com.defnf.syndicate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defnf.syndicate.data.models.Feed
import com.defnf.syndicate.data.models.Group
import com.defnf.syndicate.data.preferences.NotificationPreferences
import com.defnf.syndicate.data.repository.RssRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A snackbar message, optionally with an action such as undoing what was just done. */
data class UserMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedListViewModel @Inject constructor(
    private val repository: RssRepository,
    notificationPreferences: NotificationPreferences
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

    /** The app-wide notifications setting; per-feed and per-group notifications only apply while it's on. */
    val appNotificationsEnabled: StateFlow<Boolean> = notificationPreferences.notificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _message = MutableStateFlow<UserMessage?>(null)
    val message: StateFlow<UserMessage?> = _message.asStateFlow()

    private val _showAddFeedDialog = MutableStateFlow(false)
    val showAddFeedDialog: StateFlow<Boolean> = _showAddFeedDialog.asStateFlow()

    private val _showAddGroupDialog = MutableStateFlow(false)
    val showAddGroupDialog: StateFlow<Boolean> = _showAddGroupDialog.asStateFlow()

    // Options sheets: changes made in them are saved straight away, so they only need to know which item is open
    private val _feedOptionsId = MutableStateFlow<Long?>(null)
    val feedOptionsId: StateFlow<Long?> = _feedOptionsId.asStateFlow()

    /** Ids of the groups the feed whose options are open belongs to. */
    val feedOptionsGroupIds: StateFlow<Set<Long>> = _feedOptionsId
        .flatMapLatest { feedId ->
            if (feedId == null) {
                flowOf(emptySet())
            } else {
                repository.getGroupsForFeed(feedId).map { groups -> groups.map { it.id }.toSet() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _groupOptionsId = MutableStateFlow<Long?>(null)
    val groupOptionsId: StateFlow<Long?> = _groupOptionsId.asStateFlow()

    /** Ids of the feeds in the group whose options are open. */
    val groupOptionsFeedIds: StateFlow<Set<Long>> = _groupOptionsId
        .flatMapLatest { groupId ->
            if (groupId == null) {
                flowOf(emptySet())
            } else {
                repository.getFeedsByGroup(groupId).map { feeds -> feeds.map { it.id }.toSet() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val _feedToDelete = MutableStateFlow<Feed?>(null)
    val feedToDelete: StateFlow<Feed?> = _feedToDelete.asStateFlow()

    private val _groupToDelete = MutableStateFlow<Group?>(null)
    val groupToDelete: StateFlow<Group?> = _groupToDelete.asStateFlow()

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

    fun clearMessage() {
        _message.value = null
    }

    private fun showError(text: String) {
        _message.value = UserMessage(text)
    }

    fun addFeed(url: String) {
        if (url.isBlank()) {
            showError("Please enter a valid URL")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true

            val result = repository.addFeedFromUrl(url.trim())

            if (result.isSuccess) {
                val feedId = result.getOrThrow()
                val title = try {
                    repository.getFeedById(feedId)?.title
                } catch (e: Exception) {
                    null
                }
                _showAddFeedDialog.value = false
                // Offer to set up notifications and groups for the new feed straight away
                _message.value = UserMessage(
                    text = "Added ${title ?: "feed"}",
                    actionLabel = "Options",
                    onAction = { showFeedOptions(feedId) }
                )
            } else {
                showError(result.exceptionOrNull()?.message ?: "Failed to add feed")
            }

            _isLoading.value = false
        }
    }

    fun addGroup(name: String) {
        if (name.isBlank()) {
            showError("Please enter a group name")
            return
        }

        viewModelScope.launch {
            try {
                val groupId = repository.createGroup(name.trim())
                _showAddGroupDialog.value = false
                // A new group is empty, so go straight on to choosing its feeds
                _groupOptionsId.value = groupId
            } catch (e: Exception) {
                showError("Failed to create group: ${e.message}")
            }
        }
    }

    // Feed options

    fun showFeedOptions(feedId: Long) {
        _groupOptionsId.value = null
        _feedOptionsId.value = feedId
    }

    fun hideFeedOptions() {
        _feedOptionsId.value = null
    }

    fun setFeedNotifications(feedId: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateFeedNotifications(feedId, enabled)
            } catch (e: Exception) {
                showError("Failed to update feed notifications: ${e.message}")
            }
        }
    }

    /** Toggles a feed's notifications from a swipe, confirming the new state with a way to undo it. */
    fun toggleFeedNotifications(feedId: Long) {
        viewModelScope.launch {
            try {
                // Read from the database rather than the list, which may not yet reflect a toggle just made
                val feed = repository.getFeedById(feedId) ?: return@launch
                val enabled = !feed.notificationsEnabled
                repository.updateFeedNotifications(feedId, enabled)
                _message.value = notificationsToggledMessage(feed.title, enabled) {
                    setFeedNotifications(feedId, !enabled)
                }
            } catch (e: Exception) {
                showError("Failed to update feed notifications: ${e.message}")
            }
        }
    }

    fun setFeedInGroup(feedId: Long, groupId: Long, inGroup: Boolean) {
        viewModelScope.launch {
            try {
                repository.setFeedInGroup(feedId, groupId, inGroup)
            } catch (e: Exception) {
                showError("Failed to update groups: ${e.message}")
            }
        }
    }

    /** Creates a group from the feed options and puts the feed in it. */
    fun createGroupWithFeed(feedId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                val groupId = repository.createGroup(name.trim())
                repository.setFeedInGroup(feedId, groupId, inGroup = true)
            } catch (e: Exception) {
                showError("Failed to create group: ${e.message}")
            }
        }
    }

    // Group options

    fun showGroupOptions(groupId: Long) {
        _feedOptionsId.value = null
        _groupOptionsId.value = groupId
    }

    fun hideGroupOptions() {
        _groupOptionsId.value = null
    }

    fun renameGroup(groupId: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try {
                repository.renameGroup(groupId, name.trim())
            } catch (e: Exception) {
                showError("Failed to rename group: ${e.message}")
            }
        }
    }

    fun setDefaultGroup(groupId: Long, isDefault: Boolean) {
        viewModelScope.launch {
            try {
                repository.setGroupDefault(groupId, isDefault)
            } catch (e: Exception) {
                showError("Failed to update default group: ${e.message}")
            }
        }
    }

    fun setGroupNotifications(groupId: Long, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateGroupNotifications(groupId, enabled)
            } catch (e: Exception) {
                showError("Failed to update group notifications: ${e.message}")
            }
        }
    }

    /** Toggles a group's notifications from a swipe, confirming the new state with a way to undo it. */
    fun toggleGroupNotifications(groupId: Long) {
        viewModelScope.launch {
            try {
                // Read from the database rather than the list, which may not yet reflect a toggle just made
                val group = repository.getGroupById(groupId) ?: return@launch
                val enabled = !group.notificationsEnabled
                repository.updateGroupNotifications(groupId, enabled)
                _message.value = notificationsToggledMessage(group.name, enabled) {
                    setGroupNotifications(groupId, !enabled)
                }
            } catch (e: Exception) {
                showError("Failed to update group notifications: ${e.message}")
            }
        }
    }

    private fun notificationsToggledMessage(name: String, enabled: Boolean, undo: () -> Unit): UserMessage {
        val text = when {
            !enabled -> "Notifications off for $name"
            // Turning a feed on does nothing while notifications are off for the whole app, so say so
            !appNotificationsEnabled.value -> "Notifications on for $name, but they're turned off in Settings"
            else -> "Notifications on for $name"
        }
        return UserMessage(text = text, actionLabel = "Undo", onAction = undo)
    }

    // Deleting asks for confirmation first

    fun requestDeleteFeed(feedId: Long) {
        viewModelScope.launch {
            try {
                _feedToDelete.value = feeds.value.find { it.id == feedId } ?: repository.getFeedById(feedId)
            } catch (e: Exception) {
                showError("Failed to load feed: ${e.message}")
            }
        }
    }

    fun cancelDeleteFeed() {
        _feedToDelete.value = null
    }

    fun confirmDeleteFeed() {
        val feed = _feedToDelete.value ?: return
        _feedToDelete.value = null
        if (_feedOptionsId.value == feed.id) _feedOptionsId.value = null
        viewModelScope.launch {
            try {
                repository.deleteFeed(feed)
            } catch (e: Exception) {
                showError("Failed to delete feed: ${e.message}")
            }
        }
    }

    fun requestDeleteGroup(groupId: Long) {
        viewModelScope.launch {
            try {
                _groupToDelete.value = groups.value.find { it.id == groupId } ?: repository.getGroupById(groupId)
            } catch (e: Exception) {
                showError("Failed to load group: ${e.message}")
            }
        }
    }

    fun cancelDeleteGroup() {
        _groupToDelete.value = null
    }

    fun confirmDeleteGroup() {
        val group = _groupToDelete.value ?: return
        _groupToDelete.value = null
        if (_groupOptionsId.value == group.id) _groupOptionsId.value = null
        viewModelScope.launch {
            try {
                // By id, so the feeds are released, its notification is dismissed and a default is cleared
                repository.deleteGroup(group.id)
            } catch (e: Exception) {
                showError("Failed to delete group: ${e.message}")
            }
        }
    }

    suspend fun getDefaultGroup(): Group? {
        return try {
            repository.getDefaultGroup()
        } catch (e: Exception) {
            showError("Failed to load default group: ${e.message}")
            null
        }
    }
}
