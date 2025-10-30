package com.defnf.syndicate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defnf.syndicate.data.models.Article
import com.defnf.syndicate.data.repository.RssRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(
    private val repository: RssRepository
) : ViewModel() {
    
    private val _article = MutableStateFlow<Article?>(null)
    val article: StateFlow<Article?> = _article.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    fun loadArticle(articleId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get article from repository
                val articleResult = repository.getArticleById(articleId)
                _article.value = articleResult
                
                // Mark as read when viewed
                if (articleResult != null) {
                    repository.markAsRead(articleId, true)
                }
            } catch (e: Exception) {
                _article.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}