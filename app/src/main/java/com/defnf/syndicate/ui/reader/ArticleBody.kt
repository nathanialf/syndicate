package com.defnf.syndicate.ui.reader

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter

/** Renders an article body with reading typography: roomy paragraphs, headings, quotes, lists and figures. */
@Composable
fun ArticleBody(content: ArticleContent, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BlockSpacing)
    ) {
        Blocks(content.blocks)
    }
}

/** [quoted] blocks are inside a quote, whose text is set apart in italics. */
@Composable
private fun Blocks(blocks: List<ArticleBlock>, quoted: Boolean = false) {
    blocks.forEach { block -> Block(block, quoted) }
}

@Composable
private fun Block(block: ArticleBlock, quoted: Boolean) {
    when (block) {
        is ArticleBlock.Paragraph -> Text(
            text = block.text.toAnnotatedString(),
            style = bodyStyle(quoted)
        )
        is ArticleBlock.Heading -> Text(
            text = block.text.toAnnotatedString(),
            style = headingStyle(block.level),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp)
        )
        is ArticleBlock.Quote -> Quote(block)
        is ArticleBlock.BulletList -> BulletList(block, quoted)
        is ArticleBlock.Image -> Figure(block)
        is ArticleBlock.Code -> CodeBlock(block.text)
        is ArticleBlock.Video -> VideoCard(block)
        ArticleBlock.Divider -> HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun bodyStyle(quoted: Boolean = false): TextStyle = MaterialTheme.typography.bodyLarge.copy(
    color = if (quoted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    fontStyle = if (quoted) FontStyle.Italic else null,
    lineHeight = 1.6.em
)

@Composable
private fun headingStyle(level: Int): TextStyle = when (level) {
    1 -> MaterialTheme.typography.headlineSmall
    2 -> MaterialTheme.typography.titleLarge
    3 -> MaterialTheme.typography.titleMedium
    else -> MaterialTheme.typography.titleSmall
}.copy(fontWeight = FontWeight.SemiBold)

@Composable
private fun Quote(quote: ArticleBlock.Quote) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
        )
        Column(
            modifier = Modifier.padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(BlockSpacing)
        ) {
            Blocks(quote.blocks, quoted = true)
        }
    }
}

@Composable
private fun BulletList(list: ArticleBlock.BulletList, quoted: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        list.items.forEachIndexed { index, item ->
            Row {
                Text(
                    text = if (list.ordered) "${list.start + index}." else "•",
                    style = bodyStyle(),
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 20.dp).padding(end = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Blocks(item, quoted)
                }
            }
        }
    }
}

/** An image with its caption; images that fail to load are left out instead of showing a broken icon. */
@Composable
private fun Figure(image: ArticleBlock.Image) {
    var failed by remember(image.url) { mutableStateOf(false) }
    if (failed) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = image.url,
            contentDescription = image.alt,
            contentScale = ContentScale.FillWidth,
            onState = { state -> if (state is AsyncImagePainter.State.Error) failed = true },
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
        )
        image.caption?.let { caption ->
            Text(
                text = caption.toAnnotatedString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = code,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace, lineHeight = 1.5.em),
            color = MaterialTheme.colorScheme.onSurface,
            softWrap = false,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(16.dp)
        )
    }
}

@Composable
private fun VideoCard(video: ArticleBlock.Video) {
    val context = LocalContext.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=${video.youTubeId}")))
                }
            }
    ) {
        Column {
            Box(contentAlignment = Alignment.Center) {
                AsyncImage(
                    // hqdefault exists for every video, unlike the larger sizes
                    model = "https://img.youtube.com/vi/${video.youTubeId}/hqdefault.jpg",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.size(width = 72.dp, height = 48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
            Text(
                text = video.title ?: "Watch on YouTube",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
private fun InlineText.toAnnotatedString(): AnnotatedString {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)
    )
    val codeStyle = SpanStyle(
        fontFamily = FontFamily.Monospace,
        background = MaterialTheme.colorScheme.surfaceContainerHighest
    )
    return remember(this, linkStyles, codeStyle) {
        buildAnnotatedString {
            append(text)
            spans.forEach { span ->
                when (val style = span.style) {
                    is InlineStyle.Link -> addLink(LinkAnnotation.Url(style.url, linkStyles), span.start, span.end)
                    InlineStyle.Bold -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), span.start, span.end)
                    InlineStyle.Italic -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), span.start, span.end)
                    InlineStyle.Underline -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), span.start, span.end)
                    InlineStyle.Strikethrough -> addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), span.start, span.end)
                    InlineStyle.Code -> addStyle(codeStyle, span.start, span.end)
                    InlineStyle.Superscript -> addStyle(
                        SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 0.75.em),
                        span.start,
                        span.end
                    )
                    InlineStyle.Subscript -> addStyle(
                        SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 0.75.em),
                        span.start,
                        span.end
                    )
                }
            }
        }
    }
}

private val BlockSpacing = 16.dp
