package com.syndicate.rssreader.data.remote

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.syndicate.rssreader.data.local.entities.ArticleEntity
import com.syndicate.rssreader.data.local.entities.FeedEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssParser @Inject constructor() {
    
    fun parseFeed(syndFeed: SyndFeed, feedUrl: String): FeedEntity {
        return FeedEntity(
            url = feedUrl,
            title = syndFeed.title ?: "Unknown Feed",
            description = syndFeed.description,
            siteUrl = syndFeed.link,
            faviconUrl = generateFaviconUrl(syndFeed.link, feedUrl),
            lastFetched = System.currentTimeMillis(),
            isAvailable = true
        )
    }
    
    fun generateFaviconUrl(siteUrl: String?, feedUrl: String): String? {
        val baseUrl = siteUrl ?: run {
            // Extract domain from RSS feed URL as fallback
            try {
                val url = java.net.URL(feedUrl)
                "${url.protocol}://${url.host}"
            } catch (e: Exception) {
                null
            }
        }
        return baseUrl?.let { "$it/favicon.ico" }
    }
    
    fun parseArticles(syndFeed: SyndFeed, feedId: Long): List<ArticleEntity> {
        return syndFeed.entries.map { entry ->
            parseArticle(entry, feedId)
        }
    }
    
    private fun parseArticle(entry: SyndEntry, feedId: Long): ArticleEntity {
        return ArticleEntity(
            id = entry.uri ?: entry.link ?: "${feedId}_${entry.title?.hashCode()}",
            feedId = feedId,
            title = entry.title ?: "Untitled",
            description = entry.description?.value,
            url = entry.link ?: "",
            author = entry.author,
            publishedDate = entry.publishedDate?.time,
            thumbnailUrl = extractThumbnailUrl(entry),
            fetchedAt = System.currentTimeMillis()
        )
    }
    
    private fun extractThumbnailUrl(entry: SyndEntry): String? {
        // Try to extract thumbnail from enclosures (media content)
        entry.enclosures.forEach { enclosure ->
            if (enclosure.type?.startsWith("image/") == true) {
                return enclosure.url
            }
        }
        
        // Try to extract from content
        entry.contents.forEach { content ->
            val value = content.value
            if (value != null) {
                val imgRegex = """<img[^>]+src="([^"]+)"""".toRegex()
                val match = imgRegex.find(value)
                if (match != null) {
                    return match.groupValues[1]
                }
            }
        }
        
        return null
    }
}