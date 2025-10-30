package com.defnf.syndicate.data.models

data class Article(
    val id: String, // GUID from feed
    val feedId: Long,
    val feedTitle: String,
    val feedFaviconUrl: String?,
    val title: String,
    val description: String?,
    val url: String,
    val author: String?,
    val publishedDate: Long?,
    val thumbnailUrl: String?,
    val isRead: Boolean,
    val readAt: Long?
)