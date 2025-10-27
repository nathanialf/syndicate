package com.syndicate.rssreader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syndicate.rssreader.data.models.Article
import com.syndicate.rssreader.data.models.ArticleFilter
import com.syndicate.rssreader.data.models.Feed
import com.syndicate.rssreader.data.repository.RssRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ArticleListViewModel @Inject constructor(
    private val repository: RssRepository
) : ViewModel() {
    
    private val _feedId = MutableStateFlow<Long?>(null)
    private val _groupId = MutableStateFlow<Long?>(null)
    
    val articles: StateFlow<List<Article>> = combine(_feedId, _groupId) { feedId, groupId ->
            Pair(feedId, groupId)
        }
        .flatMapLatest { (feedId, groupId) ->
            repository.getArticles(
                ArticleFilter(
                    feedId = feedId,
                    unreadOnly = false,
                    searchQuery = null,
                    groupId = groupId
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _currentFeed = MutableStateFlow<Feed?>(null)
    val currentFeed: StateFlow<Feed?> = _currentFeed.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
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
    
    suspend fun getDefaultGroup(): com.syndicate.rssreader.data.models.Group? {
        return try {
            repository.getDefaultGroup()
        } catch (e: Exception) {
            null
        }
    }
}