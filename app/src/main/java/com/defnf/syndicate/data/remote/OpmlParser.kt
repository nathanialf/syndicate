package com.defnf.syndicate.data.remote

import com.defnf.syndicate.data.models.OpmlFeed
import com.defnf.syndicate.data.models.OpmlImportResult
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpmlParser @Inject constructor() {
    
    fun parseOpml(inputStream: InputStream): OpmlImportResult {
        val feeds = mutableListOf<OpmlFeed>()
        val groups = mutableSetOf<String>()
        
        try {
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val parser = factory.newPullParser()
            parser.setInput(inputStream, null)
            
            var eventType = parser.eventType
            var currentGroupName: String? = null
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        when (parser.name.lowercase()) {
                            "outline" -> {
                                val type = parser.getAttributeValue(null, "type")
                                val xmlUrl = parser.getAttributeValue(null, "xmlUrl")
                                val htmlUrl = parser.getAttributeValue(null, "htmlUrl")
                                val text = parser.getAttributeValue(null, "text")
                                val title = parser.getAttributeValue(null, "title")
                                
                                when {
                                    // RSS feed entry
                                    !xmlUrl.isNullOrBlank() -> {
                                        val feedTitle = title ?: text ?: "Untitled Feed"
                                        val feedUrl = xmlUrl.trim()
                                        
                                        if (feedUrl.isNotBlank()) {
                                            feeds.add(
                                                OpmlFeed(
                                                    url = feedUrl,
                                                    title = feedTitle,
                                                    description = htmlUrl,
                                                    groupName = currentGroupName
                                                )
                                            )
                                            
                                            currentGroupName?.let { groups.add(it) }
                                        }
                                    }
                                    // Group/folder entry
                                    !text.isNullOrBlank() && xmlUrl.isNullOrBlank() -> {
                                        currentGroupName = text.trim()
                                    }
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (parser.name.lowercase() == "outline") {
                            // Reset group when closing an outline that was a group
                            val hasNestedOutlines = feeds.any { it.groupName == currentGroupName }
                            if (!hasNestedOutlines) {
                                currentGroupName = null
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            throw OpmlParseException("Failed to parse OPML file: ${e.message}", e)
        } finally {
            inputStream.close()
        }
        
        return OpmlImportResult(
            feedsToImport = feeds,
            groupsToCreate = groups.toList(),
            totalFeeds = feeds.size
        )
    }
}

class OpmlParseException(message: String, cause: Throwable? = null) : Exception(message, cause)