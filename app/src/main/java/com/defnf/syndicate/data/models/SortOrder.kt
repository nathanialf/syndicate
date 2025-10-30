package com.defnf.syndicate.data.models

enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST
}

data class ArticleFilter(
    val feedId: Long? = null,
    val groupId: Long? = null,
    val unreadOnly: Boolean = false,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val searchQuery: String? = null
)