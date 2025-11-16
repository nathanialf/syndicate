package com.defnf.syndicate.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.defnf.syndicate.data.models.Article
import com.defnf.syndicate.ui.common.LayoutConstants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import android.net.Uri
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableArticleCard(
    article: Article,
    onArticleClick: (Article) -> Unit,
    onToggleReadState: (Article) -> Unit,
    resetSwipe: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val swipeState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.4f }
    )
    
    // Handle swipe actions
    LaunchedEffect(swipeState.currentValue) {
        when (swipeState.currentValue) {
            SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> {
                if (swipeState.targetValue != SwipeToDismissBoxValue.Settled) {
                    // Toggle read state on swipe in either direction
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onToggleReadState(article)
                    swipeState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }
            SwipeToDismissBoxValue.Settled -> { /* No action needed */ }
        }
    }
    
    // Reset swipe state when requested
    LaunchedEffect(resetSwipe) {
        if (resetSwipe) {
            swipeState.snapTo(SwipeToDismissBoxValue.Settled)
        }
    }
    
    SwipeToDismissBox(
        state = swipeState,
        backgroundContent = {
            ArticleSwipeBackground(
                swipeDirection = swipeState.dismissDirection,
                isRead = article.isRead
            )
        },
        modifier = modifier
    ) {
        Card(
            onClick = { 
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onArticleClick(article) 
            },
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Headline image if available
                article.thumbnailUrl?.let { thumbnailUrl ->
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = "Article image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                
                // Title with greyed text for read articles
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.Medium,
                    color = if (article.isRead) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Description with rich content
                article.description?.let { description ->
                    RichContentPreview(
                        html = description,
                        isRead = article.isRead,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Feed info and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use the reusable FaviconIcon with a mock feed object
                    FaviconIcon(
                        feed = com.defnf.syndicate.data.models.Feed(
                            id = 0,
                            url = "",
                            title = "",
                            description = null,
                            siteUrl = null,
                            faviconUrl = article.feedFaviconUrl,
                            lastFetched = null,
                            isAvailable = true,
                            createdAt = 0L
                        ),
                        isSelected = false,
                        isAvailable = true,
                        size = 16.dp
                    )
                    
                    Text(
                        text = buildString {
                            append(article.feedTitle)
                            article.publishedDate?.let { date ->
                                append(" • ")
                                append(formatDate(date))
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (article.isRead) {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleSwipeBackground(
    swipeDirection: SwipeToDismissBoxValue,
    isRead: Boolean
) {
    val backgroundColor = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> {
            if (isRead) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.primaryContainer
        }
        SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
    }
    
    val iconColor = when (swipeDirection) {
        SwipeToDismissBoxValue.StartToEnd, SwipeToDismissBoxValue.EndToStart -> {
            if (isRead) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onPrimaryContainer
        }
        SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.onSurface
    }
    
    val icon = if (isRead) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle
    val text = if (isRead) "Mark as Unread" else "Mark as Read"
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 24.dp),
        contentAlignment = when (swipeDirection) {
            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
            SwipeToDismissBoxValue.Settled -> Alignment.Center
        }
    ) {
        if (swipeDirection != SwipeToDismissBoxValue.Settled) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = iconColor,
                modifier = Modifier.size(LayoutConstants.SwipeIconSize)
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    
    calendar.time = date
    val articleYear = calendar.get(Calendar.YEAR)
    
    val dateFormat = if (articleYear == currentYear) {
        SimpleDateFormat("MMM dd", Locale.getDefault())
    } else {
        SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    }
    
    return dateFormat.format(date)
}

@Composable
private fun RichContentPreview(
    html: String,
    isRead: Boolean,
    maxLines: Int,
    modifier: Modifier = Modifier
) {
    val contentElements = parseHtmlToElements(html)
    
    if (contentElements.isNotEmpty()) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            var lineCount = 0
            for (element in contentElements) {
                if (lineCount >= maxLines) break
                
                when (element) {
                    is ContentElement.TextContent -> {
                        if (element.content.trim().isNotBlank()) {
                            Text(
                                text = element.content,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isRead) {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                                },
                                maxLines = maxLines - lineCount,
                                overflow = TextOverflow.Ellipsis
                            )
                            lineCount += minOf(element.content.split('\n').size, maxLines - lineCount)
                        }
                    }
                    is ContentElement.ImageContent -> {
                        if (lineCount < maxLines) {
                            AsyncImage(
                                model = element.src,
                                contentDescription = element.alt,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            lineCount += 2 // Count image as taking 2 lines worth of space
                        }
                    }
                    is ContentElement.CodeBlockContent -> {
                        if (lineCount < maxLines) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (isRead) {
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = element.code.take(100) + if (element.code.length > 100) "..." else "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = if (isRead) {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            lineCount += 2 // Count code block as taking 2 lines worth of space
                        }
                    }
                    is ContentElement.YouTubeEmbedContent -> {
                        if (lineCount < maxLines) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .background(
                                        color = if (isRead) {
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        } else {
                                            MaterialTheme.colorScheme.primaryContainer
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = getYouTubeThumbnailUrl(element.videoId),
                                    contentDescription = element.title ?: "YouTube video",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                    alpha = if (isRead) 0.7f else 1.0f
                                )
                                // Play icon overlay
                                Icon(
                                    imageVector = Icons.Default.RssFeed, // Using RSS icon as play placeholder
                                    contentDescription = "Play video",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(32.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.6f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .padding(8.dp)
                                )
                            }
                            lineCount += 2 // Count YouTube embed as taking 2 lines worth of space
                        }
                    }
                }
            }
        }
    }
}

private sealed class ContentElement {
    data class TextContent(val content: String) : ContentElement()
    data class ImageContent(val src: String, val alt: String?) : ContentElement()
    data class CodeBlockContent(val code: String, val language: String?) : ContentElement()
    data class YouTubeEmbedContent(val videoId: String, val title: String?) : ContentElement()
}

private fun parseHtmlToElements(html: String): List<ContentElement> {
    val elements = mutableListOf<ContentElement>()
    var currentPosition = 0
    val htmlLength = html.length
    
    // Find all special elements (images, code blocks, and YouTube embeds) and their positions
    val imageRegex = """<img[^>]+src=['"](.*?)['"][^>]*(?:alt=['"](.*?)['"])?[^>]*>""".toRegex(setOf(RegexOption.IGNORE_CASE))
    val codeBlockRegex = """<pre[^>]*><code[^>]*>(.*?)</code></pre>|<pre[^>]*>(.*?)</pre>|<code[^>]*>(.*?)</code>""".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val youtubeRegex = """<iframe[^>]+src=['"](https://www\.youtube\.com/embed/([^'"?&]+))[^'"]*['"][^>]*(?:title=['"](.*?)['"])?[^>]*>.*?</iframe>""".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    
    val imageMatches = imageRegex.findAll(html).map { "image" to it }.toList()
    val codeMatches = codeBlockRegex.findAll(html).map { "code" to it }.toList()
    val youtubeMatches = youtubeRegex.findAll(html).map { "youtube" to it }.toList()
    
    // Combine and sort all matches by position
    val allMatches = (imageMatches + codeMatches + youtubeMatches).sortedBy { it.second.range.first }
    
    for ((type, match) in allMatches) {
        // Add text content before this element
        if (match.range.first > currentPosition) {
            val textContent = html.substring(currentPosition, match.range.first)
            val processedText = preprocessUnorderedLists(textContent)
            val cleanText = parseHtmlText(processedText)
            if (cleanText.trim().isNotBlank()) {
                elements.add(ContentElement.TextContent(cleanText))
            }
        }
        
        when (type) {
            "image" -> {
                val src = match.groupValues[1]
                val alt = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
                elements.add(ContentElement.ImageContent(src, alt))
            }
            "code" -> {
                // Extract code content from whichever group matched
                val codeContent = when {
                    match.groupValues[1].isNotBlank() -> match.groupValues[1] // <pre><code>
                    match.groupValues[2].isNotBlank() -> match.groupValues[2] // <pre>
                    match.groupValues[3].isNotBlank() -> match.groupValues[3] // <code>
                    else -> ""
                }
                if (codeContent.isNotBlank()) {
                    // Preserve newlines by replacing them with placeholders before HtmlCompat processing
                    val withPlaceholders = codeContent
                        .replace("\n", "___NEWLINE___")
                        .replace("\r\n", "___NEWLINE___")
                        .replace("\r", "___NEWLINE___")
                    
                    // Use HtmlCompat to strip HTML tags but preserve content
                    val htmlStripped = HtmlCompat.fromHtml(withPlaceholders, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
                    
                    // Restore newlines and clean up
                    val cleanCode = htmlStripped
                        .replace("___NEWLINE___", "\n")
                        .trim()
                    
                    elements.add(ContentElement.CodeBlockContent(cleanCode, null))
                }
            }
            "youtube" -> {
                val videoId = match.groupValues[2] // Second capture group is the video ID
                val title = match.groupValues.getOrNull(3)?.takeIf { it.isNotBlank() }
                elements.add(ContentElement.YouTubeEmbedContent(videoId, title))
            }
        }
        
        currentPosition = match.range.last + 1
    }
    
    // Add remaining text content after the last element (only if we processed some special elements)
    if (currentPosition < htmlLength && allMatches.isNotEmpty()) {
        val textContent = html.substring(currentPosition)
        val processedText = preprocessUnorderedLists(textContent)
        val cleanText = parseHtmlText(processedText)
        if (cleanText.trim().isNotBlank()) {
            elements.add(ContentElement.TextContent(cleanText))
        }
    }
    
    // If no special elements were found, treat the entire content as text
    if (allMatches.isEmpty()) {
        val processedText = preprocessUnorderedLists(html)
        val cleanText = parseHtmlText(processedText)
        if (cleanText.trim().isNotBlank()) {
            elements.add(ContentElement.TextContent(cleanText))
        }
    }
    
    return elements
}

private fun getYouTubeThumbnailUrl(videoId: String): String {
    // YouTube thumbnail quality hierarchy (best to worst):
    // maxresdefault.jpg - 1920x1080 (if available)
    // sddefault.jpg - 640x480 
    // hqdefault.jpg - 480x360
    // mqdefault.jpg - 320x180
    // default.jpg - 120x90
    
    // Try maxresdefault first (highest quality), fallback handled by Coil if not available
    return "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
}

private fun parseHtmlText(html: String): String {
    // Preprocess HTML to handle special elements before HtmlCompat processing
    var processedHtml = html
    // Note: Images will become OBJ characters, same as original behavior
    processedHtml = preprocessHyperlinks(processedHtml)
    processedHtml = preprocessUnorderedLists(processedHtml)
    return HtmlCompat.fromHtml(processedHtml, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
}

private fun preprocessImages(html: String): String {
    var processedHtml = html
    
    // Find all <img> tags and replace them with readable text placeholders
    val imgRegex = """<img[^>]+src=['"](.*?)['"][^>]*(?:alt=['"](.*?)['"])?[^>]*>""".toRegex(setOf(RegexOption.IGNORE_CASE))
    
    processedHtml = imgRegex.replace(processedHtml) { matchResult ->
        val src = matchResult.groupValues[1]
        val alt = matchResult.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
        
        // Create a readable placeholder for the image
        val placeholder = when {
            alt != null -> "[Image: $alt]"
            src.isNotBlank() -> {
                // Extract filename from URL for better context
                val filename = src.substringAfterLast('/').substringBefore('?').takeIf { it.isNotBlank() }
                if (filename != null) "[Image: $filename]" else "[Image]"
            }
            else -> "[Image]"
        }
        
        // Wrap in span to maintain inline positioning
        "<span>$placeholder</span>"
    }
    
    return processedHtml
}

private fun preprocessHyperlinks(html: String): String {
    var processedHtml = html
    
    // Find all <a> tags and replace them with readable text format
    val linkRegex = """<a[^>]+href=['"](.*?)['"][^>]*>(.*?)</a>""".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    
    processedHtml = linkRegex.replace(processedHtml) { matchResult ->
        val url = matchResult.groupValues[1]
        val linkText = matchResult.groupValues[2]
        
        // Clean the link text of any remaining HTML
        val cleanLinkText = HtmlCompat.fromHtml(linkText, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
        
        // For article list, show link text with a simple indicator
        if (cleanLinkText.isNotBlank()) {
            "<u>$cleanLinkText</u>" // Underline to indicate it's a link
        } else {
            // If no link text, show the domain
            val domain = try {
                Uri.parse(url).host ?: url
            } catch (e: Exception) {
                url
            }
            "<u>[$domain]</u>"
        }
    }
    
    return processedHtml
}

private fun preprocessUnorderedLists(html: String): String {
    var processedHtml = html
    
    // Find all <ul>...</ul> blocks and replace them with bullet-formatted text
    val ulRegex = """<ul[^>]*>(.*?)</ul>""".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    
    processedHtml = ulRegex.replace(processedHtml) { matchResult ->
        val listContent = matchResult.groupValues[1]
        
        // Extract individual list items
        val liRegex = """<li[^>]*>(.*?)</li>""".toRegex(setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val listItems = liRegex.findAll(listContent)
            .map { liMatch ->
                // Clean the list item content of any remaining HTML
                val itemContent = liMatch.groupValues[1].trim()
                val cleanContent = HtmlCompat.fromHtml(itemContent, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
                if (cleanContent.isNotBlank()) "• $cleanContent" else ""
            }
            .filter { it.isNotBlank() }
            .toList()
        
        // Join list items with line breaks, wrapped in paragraph tags to maintain spacing
        if (listItems.isNotEmpty()) {
            "<p>${listItems.joinToString("<br>")}</p>"
        } else {
            ""
        }
    }
    
    return processedHtml
}

