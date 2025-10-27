package com.syndicate.rssreader.data.models

import com.syndicate.rssreader.data.local.entities.FeedEntity

data class Feed(
    val id: Long,
    val url: String,
    val title: String,
    val description: String?,
    val siteUrl: String?,
    val faviconUrl: String?,
    val lastFetched: Long?,
    val isAvailable: Boolean,
    val createdAt: Long
) {
    fun toEntity(): FeedEntity = FeedEntity(
        id = id,
        url = url,
        title = title,
        description = description,
        siteUrl = siteUrl,
        faviconUrl = faviconUrl,
        lastFetched = lastFetched,
        isAvailable = isAvailable,
        createdAt = createdAt
    )
}