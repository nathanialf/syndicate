package com.syndicate.rssreader.ui.components

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
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.syndicate.rssreader.data.models.Article
import com.syndicate.rssreader.ui.common.LayoutConstants
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
                
                // Description
                article.description?.let { description ->
                    val cleanDescription = parseHtmlText(description).trim()
                    if (cleanDescription.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = cleanDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (article.isRead) {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                
                // Feed info and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Use the reusable FaviconIcon with a mock feed object
                    FaviconIcon(
                        feed = com.syndicate.rssreader.data.models.Feed(
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

private fun parseHtmlText(html: String): String {
    return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
}