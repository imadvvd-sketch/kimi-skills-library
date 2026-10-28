package com.imadnote.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class TaskRulesTest {
    private val zone: ZoneId = ZoneId.of("Africa/Algiers")
    private val today: LocalDate = LocalDate.of(2026, 10, 28)

    private fun at(y: Int, m: Int, d: Int, h: Int = 0, min: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, min).atZone(zone).toInstant().toEpochMilli()

    private fun task(dueAt: Long?, hasTime: Boolean = false, completedAt: Long? = null, reminder: Int? = null) =
        TaskTiming(dueAt, hasTime, completedAt, reminder)

    @Test
    fun overdueIsDateBasedAndIgnoresCompleted() {
        assertTrue(TaskRules.isOverdue(task(at(2026, 10, 27, 23, 59)), today, zone))
        assertFalse(TaskRules.isOverdue(task(at(2026, 10, 28, 0, 1)), today, zone))
        assertFalse(TaskRules.isOverdue(task(at(2026, 10, 20), completedAt = 1L), today, zone))
        assertFalse(TaskRules.isOverdue(task(null), today, zone))
    }

    @Test
    fun todayAndUpcomingBuckets() {
        assertTrue(TaskRules.isDueToday(task(at(2026, 10, 28, 18)), today, zone))
        assertFalse(TaskRules.isDueToday(task(at(2026, 10, 29)), today, zone))
        assertTrue(TaskRules.isUpcoming(task(at(2026, 10, 29)), today, zone))
        assertTrue(TaskRules.isUpcoming(task(at(2026, 11, 4)), today, zone, withinDays = 7))
        assertFalse(TaskRules.isUpcoming(task(at(2026, 11, 5)), today, zone, withinDays = 7))
        assertFalse(TaskRules.isUpcoming(task(at(2026, 10, 28)), today, zone))
    }

    @Test
    fun reminderForTimedTaskSubtractsOffset() {
        val due = at(2026, 10, 30, 14, 0)
        assertEquals(due - 10 * 60_000L, TaskRules.reminderTime(task(due, hasTime = true, reminder = 10), zone))
        assertEquals(due, TaskRules.reminderTime(task(due, hasTime = true, reminder = 0), zone))
    }

    @Test
    fun reminderForAllDayTaskUsesNineAm() {
        val due = at(2026, 10, 30)
        assertEquals(at(2026, 10, 30, 9, 0), TaskRules.reminderTime(task(due, reminder = 0), zone))
        assertEquals(at(2026, 10, 29, 9, 0), TaskRules.reminderTime(task(due, reminder = 1440), zone))
    }

    @Test
    fun noReminderWithoutDateOrOffset() {
        assertNull(TaskRules.reminderTime(task(null, reminder = 10), zone))
        assertNull(TaskRules.reminderTime(task(at(2026, 10, 30)), zone))
    }

    @Test
    fun nextOccurrenceStepsOncePerRule() {
        val due = at(2026, 10, 28, 8, 30)
        assertEquals(at(2026, 10, 29, 8, 30), TaskRules.nextOccurrence(due, Recurrence.DAILY, zone, today))
        assertEquals(at(2026, 11, 4, 8, 30), TaskRules.nextOccurrence(due, Recurrence.WEEKLY, zone, today))
        assertEquals(at(2026, 11, 28, 8, 30), TaskRules.nextOccurrence(due, Recurrence.MONTHLY, zone, today))
        assertEquals(at(2027, 10, 28, 8, 30), TaskRules.nextOccurrence(due, Recurrence.YEARLY, zone, today))
        assertNull(TaskRules.nextOccurrence(due, Recurrence.NONE, zone, today))
    }

    @Test
    fun nextOccurrenceSkipsPastDatesForOverdueTasks() {
        val due = at(2026, 10, 20, 7, 0)
        assertEquals(at(2026, 10, 28, 7, 0), TaskRules.nextOccurrence(due, Recurrence.DAILY, zone, today))
        assertEquals(at(2026, 10, 3 + 7 * 4, 7, 0), TaskRules.nextOccurrence(at(2026, 10, 3, 7, 0), Recurrence.WEEKLY, zone, today))
    }

    @Test
    fun monthlyFromMonthEndClampsToShorterMonth() {
        val jan31 = at(2026, 1, 31, 9, 0)
        assertEquals(at(2026, 2, 28, 9, 0), TaskRules.nextOccurrence(jan31, Recurrence.MONTHLY, zone, LocalDate.of(2026, 1, 31)))
    }

    @Test
    fun progressHandlesEmptyProjects() {
        assertEquals(0f, TaskRules.progress(0, 0), 0f)
        assertEquals(0.5f, TaskRules.progress(2, 4), 0.0001f)
        assertEquals(1f, TaskRules.progress(3, 3), 0f)
    }
}
