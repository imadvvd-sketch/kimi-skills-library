package app.imadnote.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.imadnote.AppContainer
import app.imadnote.data.db.AppDatabase
import app.imadnote.data.db.PageKind
import app.imadnote.domain.editor.Block
import app.imadnote.domain.editor.BlockProps
import app.imadnote.domain.editor.BlockType
import app.imadnote.domain.editor.EditorDoc
import app.imadnote.domain.editor.InlineStyle
import app.imadnote.domain.editor.Span
import app.imadnote.testContainer
import app.imadnote.testContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PageRepositoryTest {
    private lateinit var container: AppContainer
    private lateinit var workspaceId: String

    @Before fun setUp() = runBlocking {
        container = testContainer()
        workspaceId = container.workspaces.ensureDefault("Personal").id
    }

    @After fun tearDown() = container.db.close()

    private val pages get() = container.pages

    @Test fun `a new page starts with one empty block`() = runBlocking {
        val page = pages.create(workspaceId, title = "Ideas")
        val doc = pages.loadDoc(page.id)
        assertEquals(1, doc.blocks.size)
        assertEquals(BlockType.PARAGRAPH, doc.blocks[0].type)
    }

    @Test fun `document round trip keeps order, formatting and properties`() = runBlocking {
        val page = pages.create(workspaceId)
        val doc = EditorDoc(
            listOf(
                Block("h", BlockType.HEADING_1, "Title"),
                Block("t", BlockType.TODO, "Buy milk", listOf(Span(0, 3, InlineStyle.BOLD), Span(4, 8, InlineStyle.COLOR, "red")), BlockProps(checked = true), indent = 1),
                Block("tb", BlockType.TABLE, props = BlockProps(cells = listOf(listOf("a", "b"), listOf("c", "d")))),
                Block("c", BlockType.CODE, "val x = 1\nval y = 2", props = BlockProps(language = "kotlin")),
            )
        )
        pages.saveDoc(page.id, doc)
        assertEquals(doc, pages.loadDoc(page.id))
    }

    @Test fun `saving again removes deleted blocks and reorders`() = runBlocking {
        val page = pages.create(workspaceId)
        pages.saveDoc(page.id, EditorDoc(listOf(Block("a", text = "A"), Block("b", text = "B"), Block("c", text = "C"))))
        pages.saveDoc(page.id, EditorDoc(listOf(Block("c", text = "C"), Block("a", text = "A2"))))
        assertEquals(listOf("c" to "C", "a" to "A2"), pages.loadDoc(page.id).blocks.map { it.id to it.text })
    }

    @Test fun `notes survive closing and reopening the database`() = runBlocking {
        val name = "restart-test.db"
        testContext().deleteDatabase(name)
        fun open() = Room.databaseBuilder(testContext(), AppDatabase::class.java, name).build()

        val first = testContainer(db = open())
        val ws = first.workspaces.ensureDefault("Personal").id
        val page = first.pages.create(ws, title = "Persistent")
        first.pages.saveDoc(page.id, EditorDoc(listOf(Block("a", text = "still here", spans = listOf(Span(0, 5, InlineStyle.ITALIC))))))
        first.db.close()

        val second = testContainer(db = open())
        assertEquals("Persistent", second.pages.get(page.id)?.title)
        val block = second.pages.loadDoc(page.id).blocks.single()
        assertEquals("still here", block.text)
        assertEquals(listOf(Span(0, 5, InlineStyle.ITALIC)), block.spans)
        assertEquals(1, second.db.search().search(ws, "still*").size)
        second.db.close()
        testContext().deleteDatabase(name)
        Unit
    }

    @Test fun `trash hides the subtree and restore brings it back`() = runBlocking {
        val parent = pages.create(workspaceId, title = "Parent")
        val child = pages.create(workspaceId, parentId = parent.id, title = "Child")
        pages.trash(parent.id)
        assertTrue(pages.observeActive(workspaceId).first().isEmpty())
        assertEquals(setOf(parent.id, child.id), pages.observeTrashed(workspaceId).first().map { it.id }.toSet())
        pages.restore(parent.id)
        assertEquals(2, pages.observeActive(workspaceId).first().size)
    }

    @Test fun `restoring a child of a trashed parent lifts it to the top level`() = runBlocking {
        val parent = pages.create(workspaceId, title = "Parent")
        val child = pages.create(workspaceId, parentId = parent.id, title = "Child")
        pages.trash(parent.id)
        pages.restore(child.id)
        val active = pages.observeActive(workspaceId).first().single()
        assertEquals(child.id, active.id)
        assertNull(active.parentId)
    }

    @Test fun `delete forever cascades to children, blocks and the search index`() = runBlocking {
        val parent = pages.create(workspaceId, title = "Quarterly report")
        val child = pages.create(workspaceId, parentId = parent.id, title = "Appendix")
        pages.saveDoc(child.id, EditorDoc(listOf(Block("x", text = "zebra crossing"))))
        assertEquals(1, container.db.search().search(workspaceId, "zebra*").size)
        pages.deleteForever(parent.id)
        assertNull(pages.get(child.id))
        assertTrue(container.db.blocks().getAll().isEmpty())
        assertTrue(container.db.search().search(workspaceId, "zebra*").isEmpty())
        assertTrue(container.db.search().search(workspaceId, "quarterly*").isEmpty())
    }

    @Test fun `a page cannot be moved into its own descendant`() = runBlocking {
        val a = pages.create(workspaceId, title = "A")
        val b = pages.create(workspaceId, parentId = a.id, title = "B")
        val c = pages.create(workspaceId, parentId = b.id, title = "C")
        assertFalse(pages.move(a.id, c.id))
        assertFalse(pages.move(a.id, a.id))
        assertTrue(pages.move(c.id, null))
        assertNull(pages.get(c.id)?.parentId)
    }

    @Test fun `search finds titles and block text, including Arabic, and follows edits`() = runBlocking {
        val page = pages.create(workspaceId, title = "Travel plans")
        pages.saveDoc(page.id, EditorDoc(listOf(Block("a", text = "Book the flight to Algiers"), Block("b", text = "قائمة المشتريات اليومية"))))
        val search = container.db.search()
        assertEquals(listOf("page"), search.search(workspaceId, "travel*").map { it.kind })
        assertEquals(listOf("a"), search.search(workspaceId, "flight*").map { it.refId })
        assertEquals(page.id, search.search(workspaceId, "flight*").single().parentRef)
        assertEquals(listOf("b"), search.search(workspaceId, "المشتريات*").map { it.refId })
        pages.saveDoc(page.id, EditorDoc(listOf(Block("a", text = "Book the train"), Block("b", text = "قائمة"))))
        assertTrue(search.search(workspaceId, "flight*").isEmpty())
        assertEquals(1, search.search(workspaceId, "train*").size)
    }

    @Test fun `duplicate copies content under new ids`() = runBlocking {
        val page = pages.create(workspaceId, title = "Template", icon = "📄")
        pages.saveDoc(page.id, EditorDoc(listOf(Block("a", BlockType.HEADING_2, "Agenda"))))
        val copy = pages.duplicate(page.id)!!
        val block = pages.loadDoc(copy.id).blocks.single()
        assertEquals("Agenda", block.text)
        assertEquals(BlockType.HEADING_2, block.type)
        assertFalse(block.id == "a")
        assertEquals("Template", copy.title)
    }

    @Test fun `folders hold pages and concurrent metadata edits do not clobber each other`() = runBlocking {
        val folder = pages.create(workspaceId, kind = PageKind.FOLDER, title = "Work")
        val page = pages.create(workspaceId, parentId = folder.id, title = "Notes")
        pages.edit(page.id) { it.copy(isFavorite = true) }
        pages.edit(page.id) { it.copy(title = "Renamed") }
        val stored = pages.get(page.id)!!
        assertTrue(stored.isFavorite)
        assertEquals("Renamed", stored.title)
        assertTrue(pages.loadDoc(folder.id).blocks.isEmpty())
    }
}
