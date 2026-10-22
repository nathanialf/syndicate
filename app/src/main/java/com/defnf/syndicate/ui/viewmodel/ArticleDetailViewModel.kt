package com.defnf.syndicate.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defnf.syndicate.data.models.Article
import com.defnf.syndicate.data.repository.RssRepository
import com.defnf.syndicate.ui.reader.ArticleContent
import com.defnf.syndicate.ui.reader.ArticleContentParser
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ArticleDetailViewModel @Inject constructor(private val repository: RssRepository) : ViewModel() {

    private val _article = MutableStateFlow<Article?>(null)
    val article: StateFlow<Article?> = _article.asStateFlow()

    /** The article's body, parsed for the reader. */
    private val _content = MutableStateFlow<ArticleContent?>(null)
    val content: StateFlow<ArticleContent?> = _content.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // The view model outlives the article shown (the detail pane reuses it), so a load still
    // running for the previous article must not overwrite the new one
    private var loadJob: Job? = null

    fun loadArticle(articleId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                // Get article from repository
                val articleResult = repository.getArticleById(articleId)
                _content.value = articleResult?.description?.let { html ->
                    withContext(Dispatchers.Default) {
                        // A body that fails to parse still leaves the title and the link to the article
                        runCatching {
                            ArticleContentParser.parse(
                                html = html,
                                baseUrl = articleResult.url,
                                headerImageUrl = articleResult.thumbnailUrl,
                                title = articleResult.title
                            )
                        }.getOrNull()
                    }
                }
                _article.value = articleResult

                // Mark as read when viewed
                if (articleResult != null) {
                    repository.markAsRead(articleId, true)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _article.value = null
                _content.value = null
            } finally {
                // A cancelled load leaves the state to the load that replaced it
                if (isActive) _isLoading.value = false
            }
        }
    }
}
