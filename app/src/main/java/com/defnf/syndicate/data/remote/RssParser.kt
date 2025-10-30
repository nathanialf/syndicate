package com.defnf.syndicate.data.remote

import com.rometools.rome.feed.synd.SyndEntry
import com.rometools.rome.feed.synd.SyndFeed
import com.defnf.syndicate.data.local.entities.ArticleEntity
import com.defnf.syndicate.data.local.entities.FeedEntity
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
    
    fun parseArticles(syndFeed: SyndFeed, feedId: Long, feedUrl: String): List<ArticleEntity> {
        return syndFeed.entries.map { entry ->
            parseArticle(entry, feedId, feedUrl)
        }
    }
    
    private fun parseArticle(entry: SyndEntry, feedId: Long, feedUrl: String): ArticleEntity {
        val articleUrl = resolveUrl(entry.link ?: "", feedUrl)
        return ArticleEntity(
            id = entry.uri ?: entry.link ?: "${feedId}_${entry.title?.hashCode()}",
            feedId = feedId,
            title = entry.title ?: "Untitled",
            description = entry.description?.value,
            url = articleUrl,
            author = entry.author,
            publishedDate = entry.publishedDate?.time,
            thumbnailUrl = extractThumbnailUrl(entry),
            fetchedAt = System.currentTimeMillis()
        )
    }
    
    private fun resolveUrl(url: String, feedUrl: String): String {
        if (url.isBlank()) return ""
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url // Already absolute
        }
        
        try {
            val baseUrl = java.net.URL(feedUrl)
            val baseHost = "${baseUrl.protocol}://${baseUrl.host}${if (baseUrl.port != -1 && baseUrl.port != baseUrl.defaultPort) ":${baseUrl.port}" else ""}"
            
            return if (url.startsWith("/")) {
                baseHost + url // Absolute path
            } else {
                "$baseHost/$url" // Relative path
            }
        } catch (e: Exception) {
            return url // Return original if URL parsing fails
        }
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