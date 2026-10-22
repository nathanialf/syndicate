package com.defnf.syndicate.ui.reader

import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.parser.Parser

/**
 * Turns a feed's article HTML into [ArticleContent]. Feeds send anything from a bare string to a
 * full web page, so the parser is lenient: unknown elements are unwrapped, scripts, forms, tracking
 * pixels and share widgets are dropped, and whitespace is collapsed the way a browser would.
 */
object ArticleContentParser {

    /**
     * Parses [html]. Relative links and images resolve against [baseUrl]. The article's
     * [headerImageUrl] is already shown above the body, so the same image is removed from the body;
     * a leading heading repeating [title] is removed for the same reason.
     */
    fun parse(html: String, baseUrl: String = "", headerImageUrl: String? = null, title: String? = null): ArticleContent {
        val body = Jsoup.parseBodyFragment(html, baseUrl).body()
        removeClutter(body)

        var blocks = BlockBuilder().apply { visitChildren(body) }.build()
        if (headerImageUrl != null) {
            blocks = blocks.withoutImage(imageKey(headerImageUrl))
        }
        if (title != null) {
            blocks = blocks.withoutLeadingTitle(title)
        }
        return ArticleContent(blocks)
    }

    /** The article's text on one line, for previews. */
    /**
     * The article's text on one line, for previews: what the reader shows as text, without a
     * leading heading repeating [title], image captions or code.
     */
    fun summary(html: String, title: String? = null): String = parse(html, title = title).blocks
        .joinToString(" ") { it.plainText() }
        .replace(WHITESPACE, " ")
        .trim()

    private fun ArticleBlock.plainText(): String = when (this) {
        is ArticleBlock.Paragraph -> text.text
        is ArticleBlock.Heading -> text.text
        is ArticleBlock.Quote -> blocks.joinToString(" ") { it.plainText() }
        is ArticleBlock.BulletList -> items.joinToString(" ") { item -> item.joinToString(" ") { it.plainText() } }
        is ArticleBlock.Image, is ArticleBlock.Code, is ArticleBlock.Video, ArticleBlock.Divider -> ""
    }

    /**
     * A key that is equal for two URLs of the same image: the scheme, a CDN proxy prefix and the
     * resizing parameters feeds commonly add (query strings on image files, WordPress `-1024x768`
     * suffixes) are ignored.
     */
    fun imageKey(url: String): String {
        var key = Parser.unescapeEntities(url.trim(), true)
            .substringBefore('#')
            .replace(SCHEME, "")
            .replace(PHOTON_PREFIX, "")
        val path = key.substringBefore('?')
        if (IMAGE_EXTENSION.containsMatchIn(path)) key = path
        val host = key.substringBefore('/')
        return host.lowercase() + key.substring(host.length).replace(SIZE_SUFFIX, "").replace(SCALED_SUFFIX, "")
    }

    private fun removeClutter(body: Element) {
        body.select(
            "script, style, noscript, template, form, input, button, select, textarea, svg, object, embed, " +
                ".feedflare, .sharedaddy, .jp-relatedposts, img[src*=feeds.feedburner.com], " +
                "a[href*=feeds.feedburner.com/~ff], a[href*=feeds.feedburner.com/~a]"
        ).remove()
        body.select("iframe").filter { youTubeId(it.attr("src")) == null }.forEach { it.remove() }
        // Tracking pixels
        body.select("img").filter { img ->
            listOf("width", "height").any { img.attr(it).trim().removeSuffix("px").toIntOrNull()?.let { size -> size <= 2 } == true }
        }.forEach { it.remove() }
    }

    private fun List<ArticleBlock>.withoutImage(key: String): List<ArticleBlock> = mapNotNull { block ->
        when (block) {
            is ArticleBlock.Image -> block.takeUnless { imageKey(it.url) == key }
            is ArticleBlock.Quote -> ArticleBlock.Quote(block.blocks.withoutImage(key)).takeIf { it.blocks.isNotEmpty() }
            is ArticleBlock.BulletList -> block.copy(items = block.items.map { it.withoutImage(key) }.filter { it.isNotEmpty() })
                .takeIf { it.items.isNotEmpty() }
            else -> block
        }
    }

    private fun List<ArticleBlock>.withoutLeadingTitle(title: String): List<ArticleBlock> {
        val first = firstOrNull() as? ArticleBlock.Heading ?: return this
        return if (first.text.text.normalized() == title.normalized()) drop(1) else this
    }

    private fun String.normalized() = replace(WHITESPACE, " ").trim().lowercase()

    internal fun youTubeId(src: String): String? = YOUTUBE_EMBED.find(src)?.groupValues?.get(1)

    private val WHITESPACE = Regex("\\s+")
    private val SCHEME = Regex("^(https?:)?//", RegexOption.IGNORE_CASE)
    private val PHOTON_PREFIX = Regex("^i[0-3]\\.wp\\.com/", RegexOption.IGNORE_CASE)
    private val IMAGE_EXTENSION = Regex("\\.(jpe?g|png|gif|webp|avif|bmp)$", RegexOption.IGNORE_CASE)
    private val SIZE_SUFFIX = Regex("-\\d+x\\d+(?=\\.\\w+$)")
    private val SCALED_SUFFIX = Regex("-scaled(?=\\.\\w+$)")
    private val YOUTUBE_EMBED = Regex("youtube(?:-nocookie)?\\.com/embed/([\\w-]+)", RegexOption.IGNORE_CASE)
}

/** Walks the HTML tree, collecting inline content into paragraphs until a block element ends one. */
private class BlockBuilder {
    private val blocks = mutableListOf<ArticleBlock>()
    private val inline = InlineBuilder()

    fun build(): List<ArticleBlock> {
        flush()
        return blocks
    }

    fun visitChildren(element: Element) {
        element.childNodes().forEach(::visit)
    }

    private fun flush() {
        inline.build()?.let { blocks += ArticleBlock.Paragraph(it) }
    }

    private fun addBlock(block: ArticleBlock) {
        flush()
        blocks += block
    }

    private fun visit(node: Node) {
        when (node) {
            is TextNode -> inline.appendText(node.wholeText)
            is Element -> visitElement(node)
        }
    }

    private fun visitElement(element: Element) {
        when (val tag = element.normalName()) {
            "br" -> inline.lineBreak()
            "hr" -> addBlock(ArticleBlock.Divider)
            "h1", "h2", "h3", "h4", "h5", "h6" -> inlineTextOf(element)?.let {
                addBlock(ArticleBlock.Heading(tag[1].digitToInt(), it))
            }
            "blockquote" -> nestedBlocks(element).takeIf { it.isNotEmpty() }?.let { addBlock(ArticleBlock.Quote(it)) }
            "ul", "ol" -> list(element)?.let(::addBlock)
            "pre" -> element.wholeText().trimEnd().trimStart('\n', '\r')
                .takeIf { it.isNotBlank() }
                ?.let { addBlock(ArticleBlock.Code(it.replace("\r\n", "\n"))) }
            "img" -> image(element, caption = null)?.let(::addBlock)
            "figure" -> figure(element)
            "iframe" -> ArticleContentParser.youTubeId(element.attr("src"))?.let {
                addBlock(ArticleBlock.Video(it, element.attr("title").ifBlank { null }))
            }
            "table" -> table(element)
            in BLOCK_TAGS -> {
                flush()
                visitChildren(element)
                flush()
            }
            else -> {
                val style = inlineStyle(element)
                if (style != null) inline.push(style)
                visitChildren(element)
                if (style != null) inline.pop()
            }
        }
    }

    private fun inlineStyle(element: Element): InlineStyle? = when (element.normalName()) {
        "b", "strong" -> InlineStyle.Bold
        "i", "em", "cite", "dfn", "var" -> InlineStyle.Italic
        "u", "ins" -> InlineStyle.Underline
        "s", "strike", "del" -> InlineStyle.Strikethrough
        "code", "kbd", "samp", "tt" -> InlineStyle.Code
        "sup" -> InlineStyle.Superscript
        "sub" -> InlineStyle.Subscript
        "a" -> element.absUrl("href").ifBlank { element.attr("href") }
            .takeIf { it.startsWith("http://") || it.startsWith("https://") || it.startsWith("mailto:") }
            ?.let { InlineStyle.Link(it) }
        else -> null
    }

    private fun inlineTextOf(element: Element): InlineText? = InlineBuilder().also { builder ->
        fun walk(node: Node) {
            when (node) {
                is TextNode -> builder.appendText(node.wholeText)
                is Element -> if (node.normalName() == "br") {
                    builder.lineBreak()
                } else {
                    // Block content flattened into one line is still separated, like cells of a nested table
                    val isBlock = node.normalName() in BLOCK_TAGS || node.normalName() in TABLE_TAGS
                    if (isBlock) builder.appendText(" ")
                    val style = inlineStyle(node)
                    if (style != null) builder.push(style)
                    node.childNodes().forEach(::walk)
                    if (style != null) builder.pop()
                    if (isBlock) builder.appendText(" ")
                }
            }
        }
        element.childNodes().forEach(::walk)
    }.build()

    private fun nestedBlocks(element: Element): List<ArticleBlock> = BlockBuilder().apply { visitChildren(element) }.build()

    private fun list(element: Element): ArticleBlock.BulletList? {
        val items = element.children()
            .filter { it.normalName() == "li" }
            .map(::nestedBlocks)
            .filter { it.isNotEmpty() }
        if (items.isEmpty()) return null
        return ArticleBlock.BulletList(
            ordered = element.normalName() == "ol",
            items = items,
            start = element.attr("start").toIntOrNull() ?: 1
        )
    }

    private fun figure(element: Element) {
        val images = element.select("img")
        // A gallery nests a figure per image, each with its own caption
        val isGallery = element.select("figure").size > 1
        if (images.isEmpty() || isGallery) {
            flush()
            visitChildren(element)
            flush()
            return
        }
        val caption = element.selectFirst("figcaption")?.let(::inlineTextOf)
        images.forEachIndexed { index, img ->
            // The caption belongs under the last image of the figure
            image(img, caption.takeIf { index == images.lastIndex })?.let(::addBlock)
        }
    }

    private fun image(img: Element, caption: InlineText?): ArticleBlock.Image? {
        val url = imageUrl(img) ?: return null
        return ArticleBlock.Image(url = url, alt = img.attr("alt").trim().ifBlank { null }, caption = caption)
    }

    /** The image's URL; lazy-loading pages keep it in a data attribute and put a placeholder in src. */
    private fun imageUrl(img: Element): String? {
        val candidates = listOf("data-src", "data-lazy-src", "data-original", "src").map { img.attr(it) } +
            img.attr("srcset").split(',').map { it.trim().substringBefore(' ') }
        return candidates
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { resolve(img.baseUri(), it) }
            .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
    }

    private fun resolve(baseUri: String, url: String): String = when {
        url.startsWith("//") -> "https:$url"
        baseUri.isBlank() -> url
        // URL, unlike URI.resolve, handles bases without a path and unescaped characters
        else -> runCatching { java.net.URL(java.net.URL(baseUri), url).toString() }.getOrDefault(url)
    }

    /**
     * Tables are read row by row, a row's cells separated by a middle dot, followed by the row's images.
     * Nested tables are read as part of the cell holding them.
     */
    private fun table(element: Element) {
        flush()
        element.select("tr")
            .filter { row -> row.parents().firstOrNull { it.normalName() == "table" } == element }
            .forEach { row ->
                val cellElements = row.children().filter { it.normalName() == "td" || it.normalName() == "th" }
                val cells = cellElements.mapNotNull { cell -> inlineTextOf(cell) }
                if (cells.isNotEmpty()) {
                    val bold = cellElements.all { it.normalName() == "th" }
                    blocks += ArticleBlock.Paragraph(joinCells(cells, bold))
                }
                cellElements.flatMap { it.select("img") }.mapNotNullTo(blocks) { image(it, caption = null) }
            }
    }

    private fun joinCells(cells: List<InlineText>, bold: Boolean): InlineText {
        val text = StringBuilder()
        val spans = mutableListOf<InlineSpan>()
        cells.forEachIndexed { index, cell ->
            if (index > 0) text.append(" · ")
            val offset = text.length
            text.append(cell.text)
            cell.spans.mapTo(spans) { it.copy(start = it.start + offset, end = it.end + offset) }
        }
        if (bold) spans += InlineSpan(0, text.length, InlineStyle.Bold)
        return InlineText(text.toString(), spans)
    }

    private companion object {
        val BLOCK_TAGS = setOf(
            "p", "div", "section", "article", "header", "footer", "main", "aside", "nav", "center",
            "address", "details", "summary", "dl", "dt", "dd", "li", "figcaption", "caption", "body"
        )
        val TABLE_TAGS = setOf("table", "thead", "tbody", "tfoot", "tr", "td", "th")
    }
}

/**
 * Accumulates the text of one paragraph with its styles. Whitespace collapses like in a browser.
 * Styles still open when the paragraph ends (an inline element wrapping a block, like a link around
 * an image) continue in the next paragraph.
 */
private class InlineBuilder {
    private val text = StringBuilder()
    private val spans = mutableListOf<InlineSpan>()
    private val open = ArrayDeque<Pair<InlineStyle, Int>>()

    fun push(style: InlineStyle) {
        open.addLast(style to text.length)
    }

    fun pop() {
        val (style, start) = open.removeLastOrNull() ?: return
        addSpan(start, text.length, style)
    }

    fun appendText(raw: String) {
        for (char in raw) {
            if (char.isWhitespace()) {
                if (text.isNotEmpty() && !text.last().isWhitespace()) text.append(' ')
            } else {
                text.append(char)
            }
        }
    }

    fun lineBreak() {
        trimTrailingSpaces()
        if (text.isNotEmpty()) text.append('\n')
    }

    /** The paragraph so far, or null if it is blank; the builder then starts the next paragraph. */
    fun build(): InlineText? {
        val openStyles = open.map { it.first }
        while (open.isNotEmpty()) pop()
        while (text.isNotEmpty() && text.last().isWhitespace()) text.setLength(text.length - 1)

        val result = if (text.isNotEmpty()) {
            val length = text.length
            InlineText(
                text = text.toString(),
                spans = spans.mapNotNull { span ->
                    span.copy(end = minOf(span.end, length)).takeIf { it.start < it.end }
                }
            )
        } else {
            null
        }
        text.setLength(0)
        spans.clear()
        openStyles.forEach(::push)
        return result
    }

    private fun trimTrailingSpaces() {
        while (text.isNotEmpty() && text.last() == ' ') text.setLength(text.length - 1)
    }

    private fun addSpan(start: Int, end: Int, style: InlineStyle) {
        if (start < end) spans += InlineSpan(start, end, style)
    }
}
