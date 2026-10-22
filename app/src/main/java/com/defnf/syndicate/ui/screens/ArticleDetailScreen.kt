package com.defnf.syndicate.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.defnf.syndicate.ui.common.LayoutConstants
import com.defnf.syndicate.ui.components.FaviconIcon
import com.defnf.syndicate.ui.reader.ArticleBody
import com.defnf.syndicate.ui.reader.ArticleContent
import com.defnf.syndicate.ui.viewmodel.ArticleDetailViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArticleDetailScreen(articleId: String, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val viewModel: ArticleDetailViewModel = hiltViewModel()
    val article by viewModel.article.collectAsState()
    val content by viewModel.content.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current
    // The floating toolbar slides away while reading down and comes back when scrolling up
    val toolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom
    )

    LaunchedEffect(articleId) {
        viewModel.loadArticle(articleId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = article?.feedTitle ?: "Article",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, shapes = IconButtonDefaults.shapes()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(toolbarScrollBehavior)
        ) {
            when {
                isLoading -> {
                    LoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                article != null -> {
                    val art = article!!
                    ArticleDetailContent(
                        article = art,
                        content = content,
                        modifier = Modifier.fillMaxSize()
                    )

                    HorizontalFloatingToolbar(
                        expanded = true,
                        scrollBehavior = toolbarScrollBehavior,
                        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = -FloatingToolbarDefaults.ScreenOffset)
                    ) {
                        IconButton(
                            onClick = {
                                val shareIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${art.title}\n\n${art.url}")
                                    putExtra(Intent.EXTRA_SUBJECT, art.title)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Article"))
                            },
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share"
                            )
                        }

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
                            },
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInBrowser,
                                contentDescription = "Open in browser"
                            )
                        }
                    }
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

/** The article laid out for reading, kept to a comfortable line length on wide windows. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ArticleDetailContent(
    article: com.defnf.syndicate.data.models.Article,
    content: ArticleContent?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = ReadingMaxWidth)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Article image if available
            article.thumbnailUrl?.let { thumbnailUrl ->
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(MaterialTheme.shapes.extraLarge),
                    contentScale = ContentScale.Crop
                )
            }

            SelectionContainer {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    ArticleByline(article)

                    if (content != null && !content.isEmpty) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ArticleBody(content = content, modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // Leave room to scroll the end of the article clear of the floating toolbar
            Spacer(modifier = Modifier.height(LayoutConstants.ContentBottomPaddingForFab))
        }
    }
}

/** Feed, author and date under the title. */
@Composable
private fun ArticleByline(article: com.defnf.syndicate.data.models.Article) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            size = 36.dp
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = article.author?.takeIf { it.isNotBlank() && it != article.feedTitle }
                    ?.let { "${article.feedTitle} · $it" }
                    ?: article.feedTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            article.publishedDate?.let { date ->
                Text(
                    text = formatDate(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val ReadingMaxWidth = 720.dp

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
