package com.syndicate.rssreader.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.syndicate.rssreader.data.models.Feed

@Composable
fun FaviconIcon(
    feed: Feed,
    isSelected: Boolean,
    isAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else if (isAvailable) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.error
    }

    var imageLoadSuccess by remember { mutableStateOf(true) }
    
    // Generate favicon URL from siteUrl if faviconUrl is not available
    val effectiveFaviconUrl = feed.faviconUrl ?: run {
        val baseUrl = feed.siteUrl ?: run {
            // Extract domain from RSS feed URL as fallback
            try {
                val feedUrl = java.net.URL(feed.url)
                "${feedUrl.protocol}://${feedUrl.host}"
            } catch (e: Exception) {
                null
            }
        }
        baseUrl?.let { "$it/favicon.ico" }
    }

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .let { baseModifier ->
                if (!effectiveFaviconUrl.isNullOrBlank() && imageLoadSuccess) {
                    baseModifier // No background for favicons
                } else {
                    baseModifier.background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!effectiveFaviconUrl.isNullOrBlank() && imageLoadSuccess) {
            AsyncImage(
                model = effectiveFaviconUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = CircleShape
                    ),
                onError = { 
                    imageLoadSuccess = false
                }
            )
        } else {
            Icon(
                imageVector = Icons.Default.RssFeed,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}