package app.ghars.domain.session

import kotlin.math.abs
import kotlin.math.atan2

/** Geometry of the circular duration slider: 10 minutes at 12 o'clock, clockwise to 120. */
object DurationDial {
    /** The ring stops short of a full turn, like the logo, so 120 does not land on top of 10. */
    const val SWEEP_DEGREES = 330f

    private const val RANGE = SessionRules.MAX_MINUTES - SessionRules.MIN_MINUTES

    fun fraction(minutes: Int): Float =
        ((minutes - SessionRules.MIN_MINUTES).toFloat() / RANGE).coerceIn(0f, 1f)

    /**
     * Minutes for a touch at ([dx], [dy]) from the centre, y pointing down.
     * @param dragging while dragging, the value may not jump across the gap between 120 and 10.
     */
    fun minutesAt(dx: Float, dy: Float, current: Int, dragging: Boolean): Int {
        var degrees = Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())).toFloat()
        if (degrees < 0) degrees += 360f
        val raw = when {
            degrees <= SWEEP_DEGREES -> SessionRules.MIN_MINUTES + degrees / SWEEP_DEGREES * RANGE
            degrees - SWEEP_DEGREES < (360f - SWEEP_DEGREES) / 2 -> SessionRules.MAX_MINUTES.toFloat()
            else -> SessionRules.MIN_MINUTES.toFloat()
        }
        val snapped = SessionRules.snapDuration(Math.round(raw))
        if (dragging && abs(snapped - current) > RANGE / 2) {
            return if (current > SessionRules.MIN_MINUTES + RANGE / 2) SessionRules.MAX_MINUTES else SessionRules.MIN_MINUTES
        }
        return snapped
    }
}

/** "25:00" style countdown. Rounds up, so 00:00 appears only when time is really over. */
fun formatCountdown(remainingMs: Long): String {
    val seconds = (remainingMs.coerceAtLeast(0) + 999) / 1000
    return "%02d:%02d".format(java.util.Locale.ROOT, seconds / 60, seconds % 60)
}
