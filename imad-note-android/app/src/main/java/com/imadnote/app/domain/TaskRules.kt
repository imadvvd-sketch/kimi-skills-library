package com.imadnote.app.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class Recurrence { NONE, DAILY, WEEKLY, MONTHLY, YEARLY }

/** Priority levels stored as integers so they sort naturally (higher = more urgent). */
object Priority {
    const val NONE = 0
    const val LOW = 1
    const val MEDIUM = 2
    const val HIGH = 3
}

/** Minimal view of a task needed by the scheduling rules, so they stay testable without Room. */
data class TaskTiming(
    val dueAt: Long?,
    val hasTime: Boolean,
    val completedAt: Long?,
    val reminderOffsetMin: Int?,
)

object TaskRules {
    /** Reminders for all-day tasks fire at this local time on the due date. */
    val ALL_DAY_REMINDER_TIME: LocalTime = LocalTime.of(9, 0)

    fun dueDate(dueAt: Long?, zone: ZoneId): LocalDate? =
        dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }

    fun isOverdue(t: TaskTiming, today: LocalDate, zone: ZoneId): Boolean {
        if (t.completedAt != null) return false
        val date = dueDate(t.dueAt, zone) ?: return false
        return date.isBefore(today)
    }

    fun isDueToday(t: TaskTiming, today: LocalDate, zone: ZoneId): Boolean =
        t.completedAt == null && dueDate(t.dueAt, zone) == today

    fun isUpcoming(t: TaskTiming, today: LocalDate, zone: ZoneId, withinDays: Long? = null): Boolean {
        if (t.completedAt != null) return false
        val date = dueDate(t.dueAt, zone) ?: return false
        if (!date.isAfter(today)) return false
        return withinDays == null || !date.isAfter(today.plusDays(withinDays))
    }

    /** Absolute reminder time, or null when the task has no due date or no reminder. */
    fun reminderTime(t: TaskTiming, zone: ZoneId): Long? {
        val dueAt = t.dueAt ?: return null
        val offset = t.reminderOffsetMin ?: return null
        val base = if (t.hasTime) {
            dueAt
        } else {
            Instant.ofEpochMilli(dueAt).atZone(zone).toLocalDate()
                .atTime(ALL_DAY_REMINDER_TIME).atZone(zone).toInstant().toEpochMilli()
        }
        return base - offset * 60_000L
    }

    /**
     * Next due time for a recurring task. Steps forward at least once and keeps stepping
     * until the date is no earlier than [today], so completing an old overdue task does
     * not create another overdue copy.
     */
    fun nextOccurrence(dueAt: Long, rule: Recurrence, zone: ZoneId, today: LocalDate): Long? {
        if (rule == Recurrence.NONE) return null
        var next: ZonedDateTime = Instant.ofEpochMilli(dueAt).atZone(zone)
        var guard = 0
        do {
            next = step(next, rule)
            guard++
        } while (next.toLocalDate().isBefore(today) && guard < 100_000)
        return next.toInstant().toEpochMilli()
    }

    private fun step(dt: ZonedDateTime, rule: Recurrence): ZonedDateTime = when (rule) {
        Recurrence.NONE -> dt
        Recurrence.DAILY -> dt.plusDays(1)
        Recurrence.WEEKLY -> dt.plusWeeks(1)
        Recurrence.MONTHLY -> dt.plusMonths(1)
        Recurrence.YEARLY -> dt.plusYears(1)
    }

    fun progress(done: Int, total: Int): Float = if (total <= 0) 0f else done.toFloat() / total

    /** First day of the week used by the calendar view (Saturday is common in Arabic locales). */
    fun firstDayOfWeek(locale: java.util.Locale): DayOfWeek =
        java.time.temporal.WeekFields.of(locale).firstDayOfWeek
}
