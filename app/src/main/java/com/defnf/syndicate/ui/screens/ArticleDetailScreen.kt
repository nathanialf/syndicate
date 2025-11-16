package com.defnf.syndicate.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.defnf.syndicate.ui.components.FaviconIcon
import com.defnf.syndicate.ui.viewmodel.ArticleDetailViewModel
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleDetailScreen(
    articleId: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSidebarMode: Boolean = false
) {
    val viewModel: ArticleDetailViewModel = hiltViewModel()
    val article by viewModel.article.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }
    
    // Handle system back gesture
    BackHandler {
        onBackClick()
    }

    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(text = article?.feedTitle ?: "Article")
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    article?.let { art ->
                        // Share button
                        IconButton(
                            onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${art.title}\n\n${art.url}")
                                    putExtra(Intent.EXTRA_SUBJECT, art.title)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share"
                            )
                        }

                        // Open in browser button
                        IconButton(
                            onClick = {
                                if (!art.url.isNullOrBlank() && art.url.startsWith("http")) {
                                    try {
                                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(art.url))
                                        context.startActivity(browserIntent)
                                    } catch (e: Exception) {
                                        // Handle case where no browser is available or URL is malformed
                                        android.util.Log.e("ArticleDetail", "Failed to open URL: ${art.url}", e)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "Open in browser"
                            )
                        }
                    }
                },
                windowInsets = if (isSidebarMode) {
                    androidx.compose.foundation.layout.WindowInsets.systemBars
                } else {
                    androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                article != null -> {
                    ArticleDetailContent(
                        article = article!!,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    Text(
                        text = "Article not found",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleDetailContent(
    article: com.defnf.syndicate.data.models.Article,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Article image if available
        article.thumbnailUrl?.let { thumbnailUrl ->
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Article image",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Article title
        SelectionContainer {
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
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
                size = 20.dp
            )

            Text(
                text = buildString {
                    append(article.feedTitle)
                    article.publishedDate?.let { date ->
                        append(" • ")
                        append(formatDate(date))
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        // Article content
        article.description?.let { description ->
            RichContentDisplay(
                html = description,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    
    calendar.time = date
    val articleYear = calendar.get(Calendar.YEAR)
    
    val dateFormat = if (articleYear == currentYear) {
        SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
    } else {
        SimpleDateFormat("MMM dd, yyyy, h:mm a", Locale.getDefault())
    }
    
    return dateFormat.format(date)
}

@Composable
private fun RichContentDisplay(
    html: String,
    modifier: Modifier = Modifier
) {
    val contentElements = parseHtmlToElements(html)
    
    if (contentElements.isNotEmpty()) {
        Card(modifier = modifier) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                contentElements.forEach { element ->
                    when (element) {
                        is ContentElement.TextContent -> {
                            if (element.content.text.trim().isNotBlank()) {
                                SelectionContainer {
                                    Text(
                                        text = element.content,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.1
                                        )
                                    )
                                }
                            }
                        }
                        is ContentElement.ImageContent -> {
                            AsyncImage(
                                model = element.src,
                                contentDescription = element.alt,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.FillWidth,
                                error = painterResource(android.R.drawable.ic_menu_report_image),
                                placeholder = painterResource(android.R.drawable.ic_menu_gallery)
                            )
                            // Show caption if alt text is available
                            element.alt?.let { altText ->
                                Text(
                                    text = altText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontStyle = FontStyle.Italic,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                        is ContentElement.CodeBlockContent -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = element.code,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                        is ContentElement.YouTubeEmbedContent -> {
                            val context = LocalContext.current
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${element.videoId}"))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            android.util.Log.e("ArticleDetail", "Failed to open YouTube video: ${element.videoId}", e)
                                        }
                                    },
                                colors = androidx.compose.material3.CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AsyncImage(
                                        model = getYouTubeThumbnailUrl(element.videoId),
                                        contentDescription = element.title ?: "YouTube video",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(16f / 9f)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop,
                                        error = painterResource(android.R.drawable.ic_media_play),
                                        placeholder = painterResource(android.R.drawable.ic_menu_gallery)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(android.R.drawable.ic_media_play),
                                            contentDescription = "Play video",
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = element.title ?: "Watch on YouTube",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private sealed class ContentElement {
    data class TextContent(val content: AnnotatedString) : ContentElement()
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
            val styledText = parseHtmlText(processedText)
            if (styledText.text.trim().isNotBlank()) {
                elements.add(ContentElement.TextContent(styledText))
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
                    val htmlStripped = HtmlCompat.fromHtml(withPlaceholders, HtmlCompat.FROM_HTML_MODE_LEGACY).toString()
                    
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
        val styledText = parseHtmlText(processedText)
        if (styledText.text.trim().isNotBlank()) {
            elements.add(ContentElement.TextContent(styledText))
        }
    }
    
    // If no special elements were found, treat the entire content as text
    if (allMatches.isEmpty()) {
        val processedText = preprocessUnorderedLists(html)
        val styledText = parseHtmlText(processedText)
        if (styledText.text.trim().isNotBlank()) {
            elements.add(ContentElement.TextContent(styledText))
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

@OptIn(ExperimentalTextApi::class)
private fun parseHtmlText(html: String): AnnotatedString {
    // Preprocess HTML to handle special elements before HtmlCompat processing
    // Note: Images are handled separately in RichContentDisplay, so no image preprocessing here
    val processedHtml = preprocessUnorderedLists(html)
    
    // Use Android's built-in HTML parser
    val spanned = HtmlCompat.fromHtml(processedHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
    
    return buildAnnotatedString {
        val text = spanned.toString()
        append(text)
        
        // Get all spans and convert them to Compose styles
        val spans = spanned.getSpans(0, spanned.length, Any::class.java)
        
        for (span in spans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            
            if (start >= 0 && end <= length && start < end) {
                when (span) {
                    is android.text.style.URLSpan -> {
                        // Handle hyperlinks with modern LinkAnnotation
                        addStyle(
                            SpanStyle(
                                color = Color(0xFF1976D2), // Material blue color for links
                                textDecoration = TextDecoration.Underline
                            ), 
                            start, 
                            end
                        )
                        // Use LinkAnnotation for modern clickable links
                        addLink(
                            LinkAnnotation.Url(span.url),
                            start,
                            end
                        )
                    }
                    is android.text.style.StyleSpan -> {
                        when (span.style) {
                            android.graphics.Typeface.BOLD -> {
                                addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                            }
                            android.graphics.Typeface.ITALIC -> {
                                addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                            }
                            android.graphics.Typeface.BOLD_ITALIC -> {
                                addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
                            }
                        }
                    }
                    is android.text.style.RelativeSizeSpan -> {
                        val newSize = (span.sizeChange * 16).sp
                        addStyle(SpanStyle(fontSize = newSize), start, end)
                    }
                    is android.text.style.AbsoluteSizeSpan -> {
                        addStyle(SpanStyle(fontSize = span.size.sp), start, end)
                    }
                    is android.text.style.UnderlineSpan -> {
                        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                    }
                    is android.text.style.StrikethroughSpan -> {
                        addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, end)
                    }
                }
            }
        }
    }
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
        
        // Wrap in paragraph for better spacing in detailed view
        "<p><em>$placeholder</em></p>"
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
                // Clean the list item content of any remaining HTML but preserve formatting
                val itemContent = liMatch.groupValues[1].trim()
                val cleanContent = HtmlCompat.fromHtml(itemContent, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
                if (cleanContent.isNotBlank()) "• $cleanContent" else ""
            }
            .filter { it.isNotBlank() }
            .toList()
        
        // Join list items with line breaks, without extra paragraph wrapping to avoid double spacing
        if (listItems.isNotEmpty()) {
            listItems.joinToString("<br>")
        } else {
            ""
        }
    }
    
    return processedHtml
}


