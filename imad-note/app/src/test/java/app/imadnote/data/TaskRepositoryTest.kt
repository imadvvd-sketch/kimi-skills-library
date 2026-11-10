package app.imadnote.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.imadnote.FakeReminders
import app.imadnote.data.db.EventEntity
import app.imadnote.data.db.TaskStatus
import app.imadnote.data.repo.EventRepository
import app.imadnote.data.repo.TaskRepository
import app.imadnote.domain.tasks.Recurrence
import app.imadnote.testContainer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class TaskRepositoryTest {
    private val container = testContainer()
    private val reminders get() = container.reminders as FakeReminders
    private val ws = runBlocking { container.workspaces.ensureDefault("Personal").id }
    private val tasks get() = container.tasks

    @Test fun `create with labels, then labels are shared by name`() = runBlocking {
        val a = tasks.create(ws, "Write report", labelNames = listOf("work"))
        val b = tasks.create(ws, "Email boss", labelNames = listOf("Work", "urgent"))
        val labels = tasks.observeLabels(ws).first()
        assertEquals(setOf("work", "urgent"), labels.map { it.name }.toSet())
        val refs = tasks.observeTaskLabels().first()
        assertEquals(1, refs.count { it.taskId == a.id })
        assertEquals(2, refs.count { it.taskId == b.id })
    }

    @Test fun `completing and reopening`() = runBlocking {
        val task = tasks.create(ws, "Plain")
        assertNull(tasks.setCompleted(task.id, true))
        val done = tasks.get(task.id)!!
        assertEquals(TaskStatus.DONE, done.status)
        assertNotNull(done.completedAt)
        tasks.setCompleted(task.id, false)
        assertNull(tasks.get(task.id)!!.completedAt)
    }

    @Test fun `completing a repeating task creates the next occurrence with subtasks and labels`() = runBlocking {
        val due = LocalDate.of(2026, 3, 2)
        val task = tasks.create(ws, "Water plants", dueDate = due, labelNames = listOf("home"))
        tasks.edit(task.id) { it.copy(recurrence = Recurrence(Recurrence.Unit.WEEK).encode(), reminderAt = 4_000_000_000_000) }
        val sub = tasks.create(ws, "Balcony", parentId = task.id)
        tasks.setCompleted(sub.id, true)

        val next = tasks.setCompleted(task.id, true)!!
        assertEquals(due.plusWeeks(1).toEpochDay(), next.dueDate)
        assertEquals(TaskStatus.TODO, next.status)
        assertTrue(next.reminderAt!! > 4_000_000_000_000)
        val nextSubs = container.db.tasks().subtasks(next.id)
        assertEquals(listOf("Balcony"), nextSubs.map { it.title })
        assertEquals(TaskStatus.TODO, nextSubs.single().status)
        assertEquals(1, tasks.observeTaskLabels().first().count { it.taskId == next.id })
        assertEquals(setOf(TaskRepository.key(next.id)), reminders.scheduled.keys)
        // Completing an already completed occurrence does not spawn another one.
        assertNull(tasks.setCompleted(task.id, true))
    }

    @Test fun `reminders follow the task state and deletion`() = runBlocking {
        val task = tasks.create(ws, "Dentist")
        tasks.edit(task.id) { it.copy(reminderAt = 4_000_000_000_000) }
        assertTrue(TaskRepository.key(task.id) in reminders.scheduled)
        tasks.setCompleted(task.id, true)
        assertFalse(TaskRepository.key(task.id) in reminders.scheduled)
        tasks.setCompleted(task.id, false)
        assertTrue(TaskRepository.key(task.id) in reminders.scheduled)
        tasks.delete(task.id)
        assertTrue(reminders.scheduled.isEmpty())
        assertNull(tasks.get(task.id))
    }

    @Test fun `deleting a project keeps its tasks and notes`() = runBlocking {
        val project = container.projects.create(ws, "Garden")
        val task = tasks.create(ws, "Dig", projectId = project.id)
        val page = container.pages.create(ws, title = "Plants list", projectId = project.id)
        container.projects.delete(project.id)
        assertNull(tasks.get(task.id)!!.projectId)
        assertNull(container.pages.get(page.id)!!.projectId)
    }

    @Test fun `tasks, projects and events are searchable`() = runBlocking {
        tasks.create(ws, "Renew passport")
        container.projects.create(ws, "Kitchen renovation")
        container.events.save(EventEntity("", ws, "Dentist appointment", startAt = 1000, endAt = 2000, createdAt = 0, updatedAt = 0))
        val search = container.db.search()
        assertEquals(listOf("task"), search.search(ws, "passport*").map { it.kind })
        assertEquals(listOf("project"), search.search(ws, "renovation*").map { it.kind })
        assertEquals(listOf("event"), search.search(ws, "dentist*").map { it.kind })
    }

    @Test fun `events keep end after start and schedule reminders`() = runBlocking {
        val saved = container.events.save(EventEntity("", ws, "Call", startAt = 5000, endAt = 1000, reminderAt = 4_000_000_000_000, createdAt = 0, updatedAt = 0))
        assertEquals(5000, saved.endAt)
        assertTrue(EventRepository.key(saved.id) in reminders.scheduled)
        container.events.delete(saved.id)
        assertTrue(reminders.scheduled.isEmpty())
    }
}
