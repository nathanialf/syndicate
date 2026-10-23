package com.defnf.syndicate.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import coil.size.Size
import com.defnf.syndicate.data.models.Feed
import java.io.IOException
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * A feed's favicon over a tonal RSS badge. The badge shows at once and stays for feeds without a
 * favicon; a favicon fades in over it when it arrives, or appears at once when it is already in
 * memory, so rows don't flash an empty circle while scrolling.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FaviconIcon(feed: Feed, isSelected: Boolean, isAvailable: Boolean, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val context = LocalContext.current

    val badgeColor = when {
        isSelected -> MaterialTheme.colorScheme.surface
        isAvailable -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val iconTint = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isAvailable -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }

    // A favicon that failed this session isn't requested again each time its row scrolls back in
    val url = feed.faviconUrl?.takeIf { it.isNotBlank() && it !in FailedFavicons }
    val request = remember(url) {
        url?.let {
            ImageRequest.Builder(context)
                .data(it)
                .memoryCacheKey(it)
                // One decode serves every size it's shown at: large enough to stay sharp at 40dp on the
                // densest screens, small enough that a site's 256px icon doesn't fill the memory cache
                .size(FaviconDecodeSize)
                .build()
        }
    }
    var loaded by remember(url) {
        mutableStateOf(url != null && context.imageLoader.memoryCache?.get(MemoryCache.Key(url)) != null)
    }
    val faviconAlpha by animateFloatAsState(
        targetValue = if (loaded) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "favicon_alpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(LauncherIconShape)
            .background(badgeColor),
        contentAlignment = Alignment.Center
    ) {
        if (faviconAlpha < 1f) {
            Icon(
                imageVector = Icons.Default.RssFeed,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(size / 2)
            )
        }
        if (request != null) {
            AsyncImage(
                model = request,
                contentDescription = null,
                // Fill the shape even when a site's icon isn't square
                contentScale = ContentScale.Crop,
                onSuccess = { loaded = true },
                // Network errors (offline, timeouts) are retried next time; only a missing or undecodable icon is remembered
                onError = { state -> if (state.result.throwable !is IOException) url?.let(FailedFavicons::add) },
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = faviconAlpha }
                    // Favicons are often transparent, drawn for a light page
                    .background(Color.White, LauncherIconShape)
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        shape = LauncherIconShape
                    )
            )
        }
    }
}

private val FaviconDecodeSize = Size(192, 192)

/** Favicon URLs that failed to load in this process (missing or undecodable, not network errors). */
private val FailedFavicons: MutableSet<String> = Collections.newSetFromMap(ConcurrentHashMap())
