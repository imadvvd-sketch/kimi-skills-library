package app.imadnote.domain.tasks

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class TasksDomainTest {
    private val d = LocalDate.of(2026, 1, 30) // a Friday

    @Test fun `recurrence rules`() {
        assertEquals(LocalDate.of(2026, 1, 31), Recurrence(Recurrence.Unit.DAY).next(d))
        assertEquals(LocalDate.of(2026, 2, 13), Recurrence(Recurrence.Unit.WEEK, 2).next(d))
        assertEquals(LocalDate.of(2026, 2, 28), Recurrence(Recurrence.Unit.MONTH).next(d))
        assertEquals(LocalDate.of(2027, 1, 30), Recurrence(Recurrence.Unit.YEAR).next(d))
        assertEquals(LocalDate.of(2026, 2, 2), Recurrence(Recurrence.Unit.WEEKDAY).next(d))
    }

    @Test fun `recurrence encoding round trip and bad input`() {
        for (unit in Recurrence.Unit.entries) assertEquals(Recurrence(unit, 3), Recurrence.parse(Recurrence(unit, 3).encode()))
        assertNull(Recurrence.parse("nonsense"))
        assertNull(Recurrence.parse(null))
        assertEquals(Recurrence(Recurrence.Unit.DAY, 1), Recurrence.parse("D"))
    }

    @Test fun `quick parser understands dates, priority and labels in both languages`() {
        val en = TaskQuickParser.parse("Call mum tomorrow !3 #family", d)
        assertEquals("Call mum", en.title)
        assertEquals(d.plusDays(1), en.dueDate)
        assertEquals(Priority.HIGH, en.priority)
        assertEquals(listOf("family"), en.labels)

        val ar = TaskQuickParser.parse("شراء الخبز اليوم !1", d)
        assertEquals("شراء الخبز", ar.title)
        assertEquals(d, ar.dueDate)
        assertEquals(Priority.LOW, ar.priority)
        assertEquals(d.plusDays(1), TaskQuickParser.parse("موعد غدا", d).dueDate)
    }

    @Test fun `a title made only of shortcuts is kept as typed`() {
        val result = TaskQuickParser.parse("today", d)
        assertEquals("today", result.title)
        assertNull(result.dueDate)
    }
}
