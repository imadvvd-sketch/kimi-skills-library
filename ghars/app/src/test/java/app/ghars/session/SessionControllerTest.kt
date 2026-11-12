package app.ghars.session

import app.ghars.data.prefs.ActiveSessionStore
import app.ghars.data.prefs.SessionSnapshot
import app.ghars.data.repo.SessionRecorder
import app.ghars.domain.session.Clock
import app.ghars.domain.session.SessionInfo
import app.ghars.domain.session.SessionState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionControllerTest {

    private class FakeClock(var elapsed: Long = 1_000_000, var wall: Long = 1_700_000_000_000) : Clock {
        override fun elapsedMs() = elapsed
        override fun wallMs() = wall
        fun advance(ms: Long) { elapsed += ms; wall += ms }
    }

    private class FakeStore(var saved: SessionSnapshot? = null) : ActiveSessionStore {
        override suspend fun load() = saved
        override suspend fun save(snapshot: SessionSnapshot) { saved = snapshot }
    }

    private class FakeRecorder : SessionRecorder {
        val records = mutableListOf<Pair<SessionState.Finished, Long>>()
        override suspend fun record(finished: SessionState.Finished, endedAtWall: Long) {
            records += finished to endedAtWall
        }
    }

    private val clock = FakeClock()
    private val store = FakeStore()
    private val recorder = FakeRecorder()
    private var boot = 7
    private var serviceStarts = 0
    private val min = 60_000L

    private fun TestScope.controller() = SessionController(
        clock, store, recorder, { serviceStarts++ }, { boot }, CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
    )

    private fun CoroutineScope(d: kotlinx.coroutines.CoroutineDispatcher) = kotlinx.coroutines.CoroutineScope(d)

    @Test fun `fresh install starts idle`() = runTest {
        val c = controller()
        assertEquals(SessionState.Idle, c.state.value)
        assertEquals(0, serviceStarts)
    }

    @Test fun `starting persists the session and starts the service`() = runTest {
        val c = controller()
        c.start("olive", 25)
        val state = c.state.value as SessionState.Running
        assertEquals(clock.elapsed + 25 * min, state.endsAt)
        assertEquals(state, store.saved?.state)
        assertEquals(7, store.saved?.bootCount)
        assertEquals(1, serviceStarts)
    }

    @Test fun `a completed session is recorded once with the right end time`() = runTest {
        val c = controller()
        val startWall = clock.wall
        c.start("lemon", 10)
        clock.advance(10 * min + 3_000) // noticed three seconds late
        c.tick(away = false)
        c.tick(away = false)
        assertTrue(c.state.value is SessionState.Success)
        assertEquals(1, recorder.records.size)
        assertEquals(startWall + 10 * min, recorder.records[0].second)
    }

    @Test fun `being away starts the grace period and coming back clears it`() = runTest {
        val c = controller()
        c.start("olive", 25)
        clock.advance(min)
        c.tick(away = true)
        assertEquals(clock.elapsed + 10_000, (c.state.value as SessionState.Running).graceDeadline)
        clock.advance(4_000)
        c.tick(away = false)
        assertNull((c.state.value as SessionState.Running).graceDeadline)
    }

    @Test fun `staying away withers the tree and records it`() = runTest {
        val c = controller()
        c.start("olive", 25)
        clock.advance(min)
        c.tick(away = true)
        clock.advance(10_000)
        c.tick(away = true)
        assertTrue(c.state.value is SessionState.Withered)
        assertTrue(recorder.records.single().first is SessionState.Withered)
    }

    @Test fun `giving up then acknowledging returns to idle`() = runTest {
        val c = controller()
        c.start("cactus", 30)
        clock.advance(min)
        c.giveUp()
        assertTrue(c.state.value is SessionState.GivenUp)
        c.acknowledgeResult()
        assertEquals(SessionState.Idle, c.state.value)
        assertEquals(SessionState.Idle, store.saved?.state)
        assertEquals(1, recorder.records.size)
    }

    // --- process death ---

    private fun savedRunning(endsIn: Long, grace: Long? = null) {
        val info = SessionInfo("olive", 25, clock.elapsed - (25 * min - endsIn), clock.wall - (25 * min - endsIn))
        store.saved = SessionSnapshot(
            SessionState.Running(info, endsAt = clock.elapsed + endsIn, graceDeadline = grace), bootCount = 7,
        )
    }

    @Test fun `a session still running after process death carries on`() = runTest {
        savedRunning(endsIn = 5 * min)
        val c = controller()
        assertTrue(c.state.value is SessionState.Running)
        assertEquals(1, serviceStarts)
        assertTrue(recorder.records.isEmpty())
    }

    @Test fun `a session that ended while the process was dead is a success`() = runTest {
        savedRunning(endsIn = 5 * min)
        clock.advance(30 * min)
        val c = controller()
        assertTrue(c.state.value is SessionState.Success)
        assertEquals(1, recorder.records.size)
        assertEquals(0, serviceStarts)
    }

    @Test fun `process death during the grace period withers the tree`() = runTest {
        savedRunning(endsIn = 5 * min, grace = clock.elapsed + 4_000)
        clock.advance(60_000)
        val c = controller()
        assertTrue(c.state.value is SessionState.Withered)
    }

    @Test fun `a device restart withers the session even if time looks fine`() = runTest {
        savedRunning(endsIn = 5 * min)
        boot = 8
        val c = controller()
        assertTrue(c.state.value is SessionState.Withered)
        assertEquals(clock.wall, recorder.records.single().second)
        assertEquals(8, store.saved?.bootCount)
        assertEquals(0, serviceStarts)
    }

    @Test fun `an unseen result survives a restart without being recorded twice`() = runTest {
        val first = controller()
        first.start("olive", 10)
        clock.advance(10 * min)
        first.tick(away = false)
        boot = 8
        val second = controller()
        assertTrue(second.state.value is SessionState.Success)
        assertEquals(1, recorder.records.size)
    }
}
