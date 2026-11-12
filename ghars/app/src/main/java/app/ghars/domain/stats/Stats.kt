package app.ghars.domain.stats

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/** One finished session, as the forest, timeline and statistics need it. */
data class SessionRecord(
    val id: Long,
    val speciesId: String,
    val tagId: Long?,
    val plannedMinutes: Int,
    val startedAt: Long,
    val endedAt: Long,
    val success: Boolean,
)

enum class PeriodKind { Day, Week, Month, Year }

/** A calendar span, [start] inclusive to [endExclusive]. */
data class Period(val kind: PeriodKind, val start: LocalDate, val endExclusive: LocalDate) {
    fun startMs(zone: ZoneId): Long = start.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endMs(zone: ZoneId): Long = endExclusive.atStartOfDay(zone).toInstant().toEpochMilli()

    /** Last day inside the period. */
    val lastDay: LocalDate get() = endExclusive.minusDays(1)
}

object Periods {
    /**
     * The period of [kind] containing [today], moved back by [stepsBack] whole periods.
     * @param firstDayOfWeek Saturday in much of the Arab world, Monday or Sunday elsewhere.
     */
    fun of(kind: PeriodKind, today: LocalDate, stepsBack: Int, firstDayOfWeek: DayOfWeek): Period {
        val back = stepsBack.coerceAtLeast(0).toLong()
        return when (kind) {
            PeriodKind.Day -> today.minusDays(back).let { Period(kind, it, it.plusDays(1)) }
            PeriodKind.Week -> today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).minusWeeks(back)
                .let { Period(kind, it, it.plusWeeks(1)) }
            PeriodKind.Month -> today.withDayOfMonth(1).minusMonths(back).let { Period(kind, it, it.plusMonths(1)) }
            PeriodKind.Year -> today.withDayOfYear(1).minusYears(back).let { Period(kind, it, it.plusYears(1)) }
        }
    }
}

/** Focus minutes for one tag. A null [tagId] gathers sessions without a tag. */
data class TagTotal(val tagId: Long?, val minutes: Int)

data class StatsSummary(
    /** Minutes of completed focus. Withered and given-up sessions add nothing. */
    val focusMinutes: Int,
    val grown: Int,
    val withered: Int,
    /** Largest first. */
    val byTag: List<TagTotal>,
    /** 24 values, index = hour the session started. */
    val byHour: List<Int>,
    /** 7 values, starting from the first day of the week. */
    val byWeekday: List<Int>,
    val weekdays: List<DayOfWeek>,
) {
    val isEmpty: Boolean get() = grown == 0 && withered == 0

    /** Hour with the most focus, or null when there is none. Earliest wins a tie. */
    val bestHour: Int? get() = byHour.indices.maxByOrNull { byHour[it] }?.takeIf { byHour[it] > 0 }
    val bestWeekday: DayOfWeek? get() = byWeekday.indices.maxByOrNull { byWeekday[it] }?.takeIf { byWeekday[it] > 0 }?.let { weekdays[it] }
}

data class Streak(val current: Int, val best: Int)

object StatsCalculator {

    fun summarize(records: List<SessionRecord>, zone: ZoneId, firstDayOfWeek: DayOfWeek): StatsSummary {
        val weekdays = (0 until 7).map { firstDayOfWeek.plus(it.toLong()) }
        val byHour = IntArray(24)
        val byWeekday = IntArray(7)
        val byTag = LinkedHashMap<Long?, Int>()
        var minutes = 0
        var grown = 0
        for (record in records) {
            if (!record.success) continue
            grown++
            minutes += record.plannedMinutes
            val start = Instant.ofEpochMilli(record.startedAt).atZone(zone)
            byHour[start.hour] += record.plannedMinutes
            byWeekday[weekdays.indexOf(start.dayOfWeek)] += record.plannedMinutes
            byTag[record.tagId] = (byTag[record.tagId] ?: 0) + record.plannedMinutes
        }
        return StatsSummary(
            focusMinutes = minutes,
            grown = grown,
            withered = records.size - grown,
            byTag = byTag.map { TagTotal(it.key, it.value) }.sortedByDescending { it.minutes },
            byHour = byHour.toList(),
            byWeekday = byWeekday.toList(),
            weekdays = weekdays,
        )
    }

    /**
     * Runs of consecutive days with at least one completed session.
     * The current streak is still alive if the last such day was today or yesterday:
     * today is not lost until it is over.
     */
    fun streak(successStartTimes: List<Long>, zone: ZoneId, today: LocalDate): Streak {
        val days = successStartTimes.map { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }.toSortedSet()
        if (days.isEmpty()) return Streak(0, 0)
        var best = 0
        var run = 0
        var previous: LocalDate? = null
        for (day in days) {
            run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        val last = days.last()
        val current = if (last == today || last == today.minusDays(1)) run else 0
        return Streak(current, best)
    }

    /** "1h 25m" needs two numbers; the screen formats them. */
    fun hoursAndMinutes(totalMinutes: Int): Pair<Int, Int> = totalMinutes / 60 to totalMinutes % 60
}
