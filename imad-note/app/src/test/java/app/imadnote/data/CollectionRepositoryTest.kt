package app.imadnote.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.imadnote.data.db.PropertyType
import app.imadnote.data.db.ViewType
import app.imadnote.domain.db.DbJson
import app.imadnote.testContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionRepositoryTest {
    private val container = testContainer()
    private val repo get() = container.collections
    private val ws = runBlocking { container.workspaces.ensureDefault("Personal").id }

    @Test fun `a new database has a title, status, date and a table view`() = runBlocking {
        val c = repo.create(ws, "Reading list")
        val props = repo.observeProperties(c.id).first()
        assertEquals(listOf(PropertyType.TITLE, PropertyType.STATUS, PropertyType.DATE), props.map { it.type })
        assertEquals(3, DbJson.property(props[1].config).options.size)
        assertEquals(listOf(ViewType.TABLE), repo.observeViews(c.id).first().map { it.type })
    }

    @Test fun `records, cells, duplicate and delete`() = runBlocking {
        val c = repo.create(ws, "Books")
        val title = repo.observeProperties(c.id).first().first()
        val r = repo.addRecord(c.id)
        repo.setCell(r.id, title.id, "Dune")
        val copy = repo.duplicateRecord(r.id)!!
        val cells = repo.observeCells(c.id).first()
        assertEquals(listOf("Dune", "Dune"), cells.filter { it.propertyId == title.id }.map { it.value })
        repo.setCell(r.id, title.id, "")
        assertEquals(1, repo.observeCells(c.id).first().size)
        repo.deleteRecord(copy.id)
        assertEquals(listOf(r.id), repo.observeRecords(c.id).first().map { it.id })
    }

    @Test fun `relations link records across databases and disappear with them`() = runBlocking {
        val authors = repo.create(ws, "Authors")
        val books = repo.create(ws, "Books")
        val author = repo.addRecord(authors.id, mapOf(repo.observeProperties(authors.id).first().first().id to "Herbert"))
        val rel = repo.addProperty(books.id, "Author", PropertyType.RELATION)
        repo.updatePropertyConfig(rel, app.imadnote.domain.db.PropertyConfig(relationTarget = authors.id))
        val book = repo.addRecord(books.id)
        repo.setRelations(book.id, rel.id, listOf(author.id, author.id, book.id))
        assertEquals(listOf(author.id), repo.observeRelations(books.id).first().map { it.toRecordId })
        val titles = repo.observeRecordTitles(ws).first()
        assertEquals("Herbert", titles.single { it.recordId == author.id }.title)
        repo.deleteRecord(author.id)
        assertTrue(repo.observeRelations(books.id).first().isEmpty())
    }

    @Test fun `changing text to select creates options and keeps values`() = runBlocking {
        val c = repo.create(ws, "Tasks")
        val tag = repo.addProperty(c.id, "Tag", PropertyType.TEXT)
        val a = repo.addRecord(c.id, mapOf(tag.id to "Home"))
        val b = repo.addRecord(c.id, mapOf(tag.id to "Work"))
        repo.addRecord(c.id, mapOf(tag.id to "Home"))
        repo.changePropertyType(tag, PropertyType.SELECT)
        val updated = repo.observeProperties(c.id).first().single { it.id == tag.id }
        val options = DbJson.property(updated.config).options
        assertEquals(listOf("Home", "Work"), options.map { it.name })
        val cells = repo.observeCells(c.id).first().filter { it.propertyId == tag.id }.associate { it.recordId to it.value }
        assertEquals(options[0].id, cells[a.id])
        assertEquals(options[1].id, cells[b.id])

        // Removing an option clears values that used it.
        repo.updatePropertyConfig(updated, DbJson.property(updated.config).copy(options = options.drop(1)))
        assertNull(repo.observeCells(c.id).first().firstOrNull { it.recordId == a.id && it.propertyId == tag.id })
    }

    @Test fun `title property cannot be deleted or retyped, last view cannot be deleted`() = runBlocking {
        val c = repo.create(ws, "Misc")
        val title = repo.observeProperties(c.id).first().first()
        repo.deleteProperty(title)
        repo.changePropertyType(title, PropertyType.NUMBER)
        assertEquals(PropertyType.TITLE, repo.observeProperties(c.id).first().first().type)
        val view = repo.observeViews(c.id).first().single()
        assertFalse(repo.deleteView(view))
        repo.addView(c.id, "Board", ViewType.BOARD)
        assertTrue(repo.deleteView(view))
    }

    @Test fun `records are searchable by title and text`() = runBlocking {
        val c = repo.create(ws, "Recipes")
        val title = repo.observeProperties(c.id).first().first()
        val notes = repo.addProperty(c.id, "Notes", PropertyType.TEXT)
        val r = repo.addRecord(c.id, mapOf(title.id to "Couscous"))
        repo.setCell(r.id, notes.id, "steam semolina twice")
        val search = container.db.search()
        assertEquals(c.id, search.search(ws, "couscous*").single().parentRef)
        assertEquals(1, search.search(ws, "semolina*").size)
        repo.delete(c.id)
        assertTrue(search.search(ws, "couscous*").isEmpty())
    }
}
