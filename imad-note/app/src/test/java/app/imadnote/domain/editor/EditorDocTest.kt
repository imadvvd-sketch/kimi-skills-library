package app.imadnote.domain.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorDocTest {
    private fun doc(vararg blocks: Block) = EditorDoc(blocks.toList())
    private fun p(id: String, text: String = id, indent: Int = 0, type: BlockType = BlockType.PARAGRAPH, props: BlockProps = BlockProps()) =
        Block(id, type, text, indent = indent, props = props)

    private fun EditorDoc.ids() = blocks.map { it.id }

    @Test fun `enter splits text and spans`() {
        val start = doc(Block("a", text = "hello world", spans = listOf(Span(3, 8, InlineStyle.BOLD))))
        val result = start.split("a", 5, "b")
        assertEquals("hello", result.doc.blocks[0].text)
        assertEquals(listOf(Span(3, 5, InlineStyle.BOLD)), result.doc.blocks[0].spans)
        assertEquals(" world", result.doc.blocks[1].text)
        assertEquals(listOf(Span(0, 3, InlineStyle.BOLD)), result.doc.blocks[1].spans)
        assertEquals(Caret("b", 0), result.caret)
    }

    @Test fun `enter continues a list but not a heading`() {
        assertEquals(BlockType.BULLET, doc(p("a", type = BlockType.BULLET)).split("a", 1, "b").doc.blocks[1].type)
        assertEquals(BlockType.PARAGRAPH, doc(p("a", type = BlockType.HEADING_1)).split("a", 1, "b").doc.blocks[1].type)
    }

    @Test fun `a new todo item is unchecked`() {
        val result = doc(p("a", type = BlockType.TODO, props = BlockProps(checked = true))).split("a", 1, "b")
        assertFalse(result.doc.blocks[1].props.checked)
    }

    @Test fun `enter on an empty list item leaves the list`() {
        val nested = doc(p("a", type = BlockType.BULLET), p("b", "", indent = 1, type = BlockType.BULLET))
        val outdented = nested.split("b", 0, "x").doc
        assertEquals(0, outdented.blocks[1].indent)
        assertEquals(BlockType.BULLET, outdented.blocks[1].type)
        assertEquals(BlockType.PARAGRAPH, outdented.split("b", 0, "x").doc.blocks[1].type)
        assertEquals(2, outdented.blocks.size)
    }

    @Test fun `enter at the end of an open toggle creates a child`() {
        val result = doc(p("a", "Details", type = BlockType.TOGGLE)).split("a", 7, "b").doc
        assertEquals(1, result.blocks[1].indent)
        assertEquals(BlockType.PARAGRAPH, result.blocks[1].type)
    }

    @Test fun `enter on a collapsed toggle inserts after its hidden children`() {
        val start = doc(p("a", type = BlockType.TOGGLE, props = BlockProps(collapsed = true)), p("child", indent = 1), p("z"))
        assertEquals(listOf("a", "child", "new", "z"), start.split("a", 1, "new").doc.ids())
    }

    @Test fun `backspace converts a styled block to a paragraph first`() {
        val result = doc(p("a"), p("b", type = BlockType.HEADING_2)).mergeBackward("b")
        assertEquals(BlockType.PARAGRAPH, result.doc.blocks[1].type)
        assertEquals(2, result.doc.blocks.size)
    }

    @Test fun `backspace merges into the previous block and places the caret at the seam`() {
        val start = doc(Block("a", text = "foo"), Block("b", text = "bar", spans = listOf(Span(0, 3, InlineStyle.ITALIC))))
        val result = start.mergeBackward("b")
        assertEquals(listOf("a"), result.doc.ids())
        assertEquals("foobar", result.doc.blocks[0].text)
        assertEquals(listOf(Span(3, 6, InlineStyle.ITALIC)), result.doc.blocks[0].spans)
        assertEquals(Caret("a", 3), result.caret)
    }

    @Test fun `backspace after a divider removes the divider`() {
        val result = doc(p("a"), p("d", "", type = BlockType.DIVIDER), p("b")).mergeBackward("b")
        assertEquals(listOf("a", "b"), result.doc.ids())
    }

    @Test fun `backspace on the first block is a no-op`() {
        val start = doc(p("a"))
        assertEquals(start, start.mergeBackward("a").doc)
    }

    @Test fun `move down carries children and skips the next subtree`() {
        val start = doc(p("a"), p("a1", indent = 1), p("b"), p("b1", indent = 1), p("c"))
        assertEquals(listOf("b", "b1", "a", "a1", "c"), start.move("a", up = false).ids())
        assertEquals(listOf("a", "a1", "c", "b", "b1"), start.move("b", up = false).ids())
        assertEquals(start, start.move("c", up = false))
    }

    @Test fun `move up carries children above the previous sibling subtree`() {
        val start = doc(p("a"), p("a1", indent = 1), p("b"), p("b1", indent = 1))
        assertEquals(listOf("b", "b1", "a", "a1"), start.move("b", up = true).ids())
        assertEquals(start, start.move("a", up = true))
    }

    @Test fun `moving never leaves an orphaned indent`() {
        val moved = doc(p("a"), p("a1", indent = 1)).move("a1", up = true)
        assertEquals(listOf("a1", "a"), moved.ids())
        assertEquals(0, moved.blocks[0].indent)
    }

    @Test fun `indent is limited to one level below the previous block and moves children`() {
        val start = doc(p("a"), p("b"), p("b1", indent = 1))
        val indented = start.indent("b", 1)
        assertEquals(listOf(0, 1, 2), indented.blocks.map { it.indent })
        assertEquals(indented, indented.indent("b", 1))
        assertEquals(start, start.indent("a", 1))
        assertEquals(start, indented.indent("b", -1))
    }

    @Test fun `delete promotes children`() {
        val result = doc(p("a"), p("a1", indent = 1), p("a2", indent = 2)).delete("a")
        assertEquals(listOf("a1", "a2"), result.doc.ids())
        assertEquals(listOf(0, 1), result.doc.blocks.map { it.indent })
    }

    @Test fun `duplicate copies the subtree with new ids`() {
        var n = 0
        val result = doc(p("a"), p("a1", indent = 1), p("b")).duplicate("a") { "n${n++}" }
        assertEquals(listOf("a", "a1", "n0", "n1", "b"), result.doc.ids())
        assertEquals("a1", result.doc.blocks[3].text)
    }

    @Test fun `collapsed toggles hide descendants only`() {
        val start = doc(p("t", type = BlockType.TOGGLE, props = BlockProps(collapsed = true)), p("x", indent = 1), p("y", indent = 2), p("z"))
        assertEquals(listOf("t", "z"), start.visibleBlocks().map { it.id })
    }

    @Test fun `numbering restarts after a break and per indent`() {
        val n = BlockType.NUMBERED
        val start = doc(p("a", type = n), p("b", type = n), p("b1", indent = 1, type = n), p("c", type = n), p("x"), p("d", type = n))
        assertEquals(mapOf("a" to 1, "b" to 2, "b1" to 1, "c" to 3, "d" to 1), start.numbering())
    }

    @Test fun `converting to a non-text block clears text and seeds a table`() {
        val table = doc(p("a", "hello")).convert("a", BlockType.TABLE).blocks[0]
        assertEquals("", table.text)
        assertEquals(3, table.props.cells.size)
    }

    @Test fun `history undoes and redoes, coalescing typing`() {
        val history = EditorHistory()
        val v0 = doc(p("a", ""))
        val v1 = v0.setText("a", "h", emptyList())
        val v2 = v1.setText("a", "hi", emptyList())
        history.record(v0, typingBlockId = "a", now = 0)
        history.record(v1, typingBlockId = "a", now = 100)
        assertEquals(v0, history.undo(v2))
        assertEquals(v2, history.redo(v0))
        assertNull(history.redo(v2))
        assertTrue(history.canUndo)
    }

    @Test fun `a new edit clears redo`() {
        val history = EditorHistory()
        val v0 = doc(p("a", "0"))
        val v1 = doc(p("a", "1"))
        history.record(v0)
        history.undo(v1)
        history.record(v0)
        assertFalse(history.canRedo)
    }

    @Test fun `markdown shortcuts`() {
        assertEquals(BlockType.HEADING_2, Markdown.shortcutFor("## Title")!!.type)
        assertEquals("Title", Markdown.shortcutFor("## Title")!!.remaining)
        assertEquals(BlockType.TODO, Markdown.shortcutFor("[] buy milk")!!.type)
        assertNull(Markdown.shortcutFor("#hashtag"))
    }

    @Test fun `markdown parse and export round trip`() {
        val source = "# Plan\n- [x] done\n- [ ] next\n  - nested\n1. one\n2. two\n> quote\n---\n```kotlin\nval x = 1\n```\nplain"
        var n = 0
        val blocks = Markdown.parse(source) { "b${n++}" }
        assertEquals(
            listOf(BlockType.HEADING_1, BlockType.TODO, BlockType.TODO, BlockType.BULLET, BlockType.NUMBERED, BlockType.NUMBERED,
                BlockType.QUOTE, BlockType.DIVIDER, BlockType.CODE, BlockType.PARAGRAPH),
            blocks.map { it.type },
        )
        assertTrue(blocks[1].props.checked)
        assertEquals(1, blocks[3].indent)
        assertEquals("val x = 1", blocks[8].text)
        assertEquals(source, Markdown.export(EditorDoc(blocks)))
    }
}
