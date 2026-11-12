package app.ghars.data.repo

import app.ghars.data.db.SessionDao
import app.ghars.data.db.SessionEntity
import app.ghars.data.db.SpeciesDao
import app.ghars.data.db.SpeciesEntity
import app.ghars.domain.session.SessionState
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/** Writes finished sessions to the permanent record. */
interface SessionRecorder {
    suspend fun record(finished: SessionState.Finished, endedAtWall: Long)
}

class SessionRepository @Inject constructor(private val dao: SessionDao) : SessionRecorder {
    override suspend fun record(finished: SessionState.Finished, endedAtWall: Long) {
        dao.insert(
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
            ),
        )
    }
}

class SpeciesRepository @Inject constructor(private val dao: SpeciesDao) {
    fun observeOwned(): Flow<List<SpeciesEntity>> = dao.observeOwned()
}
