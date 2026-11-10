package app.imadnote.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.imadnote.domain.editor.Block
import app.imadnote.domain.editor.EditorDoc
import app.imadnote.testContainer
import app.imadnote.testSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkspaceRepositoryTest {
    @Test fun `the personal workspace is created once`() = runBlocking {
        val container = testContainer()
        val first = container.workspaces.ensureDefault("Personal")
        val second = container.workspaces.ensureDefault("Personal")
        assertEquals(first.id, second.id)
        assertEquals(1, container.workspaces.workspaces.first().size)
        assertEquals(first.id, container.workspaces.current.first()?.id)
    }

    @Test fun `creating a workspace switches to it and content stays separate`() = runBlocking {
        val container = testContainer()
        val personal = container.workspaces.ensureDefault("Personal")
        container.pages.create(personal.id, title = "Private")
        val work = container.workspaces.create("Work", "💼")
        assertEquals(work.id, container.workspaces.current.first()?.id)
        assertTrue(container.pages.observeActive(work.id).first().isEmpty())
        assertEquals(1, container.pages.observeActive(personal.id).first().size)
    }

    @Test fun `deleting a workspace removes its content and index, but never the last one`() = runBlocking {
        val container = testContainer()
        val personal = container.workspaces.ensureDefault("Personal")
        assertFalse(container.workspaces.delete(personal.id))
        val work = container.workspaces.create("Work", "💼")
        val page = container.pages.create(work.id, title = "Roadmap")
        container.pages.saveDoc(page.id, EditorDoc(listOf(Block("a", text = "milestone"))))
        assertTrue(container.workspaces.delete(work.id))
        assertEquals(personal.id, container.workspaces.current.first()?.id)
        assertTrue(container.db.pages().getAll().isEmpty())
        assertTrue(container.db.search().search(work.id, "milestone*").isEmpty())
    }

    @Test fun `theme choice persists in the settings store`() = runBlocking {
        val settings = testSettings()
        assertEquals(ThemeMode.SYSTEM, settings.themeMode.first())
        settings.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, settings.themeMode.first())
    }
}
