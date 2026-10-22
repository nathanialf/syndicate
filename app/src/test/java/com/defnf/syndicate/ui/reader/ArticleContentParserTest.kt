package com.defnf.syndicate.ui.reader

import com.defnf.syndicate.ui.reader.ArticleBlock.BulletList
import com.defnf.syndicate.ui.reader.ArticleBlock.Code
import com.defnf.syndicate.ui.reader.ArticleBlock.Heading
import com.defnf.syndicate.ui.reader.ArticleBlock.Image
import com.defnf.syndicate.ui.reader.ArticleBlock.Paragraph
import com.defnf.syndicate.ui.reader.ArticleBlock.Quote
import com.defnf.syndicate.ui.reader.ArticleBlock.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArticleContentParserTest {

    private fun parse(html: String, headerImageUrl: String? = null, title: String? = null) =
        ArticleContentParser.parse(html, baseUrl = "https://example.com/posts/1", headerImageUrl = headerImageUrl, title = title).blocks

    private fun List<ArticleBlock>.texts() = map { (it as Paragraph).text.text }

    @Test
    fun `paragraphs collapse whitespace and skip empty ones`() {
        val blocks = parse("<p>  Hello\n   world </p><p>&nbsp;</p><div>Second <br/> line</div>")
        assertEquals(listOf("Hello world", "Second\nline"), blocks.texts())
    }

    @Test
    fun `loose text and block elements become separate paragraphs`() {
        val blocks = parse("Intro text<p>Body</p>Outro")
        assertEquals(listOf("Intro text", "Body", "Outro"), blocks.texts())
    }

    @Test
    fun `inline styles and links keep their ranges`() {
        val text = (parse("<p>A <b>bold</b> and <a href=\"/more\">linked <i>word</i></a>.</p>").single() as Paragraph).text
        assertEquals("A bold and linked word.", text.text)
        assertTrue(InlineSpan(2, 6, InlineStyle.Bold) in text.spans)
        assertTrue(InlineSpan(11, 22, InlineStyle.Link("https://example.com/more")) in text.spans)
        assertTrue(InlineSpan(18, 22, InlineStyle.Italic) in text.spans)
    }

    @Test
    fun `headings, quotes, lists and code are structured`() {
        val blocks = parse(
            """
            <h2>Section</h2>
            <blockquote><p>Quoted</p></blockquote>
            <ol start="3"><li>Three</li><li>Four<ul><li>Nested</li></ul></li></ol>
            <pre><code>val x = 1
            val y = 2</code></pre>
            """.trimIndent()
        )
        assertEquals(Heading(2, InlineText("Section")), blocks[0])
        assertEquals(Quote(listOf(Paragraph(InlineText("Quoted")))), blocks[1])
        val list = blocks[2] as BulletList
        assertTrue(list.ordered)
        assertEquals(3, list.start)
        assertEquals(listOf(Paragraph(InlineText("Three"))), list.items[0])
        assertEquals(BulletList(ordered = false, items = listOf(listOf(Paragraph(InlineText("Nested"))))), list.items[1][1])
        assertEquals(Code("val x = 1\nval y = 2"), blocks[3])
    }

    @Test
    fun `figures keep their caption and lazy images their real source`() {
        val blocks = parse(
            "<figure><img src=\"data:image/gif;base64,R0l\" data-src=\"/img/a.jpg\" alt=\"A\"><figcaption>Caption</figcaption></figure>"
        )
        assertEquals(listOf(Image("https://example.com/img/a.jpg", "A", InlineText("Caption"))), blocks)
    }

    @Test
    fun `scripts, tracking pixels and other embeds are dropped, YouTube is kept`() {
        val blocks = parse(
            "<p>Text</p><script>alert(1)</script><img src=\"https://t.example/p.gif\" width=\"1\" height=\"1\">" +
                "<iframe src=\"https://ads.example/x\"></iframe><iframe src=\"https://www.youtube.com/embed/abc_123?rel=0\" title=\"Clip\"></iframe>"
        )
        assertEquals(listOf(Paragraph(InlineText("Text")), Video("abc_123", "Clip")), blocks)
    }

    @Test
    fun `an image inside a link splits the paragraph without losing the link`() {
        val blocks = parse("<p><a href=\"https://a.example\">before <img src=\"https://a.example/i.png\"> after</a></p>")
        assertEquals(3, blocks.size)
        assertEquals(InlineText("before", listOf(InlineSpan(0, 6, InlineStyle.Link("https://a.example")))), (blocks[0] as Paragraph).text)
        assertTrue(blocks[1] is Image)
        assertEquals(InlineText("after", listOf(InlineSpan(0, 5, InlineStyle.Link("https://a.example")))), (blocks[2] as Paragraph).text)
    }

    @Test
    fun `the header image is removed from the body`() {
        val html = "<p><img src=\"https://cdn.example/photo-1024x768.jpg?w=1024&amp;ssl=1\"></p><p>Body</p>" +
            "<img src=\"https://cdn.example/other.jpg\">"
        val blocks = parse(html, headerImageUrl = "http://CDN.example/photo.jpg?w=300&amp;h=200")
        assertEquals(listOf(Paragraph(InlineText("Body")), Image("https://cdn.example/other.jpg", null, null)), blocks)
    }

    @Test
    fun `different images with the same path but different ids are kept`() {
        val key = ArticleContentParser::imageKey
        assertTrue(key("https://x.example/image.php?id=1") != key("https://x.example/image.php?id=2"))
        assertEquals(key("https://i0.wp.com/x.example/a.png?resize=10"), key("https://x.example/a.png"))
    }

    @Test
    fun `a leading heading repeating the title is removed`() {
        val blocks = parse("<h1>My  Post</h1><p>Body</p>", title = "My post")
        assertEquals(listOf(Paragraph(InlineText("Body"))), blocks)
    }

    @Test
    fun `tables read row by row`() {
        val blocks = parse("<table><tr><th>Name</th><th>Size</th></tr><tr><td>A</td><td>1</td></tr></table>")
        assertEquals(listOf("Name · Size", "A · 1"), blocks.texts())
    }

    @Test
    fun `summary is the text on one line`() {
        assertEquals("Title Body text", ArticleContentParser.summary("<h1>Title</h1><p>Body&nbsp;text</p><img src=\"x.png\">"))
    }
}
