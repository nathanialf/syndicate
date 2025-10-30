package com.defnf.syndicate.data.remote

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
            android.util.Log.d("RssFetcher", "Fetching feed from: $url")
            val feedUrl = URL(url)
            val input = SyndFeedInput()
            feedUrl.openStream().use { inputStream ->
                val feed = input.build(XmlReader(inputStream))
                android.util.Log.d("RssFetcher", "Successfully fetched feed: ${feed.title} with ${feed.entries?.size ?: 0} entries")
                Result.success(feed)
            }
        } catch (e: Exception) {
            android.util.Log.e("RssFetcher", "Failed to fetch feed from $url: ${e.message}", e)
            Result.failure(e)
        }
    }
}