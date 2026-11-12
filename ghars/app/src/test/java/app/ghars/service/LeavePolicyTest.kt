package app.ghars.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LeavePolicyTest {
    private fun away(fg: Boolean = false, on: Boolean = true, locked: Boolean = false, call: Boolean = false) =
        LeavePolicy.isAway(fg, on, locked, call)

    @Test fun `another app with the screen on is away`() = assertTrue(away())
    @Test fun `our app on screen is not away`() = assertFalse(away(fg = true))
    @Test fun `screen off is allowed`() = assertFalse(away(on = false))
    @Test fun `glancing at the lock screen is allowed`() = assertFalse(away(locked = true))
    @Test fun `a call is allowed`() = assertFalse(away(call = true))

    @Test fun `minutes left rounds up`() {
        assertTrue(SessionNotifier.minutesLeft(1) == 1)
        assertTrue(SessionNotifier.minutesLeft(60_000) == 1)
        assertTrue(SessionNotifier.minutesLeft(60_001) == 2)
        assertTrue(SessionNotifier.minutesLeft(0) == 0)
    }
}
