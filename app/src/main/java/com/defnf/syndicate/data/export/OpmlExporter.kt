package com.defnf.syndicate.data.export

import com.defnf.syndicate.data.models.Feed
import com.defnf.syndicate.data.models.Group
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpmlExporter @Inject constructor() {
    
    suspend fun exportToOpml(
        context: Context,
        feeds: List<Feed>,
        groups: List<Group>,
        feedGroupMap: Map<Long, List<Group>>
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val fileName = "rss_feeds_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.opml"
            val file = File(context.getExternalFilesDir(null), fileName)
            
            FileWriter(file).use { writer ->
                writer.write(generateOpmlContent(feeds, groups, feedGroupMap))
            }
            
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateOpmlContent(
        feeds: List<Feed>,
        groups: List<Group>,
        feedGroupMap: Map<Long, List<Group>>
    ): String {
        val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        val currentDate = dateFormat.format(Date())
        
        val sb = StringBuilder()
        sb.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        sb.appendLine("<opml version=\"1.0\">")
        sb.appendLine("  <head>")
        sb.appendLine("    <title>RSS Feeds Export</title>")
        sb.appendLine("    <dateCreated>$currentDate</dateCreated>")
        sb.appendLine("    <dateModified>$currentDate</dateModified>")
        sb.appendLine("    <ownerName>Syndicate RSS Reader</ownerName>")
        sb.appendLine("  </head>")
        sb.appendLine("  <body>")
        
        // Group feeds by their groups
        val groupedFeeds = mutableMapOf<Long?, MutableList<Feed>>()
        
        // Initialize all groups
        groups.forEach { group ->
            groupedFeeds[group.id] = mutableListOf()
        }
        
        // Add ungrouped feeds
        groupedFeeds[null] = mutableListOf()
        
        // Categorize feeds
        feeds.forEach { feed ->
            val feedGroups = feedGroupMap[feed.id] ?: emptyList()
            if (feedGroups.isEmpty()) {
                // Ungrouped feed
                groupedFeeds[null]?.add(feed)
            } else {
                // Add to each group the feed belongs to
                feedGroups.forEach { group ->
                    groupedFeeds[group.id]?.add(feed)
                }
            }
        }
        
        // Write ungrouped feeds first
        groupedFeeds[null]?.forEach { feed ->
            sb.appendLine(generateFeedOutline(feed))
        }
        
        // Write grouped feeds
        groups.forEach { group ->
            val groupFeeds = groupedFeeds[group.id] ?: emptyList()
            if (groupFeeds.isNotEmpty()) {
                sb.appendLine("    <outline text=\"${escapeXml(group.name)}\" title=\"${escapeXml(group.name)}\">")
                groupFeeds.forEach { feed ->
                    sb.appendLine("      ${generateFeedOutline(feed)}")
                }
                sb.appendLine("    </outline>")
            }
        }
        
        sb.appendLine("  </body>")
        sb.appendLine("</opml>")
        
        return sb.toString()
    }
    
    private fun generateFeedOutline(feed: Feed): String {
        val title = escapeXml(feed.title)
        val url = escapeXml(feed.url)
        val siteUrl = feed.siteUrl?.let { escapeXml(it) } ?: url
        val description = feed.description?.let { escapeXml(it) } ?: ""
        
        return "    <outline type=\"rss\" text=\"$title\" title=\"$title\" xmlUrl=\"$url\" htmlUrl=\"$siteUrl\" description=\"$description\"/>"
    }
    
    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
    }
    
    fun shareOpmlFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/xml"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "RSS Feeds Export")
            putExtra(Intent.EXTRA_TEXT, "Exported RSS feeds from Syndicate RSS Reader")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share OPML file"))
    }
}