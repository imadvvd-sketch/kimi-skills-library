package app.ghars.domain.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsTest {
    private val zone = ZoneId.of("Asia/Riyadh")
    private val sat = DayOfWeek.SATURDAY

    /** 2026-11-12 is a Thursday. */
    private val today = LocalDate.of(2026, 11, 12)

    private fun at(date: String, time: String = "09:00") =
        LocalDateTime.parse("${date}T$time").atZone(zone).toInstant().toEpochMilli()

    private var nextId = 1L
    private fun session(date: String, time: String = "09:00", minutes: Int = 25, tag: Long? = null, success: Boolean = true) =
        SessionRecord(nextId++, "olive", tag, minutes, at(date, time), at(date, time) + minutes * 60_000L, success)

    // --- periods ---

    @Test fun `day period`() {
        assertEquals(Period(PeriodKind.Day, today, today.plusDays(1)), Periods.of(PeriodKind.Day, today, 0, sat))
        assertEquals(LocalDate.of(2026, 11, 10), Periods.of(PeriodKind.Day, today, 2, sat).start)
    }

    @Test fun `week starts on the given first day`() {
        val saturdayWeek = Periods.of(PeriodKind.Week, today, 0, sat)
        assertEquals(LocalDate.of(2026, 11, 7), saturdayWeek.start)
        assertEquals(LocalDate.of(2026, 11, 14), saturdayWeek.endExclusive)
        assertEquals(LocalDate.of(2026, 11, 9), Periods.of(PeriodKind.Week, today, 0, DayOfWeek.MONDAY).start)
        assertEquals(LocalDate.of(2026, 10, 31), Periods.of(PeriodKind.Week, today, 1, sat).start)
    }

    @Test fun `a week that starts today begins today`() {
        assertEquals(today, Periods.of(PeriodKind.Week, today, 0, DayOfWeek.THURSDAY).start)
    }

    @Test fun `month and year periods, including across a year boundary`() {
        val month = Periods.of(PeriodKind.Month, today, 0, sat)
        assertEquals(LocalDate.of(2026, 11, 1), month.start)
        assertEquals(LocalDate.of(2026, 11, 30), month.lastDay)
        assertEquals(LocalDate.of(2025, 12, 1), Periods.of(PeriodKind.Month, today, 11, sat).start)
        val year = Periods.of(PeriodKind.Year, today, 0, sat)
        assertEquals(LocalDate.of(2026, 1, 1), year.start)
        assertEquals(LocalDate.of(2027, 1, 1), year.endExclusive)
        assertEquals(LocalDate.of(2024, 2, 29), Periods.of(PeriodKind.Month, today, 33, sat).lastDay) // leap year
    }

    @Test fun `period bounds in milliseconds follow the local midnight`() {
        val day = Periods.of(PeriodKind.Day, today, 0, sat)
        assertEquals(at("2026-11-12", "00:00"), day.startMs(zone))
        assertEquals(at("2026-11-13", "00:00"), day.endMs(zone))
        assertEquals(today, Periods.of(PeriodKind.Day, today, -5, sat).start) // never into the future
    }

    // --- summary ---

    @Test fun `empty summary`() {
        val s = StatsCalculator.summarize(emptyList(), zone, sat)
        assertTrue(s.isEmpty)
        assertEquals(0, s.focusMinutes)
        assertNull(s.bestHour)
        assertNull(s.bestWeekday)
        assertEquals(24, s.byHour.size)
        assertEquals(7, s.byWeekday.size)
        assertEquals(sat, s.weekdays.first())
        assertEquals(DayOfWeek.FRIDAY, s.weekdays.last())
    }

    @Test fun `only completed sessions count as focus time`() {
        val s = StatsCalculator.summarize(
            listOf(session("2026-11-12", minutes = 25), session("2026-11-12", minutes = 60, success = false)), zone, sat,
        )
        assertEquals(25, s.focusMinutes)
        assertEquals(1, s.grown)
        assertEquals(1, s.withered)
        assertFalse(s.isEmpty)
    }

    @Test fun `a period with only withered trees is not empty`() {
        val s = StatsCalculator.summarize(listOf(session("2026-11-12", success = false)), zone, sat)
        assertFalse(s.isEmpty)
        assertEquals(0, s.focusMinutes)
        assertTrue(s.byTag.isEmpty())
    }

    @Test fun `minutes by tag, largest first, untagged kept apart`() {
        val s = StatsCalculator.summarize(
            listOf(
                session("2026-11-10", minutes = 25, tag = 1),
                session("2026-11-11", minutes = 50, tag = 2),
                session("2026-11-12", minutes = 30, tag = 1),
                session("2026-11-12", minutes = 10),
                session("2026-11-12", minutes = 120, tag = 3, success = false),
            ), zone, sat,
        )
        assertEquals(listOf(TagTotal(1, 55), TagTotal(2, 50), TagTotal(null, 10)), s.byTag)
    }

    @Test fun `minutes by hour and by weekday use the start time in the local zone`() {
        val s = StatsCalculator.summarize(
            listOf(
                session("2026-11-12", "09:10", 25),  // Thursday
                session("2026-11-12", "09:50", 30),
                session("2026-11-07", "23:30", 60),  // Saturday, runs past midnight
                session("2026-11-13", "06:00", 10),  // Friday
            ), zone, sat,
        )
        assertEquals(55, s.byHour[9])
        assertEquals(60, s.byHour[23])
        assertEquals(0, s.byHour[0])
        assertEquals(23, s.bestHour)
        assertEquals(listOf(60, 0, 0, 0, 0, 55, 10), s.byWeekday) // Sat..Fri
        assertEquals(DayOfWeek.SATURDAY, s.bestWeekday)
    }

    // --- streak ---

    @Test fun `no sessions, no streak`() {
        assertEquals(Streak(0, 0), StatsCalculator.streak(emptyList(), zone, today))
    }

    @Test fun `consecutive days ending today`() {
        val times = listOf(at("2026-11-10"), at("2026-11-11"), at("2026-11-12"))
        assertEquals(Streak(3, 3), StatsCalculator.streak(times, zone, today))
    }

    @Test fun `several sessions in one day count once`() {
        val times = listOf(at("2026-11-12", "08:00"), at("2026-11-12", "14:00"), at("2026-11-12", "20:00"))
        assertEquals(Streak(1, 1), StatsCalculator.streak(times, zone, today))
    }

    @Test fun `the streak survives until today is over`() {
        val times = listOf(at("2026-11-10"), at("2026-11-11"))
        assertEquals(Streak(2, 2), StatsCalculator.streak(times, zone, today))
    }

    @Test fun `a missed day ends the current streak but keeps the best`() {
        val times = listOf(at("2026-11-05"), at("2026-11-06"), at("2026-11-07"), at("2026-11-08"), at("2026-11-10"))
        assertEquals(Streak(0, 4), StatsCalculator.streak(times, zone, today))
    }

    @Test fun `best streak can be older than the current one`() {
        val times = listOf(at("2026-10-01"), at("2026-10-02"), at("2026-10-03"), at("2026-11-11"), at("2026-11-12"))
        assertEquals(Streak(2, 3), StatsCalculator.streak(times, zone, today))
    }

    @Test fun `days are local, so a late night session belongs to that night`() {
        // 23:30 in Riyadh is the same UTC day, 00:30 is still the previous UTC day: both must be local days.
        val times = listOf(at("2026-11-11", "23:30"), at("2026-11-12", "00:30"))
        assertEquals(Streak(2, 2), StatsCalculator.streak(times, zone, today))
    }

    @Test fun `unordered input is fine`() {
        val times = listOf(at("2026-11-12"), at("2026-11-10"), at("2026-11-11"))
        assertEquals(Streak(3, 3), StatsCalculator.streak(times, zone, today))
    }

    @Test fun `hours and minutes`() {
        assertEquals(0 to 0, StatsCalculator.hoursAndMinutes(0))
        assertEquals(0 to 45, StatsCalculator.hoursAndMinutes(45))
        assertEquals(1 to 25, StatsCalculator.hoursAndMinutes(85))
        assertEquals(2 to 0, StatsCalculator.hoursAndMinutes(120))
    }
}
