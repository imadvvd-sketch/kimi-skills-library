package app.ghars.domain.session

import app.ghars.domain.session.SessionEvent.GiveUp
import app.ghars.domain.session.SessionEvent.Interrupted
import app.ghars.domain.session.SessionEvent.LeftApp
import app.ghars.domain.session.SessionEvent.Pause
import app.ghars.domain.session.SessionEvent.Reset
import app.ghars.domain.session.SessionEvent.Resume
import app.ghars.domain.session.SessionEvent.Returned
import app.ghars.domain.session.SessionEvent.Start
import app.ghars.domain.session.SessionEvent.Tick
import app.ghars.domain.session.SessionState.GivenUp
import app.ghars.domain.session.SessionState.Idle
import app.ghars.domain.session.SessionState.Paused
import app.ghars.domain.session.SessionState.Running
import app.ghars.domain.session.SessionState.Success
import app.ghars.domain.session.SessionState.Withered
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionStateMachineTest {

    private val t0 = 1_000_000L
    private val min = 60_000L
    private val sec = 1_000L
    private val info = SessionInfo("olive", plannedMinutes = 25, startedAt = t0, startedAtWall = 42L)
    private val end = t0 + 25 * min

    private fun reduce(s: SessionState, e: SessionEvent, at: Long) = SessionStateMachine.reduce(s, e, at)
    private fun running() = reduce(Idle, Start(info), t0) as Running

    /** Feeds events in order; each pair is (event, time). */
    private fun play(vararg steps: Pair<SessionEvent, Long>): SessionState =
        steps.fold(Idle as SessionState) { s, (e, at) -> reduce(s, e, at) }

    // --- start ---

    @Test fun `start from idle runs until the planned end`() {
        assertEquals(Running(info, endsAt = end), running())
    }

    @Test fun `start is ignored while a session is active or finished`() {
        val other = Start(info.copy(speciesId = "lemon"))
        assertEquals(running(), reduce(running(), other, t0 + min))
        val done = reduce(running(), Tick, end)
        assertEquals(done, reduce(done, other, end + min))
    }

    @Test fun `start rejects durations outside 10 to 120 in steps of 5`() {
        for (bad in listOf(0, 5, 9, 12, 121, 125, -10)) {
            assertEquals("minutes=$bad", Idle, reduce(Idle, Start(info.copy(plannedMinutes = bad)), t0))
        }
        for (good in listOf(10, 15, 60, 120)) {
            assertTrue("minutes=$good", reduce(Idle, Start(info.copy(plannedMinutes = good)), t0) is Running)
        }
    }

    // --- success ---

    @Test fun `tick before the end changes nothing`() {
        assertEquals(running(), reduce(running(), Tick, end - 1))
    }

    @Test fun `tick at the end succeeds`() {
        assertEquals(Success(info, endedAt = end), reduce(running(), Tick, end))
    }

    @Test fun `a late tick still ends the session at the planned time`() {
        // The process was dead for an hour; the session ended when it was due, not when we noticed.
        assertEquals(Success(info, endedAt = end), reduce(running(), Tick, end + 60 * min))
    }

    // --- leaving the app ---

    @Test fun `leaving starts a ten second grace period`() {
        val s = reduce(running(), LeftApp, t0 + min) as Running
        assertEquals(t0 + min + 10 * sec, s.graceDeadline)
        assertEquals(end, s.endsAt)
    }

    @Test fun `leaving again does not extend the grace period`() {
        val first = reduce(running(), LeftApp, t0 + min)
        assertEquals(first, reduce(first, LeftApp, t0 + min + 5 * sec))
    }

    @Test fun `returning within the grace period carries on`() {
        val s = play(Start(info) to t0, LeftApp to t0 + min, Returned to t0 + min + 9_999)
        assertEquals(running(), s)
    }

    @Test fun `not returning withers the tree at the deadline`() {
        val s = play(Start(info) to t0, LeftApp to t0 + min, Tick to t0 + min + 10 * sec)
        assertEquals(Withered(info, endedAt = t0 + min + 10 * sec), s)
    }

    @Test fun `returning too late does not save the tree`() {
        val s = play(Start(info) to t0, LeftApp to t0 + min, Returned to t0 + min + 10 * sec)
        assertEquals(Withered(info, endedAt = t0 + min + 10 * sec), s)
    }

    @Test fun `each departure gets a fresh grace period`() {
        val s = play(
            Start(info) to t0,
            LeftApp to t0 + min, Returned to t0 + min + 8 * sec,
            LeftApp to t0 + 2 * min, Returned to t0 + 2 * min + 8 * sec,
        )
        assertEquals(running(), s)
    }

    @Test fun `finishing during the grace period is a success`() {
        val s = play(Start(info) to t0, LeftApp to end - 3 * sec, Tick to end + min)
        assertEquals(Success(info, endedAt = end), s)
    }

    @Test fun `returned without having left is harmless`() {
        assertEquals(running(), reduce(running(), Returned, t0 + min))
    }

    // --- giving up ---

    @Test fun `giving up while running`() {
        assertEquals(GivenUp(info, endedAt = t0 + min), reduce(running(), GiveUp, t0 + min))
    }

    @Test fun `giving up while paused counts the pause so far`() {
        val s = play(Start(info) to t0, Pause to t0 + min, GiveUp to t0 + 3 * min)
        assertEquals(GivenUp(info, endedAt = t0 + 3 * min, pausedMs = 2 * min), s)
    }

    @Test fun `giving up after the end is still a success`() {
        assertEquals(Success(info, endedAt = end), reduce(running(), GiveUp, end + sec))
    }

    @Test fun `giving up after the grace deadline is withered, not given up`() {
        val s = play(Start(info) to t0, LeftApp to t0 + min, GiveUp to t0 + min + 11 * sec)
        assertTrue(s is Withered)
    }

    @Test fun `give up when idle does nothing`() {
        assertEquals(Idle, reduce(Idle, GiveUp, t0))
    }

    // --- pause ---

    @Test fun `pause freezes the remaining time`() {
        val s = reduce(running(), Pause, t0 + 10 * min) as Paused
        assertEquals(15 * min, s.remainingMs)
        assertEquals(t0 + 15 * min, s.resumeDeadline)
        assertEquals(15 * min, SessionStateMachine.remainingMs(s, t0 + 13 * min))
    }

    @Test fun `resume pushes the end back by the pause length`() {
        val s = play(Start(info) to t0, Pause to t0 + 10 * min, Resume to t0 + 12 * min)
        assertEquals(Running(info, endsAt = end + 2 * min, pausedMs = 2 * min), s)
    }

    @Test fun `pause resumes by itself after five minutes`() {
        val s = play(Start(info) to t0, Pause to t0 + 10 * min, Tick to t0 + 16 * min)
        assertEquals(Running(info, endsAt = end + 5 * min, pausedMs = 5 * min), s)
    }

    @Test fun `a session left paused long enough finishes on its own`() {
        val s = play(Start(info) to t0, Pause to t0 + 10 * min, Tick to end + 6 * min)
        assertEquals(Success(info, endedAt = end + 5 * min, pausedMs = 5 * min), s)
    }

    @Test fun `pause budget is shared across pauses`() {
        val s = play(
            Start(info) to t0,
            Pause to t0 + 1 * min, Resume to t0 + 4 * min,   // used 3
            Pause to t0 + 5 * min,                            // 2 left
        ) as Paused
        assertEquals(t0 + 7 * min, s.resumeDeadline)
        assertEquals(3 * min, s.pausedMs)
    }

    @Test fun `no pause once the budget is spent`() {
        val spent = play(Start(info) to t0, Pause to t0 + min, Tick to t0 + 6 * min)
        assertEquals(spent, reduce(spent, Pause, t0 + 7 * min))
    }

    @Test fun `no pause while the grace period is running`() {
        val away = reduce(running(), LeftApp, t0 + min)
        assertEquals(away, reduce(away, Pause, t0 + min + sec))
    }

    @Test fun `leaving the app while paused is allowed`() {
        val paused = reduce(running(), Pause, t0 + min)
        assertEquals(paused, reduce(paused, LeftApp, t0 + 2 * min))
    }

    @Test fun `resume when not paused does nothing`() {
        assertEquals(running(), reduce(running(), Resume, t0 + min))
        assertEquals(Idle, reduce(Idle, Resume, t0))
    }

    // --- interruption and reset ---

    @Test fun `a device restart withers an active session`() {
        assertTrue(reduce(running(), Interrupted, 5L) is Withered)
        assertTrue(reduce(reduce(running(), Pause, t0 + min), Interrupted, 5L) is Withered)
    }

    @Test fun `a device restart does not touch idle or finished sessions`() {
        val done = reduce(running(), Tick, end)
        assertEquals(done, reduce(done, Interrupted, 5L))
        assertEquals(Idle, reduce(Idle, Interrupted, 5L))
    }

    @Test fun `reset clears a finished session only`() {
        assertEquals(Idle, reduce(reduce(running(), Tick, end), Reset, end + sec))
        assertEquals(Idle, reduce(reduce(running(), GiveUp, t0 + min), Reset, t0 + 2 * min))
        assertEquals(running(), reduce(running(), Reset, t0 + min))
    }

    @Test fun `finished states ignore everything except reset`() {
        val done = reduce(running(), Tick, end)
        for (e in listOf(Tick, Pause, Resume, GiveUp, LeftApp, Returned, Start(info))) {
            assertEquals("$e", done, reduce(done, e, end + min))
        }
    }

    // --- derived values ---

    @Test fun `remaining time counts down and stops at zero`() {
        assertEquals(25 * min, SessionStateMachine.remainingMs(running(), t0))
        assertEquals(min, SessionStateMachine.remainingMs(running(), end - min))
        assertEquals(0, SessionStateMachine.remainingMs(running(), end + min))
        assertEquals(0, SessionStateMachine.remainingMs(Idle, t0))
    }

    @Test fun `progress runs from 0 to 1 and ignores paused time`() {
        assertEquals(0f, SessionStateMachine.progress(running(), t0), 1e-6f)
        assertEquals(0.4f, SessionStateMachine.progress(running(), t0 + 10 * min), 1e-6f)
        val resumed = play(Start(info) to t0, Pause to t0 + 10 * min, Resume to t0 + 12 * min)
        assertEquals(0.4f, SessionStateMachine.progress(resumed, t0 + 12 * min), 1e-6f)
        assertEquals(1f, SessionStateMachine.progress(reduce(running(), Tick, end), end), 1e-6f)
        val gaveUp = play(Start(info) to t0, Pause to t0 + 5 * min, GiveUp to t0 + 7 * min)
        assertEquals(0.2f, SessionStateMachine.progress(gaveUp, t0 + 9 * min), 1e-6f)
    }

    @Test fun `five growth stages, the last one only at completion`() {
        assertEquals(1, SessionStateMachine.stage(0f))
        assertEquals(1, SessionStateMachine.stage(0.249f))
        assertEquals(2, SessionStateMachine.stage(0.25f))
        assertEquals(3, SessionStateMachine.stage(0.5f))
        assertEquals(4, SessionStateMachine.stage(0.75f))
        assertEquals(4, SessionStateMachine.stage(0.999f))
        assertEquals(5, SessionStateMachine.stage(1f))
        assertEquals(5, SessionStateMachine.stage(7f))
        assertEquals(1, SessionStateMachine.stage(-1f))
    }

    @Test fun `duration snapping`() {
        assertEquals(10, SessionRules.snapDuration(3))
        assertEquals(25, SessionRules.snapDuration(27))
        assertEquals(30, SessionRules.snapDuration(28))
        assertEquals(120, SessionRules.snapDuration(500))
        assertFalse(SessionRules.isValidDuration(27))
        assertNull((running() as Running).graceDeadline)
    }
}
