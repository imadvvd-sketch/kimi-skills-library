package app.imadnote.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/** Time source, injectable so tests and analytics are deterministic. */
fun interface Clock {
    fun now(): Long

    companion object {
        val System = Clock { java.lang.System.currentTimeMillis() }
    }
}

fun Clock.today(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(now()).atZone(zone).toLocalDate()

fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

fun LocalDate.startOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    atStartOfDay(zone).toInstant().toEpochMilli()
