package app.imadnote.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.imadnote.AppContainer
import app.imadnote.awaitUntil
import app.imadnote.domain.editor.BlockType
import app.imadnote.domain.editor.InlineStyle
import app.imadnote.domain.editor.Span
import app.imadnote.testContainer
import app.imadnote.ui.editor.EditorViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EditorViewModelTest {
    private lateinit var container: AppContainer
    private lateinit var pageId: String
    private lateinit var vm: EditorViewModel

    @Before fun setUp() {
        container = testContainer()
        pageId = runBlocking {
            val ws = container.workspaces.ensureDefault("Personal")
            container.pages.create(ws.id, title = "Test").id
        }
        vm = EditorViewModel(pageId, container)
        awaitUntil { vm.state.doc != null }
    }

    @After fun tearDown() = container.db.close()

    private val blocks get() = vm.state.doc!!.blocks
    private val firstId get() = blocks.first().id

    /** Types [text] at the end of the block one character at a time, like a keyboard would. */
    private fun type(blockId: String, text: String) {
        for (ch in text) {
            val current = vm.state.doc!!.block(blockId)?.text ?: return
            vm.onTextChange(blockId, current + ch, current.length + 1, current.length + 1)
        }
    }

    @Test fun `typing then enter creates a second block and moves the caret`() {
        type(firstId, "Hello")
        vm.onTextChange(firstId, "Hello\n", 6, 6)
        assertEquals(listOf("Hello", ""), blocks.map { it.text })
        assertEquals(blocks[1].id, vm.focus?.blockId)
        assertEquals(blocks[1].id, vm.state.focusedBlockId)
    }

    @Test fun `enter in the middle splits the text`() {
        type(firstId, "HelloWorld")
        vm.onTextChange(firstId, "Hello\nWorld", 6, 6)
        assertEquals(listOf("Hello", "World"), blocks.map { it.text })
    }

    @Test fun `markdown prefix converts the block`() {
        type(firstId, "## ")
        assertEquals(BlockType.HEADING_2, blocks[0].type)
        assertEquals("", blocks[0].text)
        type(firstId, "Plan")
        assertEquals("Plan", blocks[0].text)
    }

    @Test fun `dashes become a divider followed by a text block`() {
        type(firstId, "---")
        assertEquals(listOf(BlockType.DIVIDER, BlockType.PARAGRAPH), blocks.map { it.type })
        assertEquals(blocks[1].id, vm.focus?.blockId)
    }

    @Test fun `slash command replaces the query and undo restores it`() {
        type(firstId, "/tod")
        vm.applySlashCommand(firstId, BlockType.TODO)
        assertEquals(BlockType.TODO, blocks[0].type)
        assertEquals("", blocks[0].text)
        vm.undo()
        assertEquals(BlockType.PARAGRAPH, blocks[0].type)
        assertEquals("/tod", blocks[0].text)
    }

    @Test fun `bold toggled with a collapsed caret applies to the next typed text only`() {
        type(firstId, "plain ")
        vm.onSelectionChange(firstId, 6, 6)
        vm.toggleStyle(InlineStyle.BOLD)
        assertTrue(vm.state.isStyleActive(InlineStyle.BOLD))
        type(firstId, "bold")
        assertEquals(listOf(Span(6, 10, InlineStyle.BOLD)), blocks[0].spans)
        vm.toggleStyle(InlineStyle.BOLD)
        type(firstId, " end")
        assertEquals(listOf(Span(6, 10, InlineStyle.BOLD)), blocks[0].spans)
    }

    @Test fun `formatting a selection toggles on and off`() {
        type(firstId, "hello world")
        vm.onSelectionChange(firstId, 0, 5)
        vm.toggleStyle(InlineStyle.ITALIC)
        assertEquals(listOf(Span(0, 5, InlineStyle.ITALIC)), blocks[0].spans)
        assertTrue(vm.state.isStyleActive(InlineStyle.ITALIC))
        vm.toggleStyle(InlineStyle.ITALIC)
        assertTrue(blocks[0].spans.isEmpty())
    }

    @Test fun `colour applies to the selection and can be cleared`() {
        type(firstId, "hello world")
        vm.onSelectionChange(firstId, 6, 11)
        vm.setColor(InlineStyle.HIGHLIGHT, "green")
        assertEquals(listOf(Span(6, 11, InlineStyle.HIGHLIGHT, "green")), blocks[0].spans)
        vm.setColor(InlineStyle.HIGHLIGHT, null)
        assertTrue(blocks[0].spans.isEmpty())
    }

    @Test fun `backspace at start merges blocks`() {
        type(firstId, "one")
        vm.onTextChange(firstId, "one\n", 4, 4)
        val second = blocks[1].id
        type(second, "two")
        vm.onBackspaceAtStart(second)
        assertEquals(listOf("onetwo"), blocks.map { it.text })
        assertEquals(3, vm.focus?.offset)
    }

    @Test fun `pasting several lines creates blocks and parses markdown`() {
        type(firstId, "start end")
        vm.onTextChange(firstId, "start first\n- item\n# Head\nlast end", 30, 30)
        assertEquals(listOf("start first", "item", "Head", "last end"), blocks.map { it.text })
        assertEquals(listOf(BlockType.PARAGRAPH, BlockType.BULLET, BlockType.HEADING_1, BlockType.PARAGRAPH), blocks.map { it.type })
    }

    @Test fun `block operations - insert, move, duplicate, delete, indent`() {
        type(firstId, "A")
        vm.insertAfterFocused(BlockType.BULLET)
        val b = blocks[1].id
        type(b, "B")
        vm.moveFocused(up = true)
        assertEquals(listOf("B", "A"), blocks.map { it.text })
        vm.duplicateFocused()
        assertEquals(listOf("B", "B", "A"), blocks.map { it.text })
        vm.indentFocused(1)
        assertEquals(listOf(0, 1, 0), blocks.map { it.indent })
        vm.deleteFocused()
        assertEquals(listOf("B", "A"), blocks.map { it.text })
    }

    @Test fun `the document always ends with a text block`() {
        vm.selectBlock(firstId)
        vm.convertFocused(BlockType.TABLE)
        assertEquals(listOf(BlockType.TABLE, BlockType.PARAGRAPH), blocks.map { it.type })
        assertEquals(3, blocks[0].props.cells.size)
    }

    @Test fun `undo and redo walk through edits`() {
        type(firstId, "A")
        vm.convertFocused(BlockType.QUOTE)
        assertTrue(vm.state.canUndo)
        vm.undo()
        assertEquals(BlockType.PARAGRAPH, blocks[0].type)
        vm.redo()
        assertEquals(BlockType.QUOTE, blocks[0].type)
        assertFalse(vm.state.canRedo)
    }

    @Test fun `changes are autosaved and reload in a new editor`() {
        type(firstId, "Autosaved text")
        vm.onSelectionChange(firstId, 0, 9)
        vm.toggleStyle(InlineStyle.BOLD)
        assertFalse(vm.state.saved)
        awaitUntil { vm.state.saved }

        val reopened = EditorViewModel(pageId, container)
        awaitUntil { reopened.state.doc != null }
        val block = reopened.state.doc!!.blocks.single()
        assertEquals("Autosaved text", block.text)
        assertEquals(listOf(Span(0, 9, InlineStyle.BOLD)), block.spans)
    }

    @Test fun `flush saves immediately when leaving the screen`() {
        type(firstId, "Leaving now")
        vm.setTitle("New title")
        vm.flush()
        awaitUntil { runBlocking { container.pages.loadDoc(pageId).blocks.first().text == "Leaving now" } }
        awaitUntil { runBlocking { container.pages.get(pageId)?.title == "New title" } }
    }
}
