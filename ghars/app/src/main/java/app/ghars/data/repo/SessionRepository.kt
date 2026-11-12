package app.ghars.data.repo

import androidx.room.withTransaction
import app.ghars.data.db.CoinTxnEntity
import app.ghars.data.db.GharsDatabase
import app.ghars.data.db.SessionEntity
import app.ghars.data.db.SpeciesWithOwnership
import app.ghars.data.db.UserSpeciesEntity
import app.ghars.domain.economy.ChallengeRules
import app.ghars.domain.economy.ChallengeState
import app.ghars.domain.economy.ChallengeStatus
import app.ghars.domain.economy.CoinReason
import app.ghars.domain.economy.CoinRules
import app.ghars.domain.economy.DailyChallenge
import app.ghars.domain.economy.DayTotals
import app.ghars.domain.session.Clock
import app.ghars.domain.session.SessionState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Writes finished sessions to the permanent record. */
interface SessionRecorder {
    suspend fun record(finished: SessionState.Finished, endedAtWall: Long)
}

class SessionRepository @Inject constructor(private val db: GharsDatabase) : SessionRecorder {
    override suspend fun record(finished: SessionState.Finished, endedAtWall: Long) {
        val coins = CoinRules.earned(finished.session.plannedMinutes, finished is SessionState.Success)
        // The tree and its coins are written together or not at all.
        db.withTransaction {
            val sessionId = db.sessionDao().insert(
                SessionEntity(
                    speciesId = finished.session.speciesId,
                    plannedMinutes = finished.session.plannedMinutes,
                    startedAt = finished.session.startedAtWall,
                    endedAt = endedAtWall,
                    status = when (finished) {
                        is SessionState.Success -> "success"
                        is SessionState.Withered -> "withered"
                        is SessionState.GivenUp -> "given_up"
                    },
                    pausedSeconds = (finished.pausedMs / 1000).toInt(),
                    coinsEarned = coins,
                ),
            )
            if (coins > 0) {
                db.coinDao().insert(CoinTxnEntity(amount = coins, reason = CoinReason.SESSION, refId = sessionId.toString(), at = endedAtWall))
            }
        }
    }
}

class SpeciesRepository @Inject constructor(private val db: GharsDatabase) {
    fun observeAll(): Flow<List<SpeciesWithOwnership>> = db.speciesDao().observeAll()
}

enum class PurchaseResult { Bought, AlreadyOwned, NotEnoughCoins, UnknownSpecies }

/** Coins: the balance, buying species, and daily challenges. */
class EconomyRepository @Inject constructor(
    private val db: GharsDatabase,
    private val clock: Clock,
) {
    val balance: Flow<Int> = db.coinDao().observeBalance()

    suspend fun purchase(speciesId: String): PurchaseResult = db.withTransaction {
        val species = db.speciesDao().find(speciesId) ?: return@withTransaction PurchaseResult.UnknownSpecies
        if (db.speciesDao().ownedCount(speciesId) > 0) return@withTransaction PurchaseResult.AlreadyOwned
        if (db.coinDao().balance() < species.priceCoins) return@withTransaction PurchaseResult.NotEnoughCoins
        val now = clock.wallMs()
        db.coinDao().insert(CoinTxnEntity(amount = -species.priceCoins, reason = CoinReason.PURCHASE, refId = speciesId, at = now))
        db.speciesDao().addOwned(UserSpeciesEntity(speciesId, now))
        PurchaseResult.Bought
    }

    private class Today(val iso: String, val from: Long, val to: Long)

    private fun today(): Today {
        val zone = ZoneId.systemDefault()
        val date: LocalDate = Instant.ofEpochMilli(clock.wallMs()).atZone(zone).toLocalDate()
        return Today(
            iso = date.toString(),
            from = date.atStartOfDay(zone).toInstant().toEpochMilli(),
            to = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        )
    }

    /** Today's challenges. "Today" is fixed when collection starts; reopen the screen after midnight. */
    fun observeChallenges(): Flow<List<ChallengeStatus>> {
        val today = today()
        return combine(
            db.sessionDao().observeTotals(today.from, today.to),
            db.coinDao().observeRefs(CoinReason.CHALLENGE, "%:${today.iso}"),
        ) { totals, refs ->
            ChallengeRules.statuses(DayTotals(totals.sessions, totals.minutes), refs.toSet(), today.iso)
        }
    }

    /** Pays the reward if the challenge is complete and not yet claimed today. */
    suspend fun claim(challenge: DailyChallenge): Boolean = db.withTransaction {
        val today = today()
        val row = db.sessionDao().totals(today.from, today.to)
        if (!challenge.isComplete(DayTotals(row.sessions, row.minutes))) return@withTransaction false
        db.coinDao().insert(
            CoinTxnEntity(amount = challenge.reward, reason = CoinReason.CHALLENGE, refId = challenge.claimRef(today.iso), at = clock.wallMs()),
        ) != -1L
    }
}
