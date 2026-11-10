package app.imadnote.domain

import app.imadnote.data.db.ProjectEntity
import app.imadnote.data.db.TaskEntity
import app.imadnote.data.db.TaskStatus
import app.imadnote.domain.analytics.Analytics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class AnalyticsTest {
    private val zone = ZoneOffset.UTC
    private val today = LocalDate.of(2026, 6, 10) // Wednesday
    private fun at(d: LocalDate) = d.atStartOfDay(zone).plusHours(10).toInstant().toEpochMilli()
    private fun task(id: String, created: LocalDate, done: LocalDate? = null, due: LocalDate? = null, priority: Int = 0, project: String? = null) = TaskEntity(
        id, "w", projectId = project, title = id, priority = priority, status = if (done != null) TaskStatus.DONE else TaskStatus.TODO,
        dueDate = due?.toEpochDay(), completedAt = done?.let(::at), createdAt = at(created), updatedAt = 0,
    )

    @Test fun `counts, rates, trends and streak come from the tasks`() {
        val tasks = listOf(
            task("a", today.minusDays(3), done = today),
            task("b", today.minusDays(3), done = today.minusDays(1)),
            task("c", today.minusDays(2), done = today.minusDays(2), project = "p"),
            task("d", today.minusDays(20), done = today.minusDays(9)),
            task("e", today, due = today.minusDays(1), priority = 3, project = "p"),
            task("f", today, priority = 1),
        )
        val project = ProjectEntity("p", "w", "Launch", createdAt = 0, updatedAt = 0)
        val r = Analytics.compute(tasks, listOf(project), today, zone)
        assertEquals(4, r.completed)
        assertEquals(2, r.open)
        assertEquals(1, r.overdue)
        assertEquals(4f / 6f, r.completionRate, 0.0001f)
        assertEquals(listOf(0, 0, 0, 0, 1, 1, 1), r.last7.map { it.done })
        assertEquals(2, r.last7.last().created)
        assertEquals(3, r.streak)
        assertEquals(3, r.doneThisWeek) // Monday 8th onwards: the 8th, 9th and 10th
        assertEquals(1, r.doneLastWeek)
        assertEquals(mapOf(3 to 1, 2 to 0, 1 to 1, 0 to 0), r.openByPriority)
        assertEquals(1, r.projects.single().done)
        assertEquals(2, r.projects.single().total)
        assertEquals(4, r.months.last().done + r.months[r.months.size - 2].done)
    }

    @Test fun `no tasks means an empty report`() {
        val r = Analytics.compute(emptyList(), emptyList(), today, zone)
        assertTrue(r.isEmpty)
        assertEquals(0, r.streak)
        assertEquals(0f, r.completionRate)
    }
}
