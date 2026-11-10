package app.imadnote.domain.analytics

import app.imadnote.data.db.ProjectEntity
import app.imadnote.data.db.TaskEntity
import app.imadnote.data.db.TaskStatus
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class DayCount(val day: LocalDate, val done: Int, val created: Int)
data class WeekCount(val weekStart: LocalDate, val done: Int)
data class MonthCount(val month: YearMonth, val done: Int, val created: Int)
data class ProjectProgress(val project: ProjectEntity, val done: Int, val total: Int) {
    val fraction get() = if (total == 0) 0f else done.toFloat() / total
}

data class AnalyticsReport(
    val completed: Int,
    val open: Int,
    val overdue: Int,
    /** Completed ÷ all tasks; 0 when there are none. */
    val completionRate: Float,
    val last7: List<DayCount>,
    val weeks: List<WeekCount>,
    val months: List<MonthCount>,
    /** Open tasks by priority level 3..0. */
    val openByPriority: Map<Int, Int>,
    val projects: List<ProjectProgress>,
    /** Consecutive days, ending today or yesterday, with at least one completion. */
    val streak: Int,
    val doneThisWeek: Int,
    val doneLastWeek: Int,
) {
    val isEmpty get() = completed == 0 && open == 0
}

/** Productivity statistics, computed only from stored tasks and projects. */
object Analytics {
    fun compute(tasks: List<TaskEntity>, projects: List<ProjectEntity>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault(), weekStart: DayOfWeek = DayOfWeek.MONDAY): AnalyticsReport {
        fun day(millis: Long) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
        val doneDays = tasks.mapNotNull { t -> t.completedAt?.takeIf { t.status == TaskStatus.DONE }?.let(::day) }
        val createdDays = tasks.map { day(it.createdAt) }
        val doneByDay = doneDays.groupingBy { it }.eachCount()
        val createdByDay = createdDays.groupingBy { it }.eachCount()
        val open = tasks.filter { it.status != TaskStatus.DONE }

        val last7 = (6 downTo 0).map { today.minusDays(it.toLong()) }.map { DayCount(it, doneByDay[it] ?: 0, createdByDay[it] ?: 0) }
        val thisWeek = today.with(TemporalAdjusters.previousOrSame(weekStart))
        val weeks = (11 downTo 0).map { thisWeek.minusWeeks(it.toLong()) }.map { start ->
            WeekCount(start, doneDays.count { it >= start && it < start.plusWeeks(1) })
        }
        val thisMonth = YearMonth.from(today)
        val months = (5 downTo 0).map { thisMonth.minusMonths(it.toLong()) }.map { m ->
            MonthCount(m, doneDays.count { YearMonth.from(it) == m }, createdDays.count { YearMonth.from(it) == m })
        }
        var streak = 0
        var cursor = if ((doneByDay[today] ?: 0) > 0) today else today.minusDays(1)
        while ((doneByDay[cursor] ?: 0) > 0) { streak++; cursor = cursor.minusDays(1) }

        val topLevel = tasks.filter { it.parentId == null }
        return AnalyticsReport(
            completed = doneDays.size,
            open = open.size,
            overdue = open.count { it.dueDate != null && it.dueDate < today.toEpochDay() },
            completionRate = if (tasks.isEmpty()) 0f else doneDays.size.toFloat() / tasks.size,
            last7 = last7,
            weeks = weeks,
            months = months,
            openByPriority = (3 downTo 0).associateWith { p -> open.count { it.priority == p } },
            projects = projects.map { p ->
                val own = topLevel.filter { it.projectId == p.id }
                ProjectProgress(p, own.count { it.status == TaskStatus.DONE }, own.size)
            }.filter { it.total > 0 }.sortedByDescending { it.fraction },
            streak = streak,
            doneThisWeek = weeks.last().done,
            doneLastWeek = weeks[weeks.size - 2].done,
        )
    }
}
