package app.ghars.domain.economy

import app.ghars.domain.session.SessionRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EconomyTest {

    @Test fun `one coin per minute under an hour`() {
        assertEquals(10, CoinRules.earned(10, success = true))
        assertEquals(25, CoinRules.earned(25, success = true))
        assertEquals(55, CoinRules.earned(55, success = true))
    }

    @Test fun `twenty percent bonus from sixty minutes`() {
        assertEquals(72, CoinRules.earned(60, success = true))
        assertEquals(78, CoinRules.earned(65, success = true))
        assertEquals(144, CoinRules.earned(120, success = true))
    }

    @Test fun `failed sessions earn nothing`() {
        assertEquals(0, CoinRules.earned(120, success = false))
        assertEquals(0, CoinRules.earned(0, success = true))
    }

    @Test fun `every allowed duration gives a whole, growing number of coins`() {
        var previous = 0
        for (m in SessionRules.MIN_MINUTES..SessionRules.MAX_MINUTES step SessionRules.STEP_MINUTES) {
            val coins = CoinRules.earned(m, success = true)
            assertTrue("$m", coins > previous)
            previous = coins
        }
    }

    @Test fun `three free species and seven priced between 150 and 600`() {
        val (free, paid) = SpeciesSeeds.all.partition { it.priceCoins == 0 }
        assertEquals(listOf("olive", "lemon", "cactus"), free.map { it.id })
        assertEquals(7, paid.size)
        assertTrue(paid.all { it.priceCoins in 150..600 })
        assertEquals(150, paid.minOf { it.priceCoins })
        assertEquals(600, paid.maxOf { it.priceCoins })
        assertEquals(SpeciesSeeds.all.size, SpeciesSeeds.all.map { it.id }.toSet().size)
    }

    // --- challenges ---

    private val day = "2026-11-12"

    @Test fun `progress is capped at the goal`() {
        val totals = DayTotals(sessions = 5, minutes = 200)
        assertEquals(1, DailyChallenge.FirstSession.progress(totals))
        assertEquals(60, DailyChallenge.HourOfFocus.progress(totals))
        assertEquals(3, DailyChallenge.ThreeSessions.progress(totals))
    }

    @Test fun `a quiet day leaves everything in progress`() {
        val statuses = ChallengeRules.statuses(DayTotals(0, 0), emptySet(), day)
        assertTrue(statuses.all { it.state == ChallengeState.InProgress && it.progress == 0 })
        assertEquals(DailyChallenge.entries.size, statuses.size)
    }

    @Test fun `one 25 minute session completes only the first challenge`() {
        val byId = ChallengeRules.statuses(DayTotals(1, 25), emptySet(), day).associateBy { it.challenge }
        assertEquals(ChallengeState.Complete, byId.getValue(DailyChallenge.FirstSession).state)
        assertEquals(ChallengeState.InProgress, byId.getValue(DailyChallenge.HourOfFocus).state)
        assertEquals(25, byId.getValue(DailyChallenge.HourOfFocus).progress)
        assertEquals(ChallengeState.InProgress, byId.getValue(DailyChallenge.ThreeSessions).state)
    }

    @Test fun `a claimed challenge stays claimed for that day only`() {
        val claimed = setOf(DailyChallenge.FirstSession.claimRef(day))
        val today = ChallengeRules.statuses(DayTotals(1, 25), claimed, day).first()
        assertEquals(ChallengeState.Claimed, today.state)
        val tomorrow = ChallengeRules.statuses(DayTotals(1, 25), claimed, "2026-11-13").first()
        assertEquals(ChallengeState.Complete, tomorrow.state)
    }

    @Test fun `claim refs are unique per challenge and day`() {
        val refs = DailyChallenge.entries.flatMap { c -> listOf(c.claimRef(day), c.claimRef("2026-11-13")) }
        assertEquals(refs.size, refs.toSet().size)
        assertFalse(DailyChallenge.HourOfFocus.isComplete(DayTotals(3, 59)))
        assertTrue(DailyChallenge.HourOfFocus.isComplete(DayTotals(1, 60)))
    }
}
