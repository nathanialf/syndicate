package com.syndicate.rssreader.data.remote

import com.rometools.rome.feed.synd.SyndFeed
import com.rometools.rome.io.SyndFeedInput
import com.rometools.rome.io.XmlReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssFetcher @Inject constructor() {
    
    suspend fun fetchFeed(url: String): Result<SyndFeed> = withContext(Dispatchers.IO) {
        try {
            val feedUrl = URL(url)
            val input = SyndFeedInput()
            feedUrl.openStream().use { inputStream ->
                val feed = input.build(XmlReader(inputStream))
                Result.success(feed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}