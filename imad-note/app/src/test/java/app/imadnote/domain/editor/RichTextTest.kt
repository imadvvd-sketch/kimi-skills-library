package app.imadnote.domain.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RichTextTest {
    private fun bold(start: Int, end: Int) = Span(start, end, InlineStyle.BOLD)

    @Test fun `typing before a span shifts it`() {
        assertEquals(listOf(bold(8, 13)), RichText.adjustForEdit("hello world", "hi hello world", listOf(bold(5, 10))))
    }

    @Test fun `typing at the end of a span extends it`() {
        assertEquals(listOf(bold(0, 6)), RichText.adjustForEdit("hello world", "hello! world", listOf(bold(0, 5))))
    }

    @Test fun `typing right before a span does not extend it`() {
        assertEquals(listOf(bold(7, 12)), RichText.adjustForEdit("hello world", "hello Xworld", listOf(bold(6, 11))))
    }

    @Test fun `deleting inside a span shrinks it`() {
        assertEquals(listOf(bold(0, 3)), RichText.adjustForEdit("hello world", "hlo world", listOf(bold(0, 5))))
    }

    @Test fun `deleting a whole span removes it`() {
        assertEquals(emptyList<Span>(), RichText.adjustForEdit("hello world", "hello ", listOf(bold(6, 11))))
    }

    @Test fun `replacing a selection that overlaps a span keeps the untouched part`() {
        // "hello [wor]ld" -> replace "lo wor" with "X"
        assertEquals(listOf(bold(4, 6)), RichText.adjustForEdit("hello world", "helXld", listOf(bold(6, 11))))
    }

    @Test fun `toggle adds then removes`() {
        val added = RichText.toggle(emptyList(), 2, 6, InlineStyle.BOLD)
        assertEquals(listOf(bold(2, 6)), added)
        assertEquals(emptyList<Span>(), RichText.toggle(added, 2, 6, InlineStyle.BOLD))
    }

    @Test fun `toggle on a partly formatted range formats all of it`() {
        assertEquals(listOf(bold(0, 8)), RichText.toggle(listOf(bold(0, 3)), 2, 8, InlineStyle.BOLD))
    }

    @Test fun `removing the middle splits a span`() {
        assertEquals(listOf(bold(0, 2), bold(4, 8)), RichText.remove(listOf(bold(0, 8)), 2, 4, InlineStyle.BOLD))
    }

    @Test fun `isActive requires full coverage across adjacent spans`() {
        val spans = listOf(bold(0, 3), bold(3, 6))
        assertTrue(RichText.isActive(spans, 1, 6, InlineStyle.BOLD))
        assertFalse(RichText.isActive(spans, 1, 7, InlineStyle.BOLD))
        assertFalse(RichText.isActive(spans, 1, 6, InlineStyle.ITALIC))
    }

    @Test fun `a new colour replaces the old colour in the range`() {
        val red = RichText.add(emptyList(), 0, 10, InlineStyle.COLOR, "red")
        val mixed = RichText.add(red, 4, 6, InlineStyle.COLOR, "blue")
        assertEquals(
            listOf(Span(0, 4, InlineStyle.COLOR, "red"), Span(4, 6, InlineStyle.COLOR, "blue"), Span(6, 10, InlineStyle.COLOR, "red")),
            mixed,
        )
    }

    @Test fun `split and join are inverse`() {
        val spans = listOf(bold(2, 9), Span(0, 4, InlineStyle.ITALIC))
        val (head, tail) = RichText.split(spans, 5)
        assertEquals(listOf(Span(0, 4, InlineStyle.ITALIC), bold(2, 5)), head)
        assertEquals(listOf(bold(0, 4)), tail)
        assertEquals(RichText.normalize(spans), RichText.join(head, 5, tail))
    }

    @Test fun `spans survive json round trip`() {
        val spans = listOf(bold(0, 3), Span(1, 2, InlineStyle.LINK, "https://example.com"))
        assertEquals(spans, BlockJson.decodeSpans(BlockJson.encodeSpans(spans)))
        assertEquals(emptyList<Span>(), BlockJson.decodeSpans("not json"))
    }
}

class EquationTest {
    @Test fun `renders symbols scripts fractions and roots`() {
        assertEquals("E = mc²", Equation.render("E = mc^2"))
        assertEquals("α + β ≤ π", Equation.render("\\alpha + \\beta \\leq \\pi"))
        assertEquals("x₁₀ⁿ", Equation.render("x_{10}^n"))
        assertEquals("(a)⁄(b)", Equation.render("\\frac{a}{b}"))
        assertEquals("√((1)⁄(x²))", Equation.render("\\sqrt{\\frac{1}{x^2}}"))
    }

    @Test fun `unsupported input is left as typed`() {
        assertEquals("\\matrix x^{q}", Equation.render("\\matrix x^{q}"))
    }
}
