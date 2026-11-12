package app.ghars.domain.session

interface Clock {
    /** Monotonic milliseconds since boot, counting sleep. Immune to the user changing the time. */
    fun elapsedMs(): Long

    /** Wall-clock milliseconds, for records only. */
    fun wallMs(): Long
}
