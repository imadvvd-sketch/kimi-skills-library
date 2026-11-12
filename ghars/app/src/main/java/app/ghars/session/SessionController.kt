package app.ghars.session

import app.ghars.data.prefs.ActiveSessionStore
import app.ghars.data.prefs.SessionSnapshot
import app.ghars.data.repo.SessionRecorder
import app.ghars.domain.session.Clock
import app.ghars.domain.session.SessionEvent
import app.ghars.domain.session.SessionInfo
import app.ghars.domain.session.SessionState
import app.ghars.domain.session.SessionStateMachine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Starts the foreground service that keeps a session alive. */
fun interface FocusServiceStarter {
    fun start()
}

/** Identifies the current boot, so monotonic times from a previous boot are not trusted. */
fun interface BootCounter {
    fun current(): Int
}

/**
 * Single owner of the live session. Applies events through [SessionStateMachine],
 * persists every change, and files the result when a session ends.
 */
class SessionController(
    private val clock: Clock,
    private val store: ActiveSessionStore,
    private val recorder: SessionRecorder,
    private val serviceStarter: FocusServiceStarter,
    private val bootCounter: BootCounter,
    private val scope: CoroutineScope,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<SessionState?>(null)

    /** Null until the saved session has been read back. */
    val state: StateFlow<SessionState?> = _state.asStateFlow()

    private val restoreJob: Job = scope.launch { restore() }

    private suspend fun restore() = mutex.withLock {
        val snapshot = store.load()
        val saved = snapshot?.state ?: SessionState.Idle
        val rebooted = snapshot != null && snapshot.bootCount != bootCounter.current()
        val event = if (rebooted) SessionEvent.Interrupted else SessionEvent.Tick
        val now = clock.elapsedMs()
        val restored = SessionStateMachine.reduce(saved, event, now)
        commit(saved, restored, now, interrupted = rebooted)
        if (restored is SessionState.Active) serviceStarter.start()
    }

    fun start(speciesId: String, minutes: Int) = dispatch(
        SessionEvent.Start(SessionInfo(speciesId, minutes, clock.elapsedMs(), clock.wallMs())),
    )

    fun pause() = dispatch(SessionEvent.Pause)
    fun resume() = dispatch(SessionEvent.Resume)
    fun giveUp() = dispatch(SessionEvent.GiveUp)
    fun acknowledgeResult() = dispatch(SessionEvent.Reset)

    /**
     * Called regularly while a session is active.
     * @param away the user is in another app with the screen on.
     */
    fun tick(away: Boolean) = scope.launch {
        restoreJob.join()
        mutex.withLock {
            val current = _state.value
            if (current is SessionState.Running) {
                if (away && current.graceDeadline == null) apply(SessionEvent.LeftApp)
                if (!away && current.graceDeadline != null) apply(SessionEvent.Returned)
            }
            apply(SessionEvent.Tick)
        }
    }

    private fun dispatch(event: SessionEvent) = scope.launch {
        restoreJob.join()
        mutex.withLock { apply(event) }
    }

    private suspend fun apply(event: SessionEvent) {
        val old = _state.value ?: return
        val now = clock.elapsedMs()
        val new = SessionStateMachine.reduce(old, event, now)
        if (new != old) {
            commit(old, new, now, interrupted = false)
            if (old is SessionState.Idle && new is SessionState.Active) serviceStarter.start()
        }
    }

    private suspend fun commit(old: SessionState, new: SessionState, now: Long, interrupted: Boolean) {
        if (old is SessionState.Active && new is SessionState.Finished) {
            // After a restart the old monotonic times mean nothing; the best end time we have is now.
            val endedAtWall = if (interrupted) clock.wallMs() else clock.wallMs() - (now - new.endedAt)
            recorder.record(new, endedAtWall)
        }
        if (new != old || interrupted) store.save(SessionSnapshot(new, bootCounter.current()))
        _state.value = new
    }
}
