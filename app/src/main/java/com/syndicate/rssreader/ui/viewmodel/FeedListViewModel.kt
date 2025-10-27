package com.syndicate.rssreader.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.syndicate.rssreader.data.models.Feed
import com.syndicate.rssreader.data.repository.RssRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedListViewModel @Inject constructor(
    private val repository: RssRepository
) : ViewModel() {
    
    val feeds: StateFlow<List<Feed>> = repository.getAllFeeds().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _showAddFeedDialog = MutableStateFlow(false)
    val showAddFeedDialog: StateFlow<Boolean> = _showAddFeedDialog.asStateFlow()
    
    fun showAddFeedDialog() {
        _showAddFeedDialog.value = true
    }
    
    fun hideAddFeedDialog() {
        _showAddFeedDialog.value = false
    }
    
    fun addFeed(url: String) {
        if (url.isBlank()) {
            _errorMessage.value = "Please enter a valid URL"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = repository.addFeedFromUrl(url.trim())
            
            if (result.isSuccess) {
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
    
    fun deleteFeed(feedId: Long) {
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
}