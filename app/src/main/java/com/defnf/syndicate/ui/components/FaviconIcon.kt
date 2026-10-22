package com.defnf.syndicate.ui.components

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.defnf.syndicate.data.models.Feed

@Composable
fun FaviconIcon(
    feed: Feed,
    isSelected: Boolean,
    isAvailable: Boolean,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Without a favicon the feed icon sits in a tonal badge, like the group icons
    val badgeColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isAvailable -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val iconTint = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isAvailable -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    var imageLoadSuccess by remember { mutableStateOf(true) }

    // Pre-build image request to reduce overhead during scroll
    val imageRequest = remember(feed.faviconUrl) {
        feed.faviconUrl?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .crossfade(false) // Disable crossfade for better scroll performance
                .allowHardware(true) // Use hardware bitmaps for better performance
                .memoryCacheKey(url) // Explicit cache key
                .build()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .let { baseModifier ->
                if (!feed.faviconUrl.isNullOrBlank() && imageLoadSuccess) {
                    baseModifier // No background for favicons
                } else {
                    baseModifier.background(badgeColor)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!feed.faviconUrl.isNullOrBlank() && imageLoadSuccess && imageRequest != null) {
            AsyncImage(
                model = imageRequest,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.White, CircleShape) // White background for transparency
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
                modifier = Modifier.size(size / 2)
            )
        }
    }
}
