package app.imadnote.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.imadnote.data.backup.BackupException
import app.imadnote.data.db.AttachmentEntity
import app.imadnote.data.db.AttachmentOwner
import app.imadnote.data.db.CellEntity
import app.imadnote.data.db.CollectionEntity
import app.imadnote.data.db.ProjectEntity
import app.imadnote.data.db.PropertyEntity
import app.imadnote.data.db.PropertyType
import app.imadnote.data.db.RecordEntity
import app.imadnote.data.db.TaskEntity
import app.imadnote.domain.editor.Block
import app.imadnote.domain.editor.EditorDoc
import app.imadnote.testContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class BackupManagerTest {
    @Test fun `export then restore into an empty device reproduces content, files and search`() = runBlocking {
        val source = testContainer()
        val ws = source.workspaces.ensureDefault("Personal").id
        val parent = source.pages.create(ws, title = "Parent")
        val child = source.pages.create(ws, parentId = parent.id, title = "Child note")
        source.pages.saveDoc(child.id, EditorDoc(listOf(Block("b1", text = "remember the passport"))))
        val project = ProjectEntity("p1", ws, "Move house", createdAt = 1, updatedAt = 1)
        source.db.projects().upsert(project)
        source.db.tasks().upsert(TaskEntity("t1", ws, projectId = "p1", title = "Book movers", createdAt = 1, updatedAt = 1))
        source.db.tasks().upsert(TaskEntity("t2", ws, parentId = "t1", title = "Get quotes", createdAt = 1, updatedAt = 1))
        source.db.collections().upsert(CollectionEntity("c1", ws, "Books", createdAt = 1, updatedAt = 1))
        source.db.collections().upsertProperty(PropertyEntity("title", "c1", "Name", PropertyType.TITLE, sortIndex = 0))
        source.db.collections().upsertRecord(RecordEntity("r1", "c1", 0, 1, 1))
        source.db.collections().upsertCell(CellEntity("r1", "title", "Dune"))
        File(source.attachments.directory, "a1.txt").writeText("file body")
        source.db.attachments().upsert(AttachmentEntity("a1", ws, AttachmentOwner.PAGE, child.id, "notes.txt", "text/plain", 9, "a1.txt", 1))

        val bytes = ByteArrayOutputStream().also { source.backup.export(it) }.toByteArray()
        val original = source.backup.snapshot()
        source.db.close()
        source.attachments.directory.listFiles()?.forEach { it.delete() }

        val target = testContainer()
        target.workspaces.ensureDefault("Something else")
        val summary = target.backup.restore(ByteArrayInputStream(bytes))
        assertEquals(2, summary.pages)
        assertEquals(2, summary.tasks)

        val restored = target.backup.snapshot()
        assertEquals(original.copy(createdAt = 0), restored.copy(createdAt = 0))
        assertEquals("file body", File(target.attachments.directory, "a1.txt").readText())
        assertEquals(ws, target.workspaces.current.first()?.id)
        val search = target.db.search()
        assertEquals(listOf("b1"), search.search(ws, "passport*").map { it.refId })
        assertEquals(listOf("t1"), search.search(ws, "movers*").map { it.refId })
        assertEquals(listOf("r1"), search.search(ws, "dune*").map { it.refId })
        target.db.close()
    }

    @Test fun `an invalid file leaves existing data untouched`() = runBlocking {
        val container = testContainer()
        val ws = container.workspaces.ensureDefault("Personal").id
        container.pages.create(ws, title = "Keep me")
        val junk = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { it.putNextEntry(ZipEntry("backup.json")); it.write("{not json".toByteArray()); it.closeEntry() }
        }.toByteArray()
        try {
            container.backup.restore(ByteArrayInputStream(junk))
            fail("expected a BackupException")
        } catch (_: BackupException) {
        }
        try {
            container.backup.restore(ByteArrayInputStream("plain text".toByteArray()))
            fail("expected a BackupException")
        } catch (_: BackupException) {
        }
        assertEquals("Keep me", container.db.pages().getAll().single().title)
        container.db.close()
    }

    @Test fun `entries that try to escape the attachments folder are ignored`() = runBlocking {
        val container = testContainer()
        val ws = container.workspaces.ensureDefault("Personal").id
        val good = ByteArrayOutputStream().also { container.backup.export(it) }.toByteArray()
        val evil = ByteArrayOutputStream().also { out ->
            ZipOutputStream(out).use { zip ->
                java.util.zip.ZipInputStream(ByteArrayInputStream(good)).use { src ->
                    while (true) {
                        val e = src.nextEntry ?: break
                        zip.putNextEntry(ZipEntry(e.name)); zip.write(src.readBytes()); zip.closeEntry()
                    }
                }
                zip.putNextEntry(ZipEntry("attachments/../../escaped.txt")); zip.write("x".toByteArray()); zip.closeEntry()
            }
        }.toByteArray()
        container.backup.restore(ByteArrayInputStream(evil))
        assertTrue(container.attachments.directory.parentFile!!.walk().none { it.name == "escaped.txt" })
        assertEquals(ws, container.workspaces.current.first()?.id)
        container.db.close()
    }
}
