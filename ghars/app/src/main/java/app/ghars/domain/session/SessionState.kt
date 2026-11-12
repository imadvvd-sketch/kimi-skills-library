package app.ghars.domain.session

import kotlinx.serialization.Serializable

/** What the user planted. Times ending in "At" are on the monotonic clock unless they say Wall. */
@Serializable
data class SessionInfo(
    val speciesId: String,
    val plannedMinutes: Int,
    val startedAt: Long,
    val startedAtWall: Long,
) {
    val plannedMs: Long get() = plannedMinutes * 60_000L
}

@Serializable
sealed interface SessionState {

    @Serializable
    data object Idle : SessionState

    /** A session that is still going: [Running] or [Paused]. */
    sealed interface Active : SessionState {
        val session: SessionInfo
    }

    /**
     * @param graceDeadline set while the user is away from the app with the screen on;
     *   the tree withers if they are not back by then.
     * @param pausedMs total time spent paused so far.
     */
    @Serializable
    data class Running(
        override val session: SessionInfo,
        val endsAt: Long,
        val graceDeadline: Long? = null,
        val pausedMs: Long = 0,
    ) : Active

    /** @param pausedMs time spent in earlier pauses, not counting this one. */
    @Serializable
    data class Paused(
        override val session: SessionInfo,
        val remainingMs: Long,
        val pausedAt: Long,
        val resumeDeadline: Long,
        val pausedMs: Long = 0,
    ) : Active

    /** A session that is over and waiting for the user to see the result. */
    sealed interface Finished : SessionState {
        val session: SessionInfo
        val endedAt: Long
        val pausedMs: Long
    }

    @Serializable
    data class Success(
        override val session: SessionInfo,
        override val endedAt: Long,
        override val pausedMs: Long = 0,
    ) : Finished

    /** The user left the app and did not come back in time, or the device restarted. */
    @Serializable
    data class Withered(
        override val session: SessionInfo,
        override val endedAt: Long,
        override val pausedMs: Long = 0,
    ) : Finished

    /** The user chose to stop. */
    @Serializable
    data class GivenUp(
        override val session: SessionInfo,
        override val endedAt: Long,
        override val pausedMs: Long = 0,
    ) : Finished
}

sealed interface SessionEvent {
    data class Start(val session: SessionInfo) : SessionEvent
    /** Time passed; settle anything that is due. */
    data object Tick : SessionEvent
    data object Pause : SessionEvent
    data object Resume : SessionEvent
    data object GiveUp : SessionEvent
    data object LeftApp : SessionEvent
    data object Returned : SessionEvent
    /** The session could not be watched to the end (device restart). */
    data object Interrupted : SessionEvent
    /** The user has seen the result. */
    data object Reset : SessionEvent
}
