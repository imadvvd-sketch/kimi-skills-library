package app.imadnote.domain.tasks

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Repeat rule stored on a task as `UNIT:interval`, e.g. `D:1` daily, `W:2` every two weeks,
 * `M:1` monthly, `Y:1` yearly, `WD:1` every weekday (Monday–Friday).
 */
data class Recurrence(val unit: Unit, val interval: Int = 1) {
    enum class Unit { DAY, WEEK, MONTH, YEAR, WEEKDAY }

    fun encode(): String = "${unit.code}:$interval"

    /** The next occurrence strictly after [from]. Monthly rules clamp to the end of short months. */
    fun next(from: LocalDate): LocalDate = when (unit) {
        Unit.DAY -> from.plusDays(interval.toLong())
        Unit.WEEK -> from.plusWeeks(interval.toLong())
        Unit.MONTH -> from.plusMonths(interval.toLong())
        Unit.YEAR -> from.plusYears(interval.toLong())
        Unit.WEEKDAY -> {
            var day = from.plusDays(1)
            while (day.dayOfWeek == DayOfWeek.SATURDAY || day.dayOfWeek == DayOfWeek.SUNDAY) day = day.plusDays(1)
            day
        }
    }

    companion object {
        private val Unit.code get() = when (this) {
            Unit.DAY -> "D"; Unit.WEEK -> "W"; Unit.MONTH -> "M"; Unit.YEAR -> "Y"; Unit.WEEKDAY -> "WD"
        }

        fun parse(raw: String?): Recurrence? {
            if (raw.isNullOrBlank()) return null
            val (code, n) = raw.split(':').let { it[0] to (it.getOrNull(1)?.toIntOrNull() ?: 1) }
            val unit = Unit.entries.firstOrNull { it.code == code } ?: return null
            return Recurrence(unit, n.coerceIn(1, 365))
        }
    }
}

enum class Priority(val level: Int) {
    NONE(0), LOW(1), MEDIUM(2), HIGH(3);

    companion object {
        fun of(level: Int) = entries.firstOrNull { it.level == level } ?: NONE
    }
}

/**
 * Quick-add shortcuts in a task title, in English and Arabic:
 * `today`/`اليوم`, `tomorrow`/`غدا`/`غدًا`, `!1`–`!3` (or `!low`, `!med`, `!high`) for priority,
 * and `#label` to attach a label. Recognised tokens are removed from the title.
 */
object TaskQuickParser {
    data class Result(val title: String, val dueDate: LocalDate?, val priority: Priority, val labels: List<String>)

    private val today = setOf("today", "اليوم")
    private val tomorrow = setOf("tomorrow", "tmr", "غدا", "غدًا", "غداً")
    private val priorities = mapOf(
        "!1" to Priority.LOW, "!low" to Priority.LOW,
        "!2" to Priority.MEDIUM, "!med" to Priority.MEDIUM, "!medium" to Priority.MEDIUM,
        "!3" to Priority.HIGH, "!high" to Priority.HIGH,
    )

    fun parse(input: String, now: LocalDate): Result {
        var due: LocalDate? = null
        var priority = Priority.NONE
        val labels = ArrayList<String>()
        val kept = ArrayList<String>()
        for (word in input.trim().split(Regex("\\s+"))) {
            val lower = word.lowercase()
            when {
                lower in today -> due = now
                lower in tomorrow -> due = now.plusDays(1)
                lower in priorities -> priority = priorities.getValue(lower)
                word.length > 1 && word.startsWith("#") -> labels += word.drop(1)
                else -> kept += word
            }
        }
        val title = kept.joinToString(" ").trim()
        // A title made only of shortcuts keeps the original text so nothing the user typed is lost.
        return if (title.isEmpty()) Result(input.trim(), null, Priority.NONE, emptyList()) else Result(title, due, priority, labels)
    }
}
