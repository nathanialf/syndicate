package com.defnf.syndicate.ui.reader

/**
 * An article body as the reader shows it: a list of blocks (paragraphs, headings, lists, figures...)
 * whose text carries inline styles. Built from the feed's HTML by [ArticleContentParser], so the UI
 * never deals with HTML.
 */
data class ArticleContent(val blocks: List<ArticleBlock>) {
    val isEmpty: Boolean get() = blocks.isEmpty()
}

sealed interface ArticleBlock {
    data class Paragraph(val text: InlineText) : ArticleBlock

    /** [level] is 1 to 6, as in `<h1>` to `<h6>`. */
    data class Heading(val level: Int, val text: InlineText) : ArticleBlock

    data class Quote(val blocks: List<ArticleBlock>) : ArticleBlock

    /** Each item is its own list of blocks, so items can hold paragraphs and nested lists. */
    data class BulletList(val ordered: Boolean, val items: List<List<ArticleBlock>>, val start: Int = 1) : ArticleBlock

    data class Image(val url: String, val alt: String?, val caption: InlineText?) : ArticleBlock

    data class Code(val text: String) : ArticleBlock

    data class Video(val youTubeId: String, val title: String?) : ArticleBlock

    data object Divider : ArticleBlock
}

/** Plain text with styled ranges. */
data class InlineText(val text: String, val spans: List<InlineSpan> = emptyList())

/** [style] applies to the characters from [start] (inclusive) to [end] (exclusive). */
data class InlineSpan(val start: Int, val end: Int, val style: InlineStyle)

sealed interface InlineStyle {
    data object Bold : InlineStyle
    data object Italic : InlineStyle
    data object Underline : InlineStyle
    data object Strikethrough : InlineStyle
    data object Code : InlineStyle
    data object Superscript : InlineStyle
    data object Subscript : InlineStyle
    data class Link(val url: String) : InlineStyle
}
