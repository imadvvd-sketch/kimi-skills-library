package app.ghars.domain.session

import app.ghars.domain.session.SessionState.Active
import app.ghars.domain.session.SessionState.Finished
import app.ghars.domain.session.SessionState.GivenUp
import app.ghars.domain.session.SessionState.Idle
import app.ghars.domain.session.SessionState.Paused
import app.ghars.domain.session.SessionState.Running
import app.ghars.domain.session.SessionState.Success
import app.ghars.domain.session.SessionState.Withered

object SessionRules {
    const val MIN_MINUTES = 10
    const val MAX_MINUTES = 120
    const val STEP_MINUTES = 5
    const val DEFAULT_MINUTES = 25

    /** How long the user may be in another app before the tree withers. */
    const val GRACE_MS = 10_000L

    /** Total pause time allowed per session. */
    const val PAUSE_BUDGET_MS = 5 * 60_000L

    /** Growth stages: seed, sprout, sapling, shrub, tree. */
    const val STAGES = 5

    fun isValidDuration(minutes: Int) =
        minutes in MIN_MINUTES..MAX_MINUTES && minutes % STEP_MINUTES == 0

    fun snapDuration(minutes: Int): Int {
        val snapped = Math.round(minutes.toFloat() / STEP_MINUTES) * STEP_MINUTES
        return snapped.coerceIn(MIN_MINUTES, MAX_MINUTES)
    }
}

/**
 * The whole session lifecycle as a pure function. No Android, no timers:
 * every deadline is an absolute time, so the result is the same whether
 * ticks arrive every second or once after the process was dead for an hour.
 */
object SessionStateMachine {

    fun reduce(state: SessionState, event: SessionEvent, now: Long): SessionState {
        if (event == SessionEvent.Interrupted) {
            // The clock restarted with the device, so "now" cannot be compared with stored times.
            return if (state is Active) Withered(state.session, state.session.startedAt, pausedSoFar(state)) else state
        }
        // Settle what is already due first, so a late event cannot rewrite the past.
        val settled = settle(state, now)
        return when (event) {
            is SessionEvent.Start ->
                if (settled is Idle && SessionRules.isValidDuration(event.session.plannedMinutes)) {
                    Running(event.session, endsAt = event.session.startedAt + event.session.plannedMs)
                } else settled

            SessionEvent.Tick -> settled

            SessionEvent.Pause ->
                if (settled is Running && settled.graceDeadline == null &&
                    settled.pausedMs < SessionRules.PAUSE_BUDGET_MS
                ) {
                    Paused(
                        session = settled.session,
                        remainingMs = settled.endsAt - now,
                        pausedAt = now,
                        resumeDeadline = now + (SessionRules.PAUSE_BUDGET_MS - settled.pausedMs),
                        pausedMs = settled.pausedMs,
                    )
                } else settled

            SessionEvent.Resume -> if (settled is Paused) resume(settled, now) else settled

            SessionEvent.GiveUp ->
                if (settled is Active) GivenUp(settled.session, now, pausedSoFar(settled, now)) else settled

            SessionEvent.LeftApp ->
                if (settled is Running && settled.graceDeadline == null) {
                    settled.copy(graceDeadline = now + SessionRules.GRACE_MS)
                } else settled

            SessionEvent.Returned ->
                if (settled is Running) settled.copy(graceDeadline = null) else settled

            SessionEvent.Reset -> if (settled is Finished) Idle else settled

            SessionEvent.Interrupted -> settled
        }
    }

    private fun settle(state: SessionState, now: Long): SessionState = when (state) {
        is Running -> {
            val grace = state.graceDeadline
            when {
                // Finishing before the grace period runs out still counts.
                grace != null && grace < state.endsAt && now >= grace ->
                    Withered(state.session, grace, state.pausedMs)
                now >= state.endsAt -> Success(state.session, state.endsAt, state.pausedMs)
                else -> state
            }
        }
        is Paused ->
            if (now >= state.resumeDeadline) settle(resume(state, state.resumeDeadline), now) else state
        else -> state
    }

    private fun resume(state: Paused, at: Long) = Running(
        session = state.session,
        endsAt = at + state.remainingMs,
        pausedMs = state.pausedMs + (at - state.pausedAt),
    )

    private fun pausedSoFar(state: Active, now: Long? = null): Long = when (state) {
        is Running -> state.pausedMs
        is Paused -> state.pausedMs + ((now ?: state.pausedAt) - state.pausedAt)
    }

    /** Focus time still to go. Zero once finished. */
    fun remainingMs(state: SessionState, now: Long): Long = when (state) {
        is Running -> (state.endsAt - now).coerceAtLeast(0)
        is Paused -> state.remainingMs
        else -> 0
    }

    /** 0 at planting, 1 when the planned time is done. */
    fun progress(state: SessionState, now: Long): Float = when (state) {
        is Active -> (1f - remainingMs(state, now).toFloat() / state.session.plannedMs).coerceIn(0f, 1f)
        is Success -> 1f
        is Finished -> ((state.endedAt - state.session.startedAt - state.pausedMs).toFloat() /
            state.session.plannedMs).coerceIn(0f, 1f)
        Idle -> 0f
    }

    /** Stage 1 (seed) to [SessionRules.STAGES] (full tree, only at completion). */
    fun stage(progress: Float): Int {
        val steps = SessionRules.STAGES - 1
        return (1 + (progress.coerceIn(0f, 1f) * steps).toInt()).coerceAtMost(SessionRules.STAGES)
    }
}
