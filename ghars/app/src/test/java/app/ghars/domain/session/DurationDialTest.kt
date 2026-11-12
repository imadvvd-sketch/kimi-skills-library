package app.ghars.domain.session

import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Test

class DurationDialTest {
    /** A touch at [degrees] clockwise from 12 o'clock. */
    private fun at(degrees: Float, current: Int = 60, dragging: Boolean = false): Int {
        val rad = Math.toRadians(degrees.toDouble())
        return DurationDial.minutesAt(sin(rad).toFloat() * 100, -cos(rad).toFloat() * 100, current, dragging)
    }

    @Test fun `top is the minimum and the end of the sweep is the maximum`() {
        assertEquals(10, at(0f))
        assertEquals(120, at(330f))
        assertEquals(65, at(165f))
    }

    @Test fun `values snap to five minutes`() {
        assertEquals(25, at(45f))  // 45/330*110+10 = 25
        assertEquals(25, at(49f))
        assertEquals(30, at(55f))
    }

    @Test fun `the gap snaps to the nearer end`() {
        assertEquals(120, at(340f))
        assertEquals(10, at(350f))
    }

    @Test fun `dragging cannot jump across the gap`() {
        assertEquals(120, at(5f, current = 115, dragging = true))
        assertEquals(10, at(325f, current = 15, dragging = true))
        assertEquals(100, at(270f, current = 115, dragging = true))
    }

    @Test fun `a tap may land anywhere`() {
        assertEquals(100, at(270f, current = 10, dragging = false))
    }

    @Test fun `fraction of the sweep`() {
        assertEquals(0f, DurationDial.fraction(10), 1e-6f)
        assertEquals(0.5f, DurationDial.fraction(65), 1e-6f)
        assertEquals(1f, DurationDial.fraction(120), 1e-6f)
    }

    @Test fun `countdown text`() {
        assertEquals("25:00", formatCountdown(25 * 60_000L))
        assertEquals("24:59", formatCountdown(25 * 60_000L - 1_000))
        assertEquals("25:00", formatCountdown(25 * 60_000L - 1))
        assertEquals("00:01", formatCountdown(1))
        assertEquals("00:00", formatCountdown(0))
        assertEquals("00:00", formatCountdown(-500))
        assertEquals("120:00", formatCountdown(120 * 60_000L))
    }
}
