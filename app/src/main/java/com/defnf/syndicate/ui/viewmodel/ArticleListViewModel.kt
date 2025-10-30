package com.defnf.syndicate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defnf.syndicate.data.models.Article
import com.defnf.syndicate.data.models.ArticleFilter
import com.defnf.syndicate.data.models.Feed
import com.defnf.syndicate.data.repository.RssRepository
import com.defnf.syndicate.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ArticleShowFilter {
    ALL, UNREAD, READ
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repository: RssRepository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {
    
    private val _feedId = MutableStateFlow<Long?>(null)
    private val _groupId = MutableStateFlow<Long?>(null)
    private val _showFilter = MutableStateFlow(ArticleShowFilter.UNREAD) // Default to unread only
    
    val articles: StateFlow<List<Article>> = combine(_feedId, _groupId, _showFilter) { feedId, groupId, showFilter ->
            Triple(feedId, groupId, showFilter)
        }
        .flatMapLatest { (feedId, groupId, showFilter) ->
            repository.getArticles(
                ArticleFilter(
                    feedId = feedId,
                    unreadOnly = when (showFilter) {
                        ArticleShowFilter.ALL -> false
                        ArticleShowFilter.UNREAD -> true
                        ArticleShowFilter.READ -> false // We'll filter read articles separately
                    },
                    searchQuery = null,
                    groupId = groupId
                )
            ).map { articleList ->
                val filteredList = when (showFilter) {
                    ArticleShowFilter.ALL -> articleList
                    ArticleShowFilter.UNREAD -> articleList.filter { !it.isRead }
                    ArticleShowFilter.READ -> articleList.filter { it.isRead }
                }
                android.util.Log.d("ArticleListViewModel", "Articles updated: ${filteredList.size} articles (filter: $showFilter)")
                filteredList
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )
    
    private val _currentFeed = MutableStateFlow<Feed?>(null)
    val currentFeed: StateFlow<Feed?> = _currentFeed.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _isStartupSyncing = MutableStateFlow(true) // Start with true to show sync on app launch
    val isStartupSyncing: StateFlow<Boolean> = _isStartupSyncing.asStateFlow()
    
    private val _shouldScrollToTop = MutableStateFlow(false)
    val shouldScrollToTop: StateFlow<Boolean> = _shouldScrollToTop.asStateFlow()
    
    val showFilter: StateFlow<ArticleShowFilter> = _showFilter.asStateFlow()
    
    // Combined state for showing refresh indicator (either manual refresh or startup sync)
    val isRefreshingOrStartup: StateFlow<Boolean> = combine(_isRefreshing, _isStartupSyncing) { isRefreshing, isStartupSyncing ->
        isRefreshing || isStartupSyncing
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = true
    )
    
    init {
        // Clear startup sync after a reasonable delay to simulate sync completion
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // 3 seconds delay for startup sync
            _isStartupSyncing.value = false
        }
    }
    
    fun setFeedId(feedId: Long?) {
        _feedId.value = feedId
        _groupId.value = null // Clear group when setting feed
        loadFeedInfo(feedId)
    }
    
    fun setGroupId(groupId: Long?) {
        _groupId.value = groupId
        _feedId.value = null // Clear feed when setting group
        loadGroupInfo(groupId)
    }
    
    private fun loadFeedInfo(feedId: Long?) {
        viewModelScope.launch {
            _isLoading.value = true
            feedId?.let { id ->
                _currentFeed.value = repository.getFeedById(id)
            } ?: run {
                _currentFeed.value = null
            }
            _isLoading.value = false
        }
    }
    
    private fun loadGroupInfo(groupId: Long?) {
        viewModelScope.launch {
            _isLoading.value = true
            groupId?.let { id ->
                // For groups, we'll create a virtual feed with the group name
                val group = repository.getGroupById(id)
                _currentFeed.value = group?.let { 
                    Feed(
                        id = -id, // Negative ID to distinguish from real feeds
                        url = "",
                        title = it.name,
                        description = "Articles from ${it.name} group",
                        siteUrl = null,
                        faviconUrl = null,
                        lastFetched = null,
                        isAvailable = true,
                        notificationsEnabled = false,
                        createdAt = System.currentTimeMillis()
                    )
                }
            } ?: run {
                _currentFeed.value = null
            }
            _isLoading.value = false
        }
    }
    
    fun markAsRead(articleId: String) {
        viewModelScope.launch {
            repository.markAsRead(articleId, true)
        }
    }
    
    fun markAsUnread(articleId: String) {
        viewModelScope.launch {
            repository.markAsRead(articleId, false)
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            when {
                _feedId.value != null -> {
                    // Mark all articles in the current feed as read
                    repository.markAllAsReadForFeed(_feedId.value!!)
                }
                _groupId.value != null -> {
                    // Mark all articles in the current group as read
                    repository.markAllAsReadForGroup(_groupId.value!!)
                }
                else -> {
                    // Mark all articles as read
                    repository.markAllAsRead()
                }
            }
        }
    }
    
    fun setShowFilter(filter: ArticleShowFilter) {
        _showFilter.value = filter
    }
    
    suspend fun getDefaultGroup(): com.defnf.syndicate.data.models.Group? {
        return try {
            repository.getDefaultGroup()
        } catch (e: Exception) {
            null
        }
    }
    
    fun refreshFeeds() {
        viewModelScope.launch {
            _isRefreshing.value = true
            
            // Use targeted sync based on current context
            when {
                _feedId.value != null -> {
                    // Sync only the current feed
                    syncScheduler.triggerManualSyncForFeed(_feedId.value!!)
                }
                _groupId.value != null -> {
                    // Sync all feeds in the current group
                    syncScheduler.triggerManualSyncForGroup(_groupId.value!!)
                }
                else -> {
                    // Sync all feeds (default behavior)
                    syncScheduler.triggerManualSync()
                }
            }
            
            // Wait a brief moment to ensure sync is started, then monitor for completion
            kotlinx.coroutines.delay(500)
            
            // Reset refresh state after a reasonable time
            // In a real implementation, you'd monitor the WorkManager for completion
            kotlinx.coroutines.delay(3000)
            _isRefreshing.value = false
            
            // Trigger scroll to top to show new articles
            _shouldScrollToTop.value = true
        }
    }
    
    fun onScrollToTopHandled() {
        _shouldScrollToTop.value = false
    }
}