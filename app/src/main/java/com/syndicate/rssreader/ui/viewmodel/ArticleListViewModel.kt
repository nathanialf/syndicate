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
    
    val articles: StateFlow<List<Article>> = _feedId
        .flatMapLatest { feedId ->
            repository.getArticles(
                ArticleFilter(
                    feedId = feedId,
                    unreadOnly = false,
                    searchQuery = null,
                    groupId = null
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
        loadFeedInfo(feedId)
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
}