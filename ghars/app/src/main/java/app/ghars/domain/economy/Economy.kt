package app.ghars.domain.economy

import kotlin.math.min

/** How coins are earned. Deliberately generous: one coin per focused minute. */
object CoinRules {
    const val LONG_SESSION_MINUTES = 60
    private const val LONG_SESSION_BONUS_PERCENT = 20

    /** Coins for a session. Withered and given-up sessions earn nothing. */
    fun earned(plannedMinutes: Int, success: Boolean): Int {
        if (!success || plannedMinutes <= 0) return 0
        val bonus = if (plannedMinutes >= LONG_SESSION_MINUTES) plannedMinutes * LONG_SESSION_BONUS_PERCENT / 100 else 0
        return plannedMinutes + bonus
    }
}

/** Why the balance changed. Stored as text in coin_txn.reason. */
object CoinReason {
    const val SESSION = "session"
    const val PURCHASE = "purchase"
    const val CHALLENGE = "challenge"
}

/** A species as first installed. Free ones are owned from the start. */
data class SpeciesSeed(val id: String, val priceCoins: Int)

object SpeciesSeeds {
    val all = listOf(
        SpeciesSeed("olive", 0),
        SpeciesSeed("lemon", 0),
        SpeciesSeed("cactus", 0),
        SpeciesSeed("palm", 150),
        SpeciesSeed("jasmine", 200),
        SpeciesSeed("pomegranate", 250),
        SpeciesSeed("fig", 300),
        SpeciesSeed("sidr", 400),
        SpeciesSeed("cedar", 500),
        SpeciesSeed("argan", 600),
    )
}

/** What the user did today, counting successful sessions only. */
data class DayTotals(val sessions: Int, val minutes: Int)

enum class ChallengeMetric { Sessions, Minutes }

/** Daily challenges. They reset at local midnight. */
enum class DailyChallenge(val id: String, val metric: ChallengeMetric, val goal: Int, val reward: Int) {
    FirstSession("first_session", ChallengeMetric.Sessions, 1, 10),
    HourOfFocus("focus_60", ChallengeMetric.Minutes, 60, 30),
    ThreeSessions("three_sessions", ChallengeMetric.Sessions, 3, 25);

    fun progress(totals: DayTotals): Int = min(
        goal,
        when (metric) {
            ChallengeMetric.Sessions -> totals.sessions
            ChallengeMetric.Minutes -> totals.minutes
        },
    )

    fun isComplete(totals: DayTotals): Boolean = progress(totals) >= goal

    /** Identifies one day's claim, so a reward cannot be collected twice. [day] is an ISO date. */
    fun claimRef(day: String): String = "$id:$day"
}

enum class ChallengeState { InProgress, Complete, Claimed }

data class ChallengeStatus(val challenge: DailyChallenge, val progress: Int, val state: ChallengeState)

object ChallengeRules {
    fun statuses(totals: DayTotals, claimedRefs: Set<String>, day: String): List<ChallengeStatus> =
        DailyChallenge.entries.map { challenge ->
            ChallengeStatus(
                challenge = challenge,
                progress = challenge.progress(totals),
                state = when {
                    challenge.claimRef(day) in claimedRefs -> ChallengeState.Claimed
                    challenge.isComplete(totals) -> ChallengeState.Complete
                    else -> ChallengeState.InProgress
                },
            )
        }
}
