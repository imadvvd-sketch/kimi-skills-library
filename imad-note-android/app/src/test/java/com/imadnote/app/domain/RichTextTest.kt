package com.imadnote.app.domain

import com.imadnote.app.domain.RichText.Block
import com.imadnote.app.domain.RichText.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {
    @Test
    fun parsesBlocksAndInlineStyles() {
        val spans = RichText.parse("# Title\nsome **bold** and _it_")
        assertTrue(spans.contains(RichText.Span(2, 7, Kind.HEADING)))
        val bold = spans.first { it.kind == Kind.BOLD }
        assertEquals("bold", "# Title\nsome **bold** and _it_".substring(bold.start, bold.end))
        val italic = spans.first { it.kind == Kind.ITALIC }
        assertEquals("it", "# Title\nsome **bold** and _it_".substring(italic.start, italic.end))
    }

    @Test
    fun underscoresInsideWordsAreNotItalic() {
        assertTrue(RichText.parse("file_name_here").none { it.kind == Kind.ITALIC })
    }

    @Test
    fun arabicTextSupportsInlineStyles() {
        val text = "مرحبا **عالم** و _جميل_"
        assertEquals("مرحبا عالم و جميل", RichText.plain(text))
    }

    @Test
    fun plainPreviewHidesMarkersAndShowsBullets() {
        assertEquals("Title\n• one\n1. two\nquote", RichText.plain("# Title\n- one\n1. two\n> quote"))
    }

    @Test
    fun visibleRunsKeepStyles() {
        val runs = RichText.visibleRuns("a **b** c")
        assertEquals(listOf("a ", "b", " c"), runs.map { it.text })
        assertEquals(setOf(Kind.BOLD), runs[1].kinds)
    }

    @Test
    fun toggleBulletOnAndOff() {
        val on = RichText.toggleBlock("one\ntwo", 0, 7, Block.BULLET)
        assertEquals("- one\n- two", on.text)
        val off = RichText.toggleBlock(on.text, 0, on.text.length, Block.BULLET)
        assertEquals("one\ntwo", off.text)
    }

    @Test
    fun numberedListNumbersEachLineAndReplacesOtherPrefixes() {
        assertEquals("1. a\n2. b", RichText.toggleBlock("- a\n# b", 0, 7, Block.NUMBER).text)
    }

    @Test
    fun toggleBlockAtCaretKeepsCaretOnText() {
        val e = RichText.toggleBlock("hello", 5, 5, Block.QUOTE)
        assertEquals("> hello", e.text)
        assertEquals(7, e.selectionStart)
    }

    @Test
    fun headingCycles() {
        val h1 = RichText.cycleHeading("x", 1, 1)
        assertEquals("# x", h1.text)
        val h2 = RichText.cycleHeading(h1.text, h1.selectionStart, h1.selectionStart)
        assertEquals("## x", h2.text)
        assertEquals("x", RichText.cycleHeading(h2.text, h2.selectionStart, h2.selectionStart).text)
    }

    @Test
    fun inlineWrapUnwrapAndInsert() {
        val wrapped = RichText.toggleInline("hello world", 6, 11, "**")
        assertEquals("hello **world**", wrapped.text)
        assertEquals(8, wrapped.selectionStart)
        assertEquals(13, wrapped.selectionEnd)
        assertEquals("hello world", RichText.toggleInline(wrapped.text, 8, 13, "**").text)
        val inserted = RichText.toggleInline("ab", 1, 1, "_")
        assertEquals("a__b", inserted.text)
        assertEquals(2, inserted.selectionStart)
    }

    @Test
    fun enterContinuesBulletAndNumberedLists() {
        val b = RichText.continueList("- one", "- one\n", 6)!!
        assertEquals("- one\n- ", b.text)
        assertEquals(8, b.selectionStart)
        val n = RichText.continueList("9. nine", "9. nine\n", 8)!!
        assertEquals("9. nine\n10. ", n.text)
    }

    @Test
    fun enterOnEmptyItemEndsList() {
        val e = RichText.continueList("- one\n- ", "- one\n- \n", 9)!!
        assertEquals("- one\n", e.text)
        assertEquals(6, e.selectionStart)
    }

    @Test
    fun ordinaryNewlinesAreLeftAlone() {
        assertNull(RichText.continueList("plain", "plain\n", 6))
        assertNull(RichText.continueList("# Title", "# Title\n", 8))
        assertNull(RichText.continueList("- a", "- ab", 4))
    }
}
