package com.defnf.syndicate.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.defnf.syndicate.data.models.Article
import com.defnf.syndicate.ui.reader.ArticleContentParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** An article card: swipe either way to toggle its read state. */
@Composable
fun SwipeableArticleCard(
    article: Article,
    onArticleClick: (Article) -> Unit,
    onToggleReadState: (Article) -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current
    val toggleRead = SwipeAction(
        icon = if (article.isRead) Icons.Default.RadioButtonUnchecked else Icons.Default.CheckCircle,
        label = if (article.isRead) "Mark unread" else "Mark read",
        containerColor = if (article.isRead) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.primaryContainer
        },
        contentColor = if (article.isRead) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        },
        onSwipe = { onToggleReadState(article) }
    )

    SwipeActionBox(
        startAction = toggleRead,
        endAction = toggleRead,
        // Same shape as the card being swiped, so the action behind it has matching corners
        modifier = modifier.clip(MaterialTheme.shapes.large)
    ) { actionsModifier ->
        Card(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                onArticleClick(article)
            },
            modifier = actionsModifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
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
                            .clip(MaterialTheme.shapes.medium),
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
                    ArticlePreview(
                        html = description,
                        title = article.title,
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

/** The start of the article's text; its images are left out since the card already shows the headline image. */
@Composable
private fun ArticlePreview(html: String, title: String, isRead: Boolean, maxLines: Int, modifier: Modifier = Modifier) {
    // Parse once per description instead of on every recomposition while scrolling
    val summary = remember(html, title) { ArticleContentParser.summary(html, title) }
    if (summary.isNotEmpty()) {
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isRead) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            },
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier
        )
    }
}
