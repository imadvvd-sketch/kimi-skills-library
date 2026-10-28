package com.imadnote.app.ui.components

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.imadnote.app.R
import com.imadnote.app.domain.Recurrence
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun dueLabel(dueAt: Long, hasTime: Boolean): String {
    val zone = ZoneId.systemDefault()
    val dt = Instant.ofEpochMilli(dueAt).atZone(zone)
    val today = LocalDate.now(zone)
    val locale = LocalConfiguration.current.locales[0]
    val date = when (dt.toLocalDate()) {
        today -> stringResource(R.string.date_today)
        today.plusDays(1) -> stringResource(R.string.date_tomorrow)
        today.minusDays(1) -> stringResource(R.string.date_yesterday)
        else -> {
            val pattern = if (dt.year == today.year) "d MMM" else "d MMM yyyy"
            DateTimeFormatter.ofPattern(pattern, locale).format(dt)
        }
    }
    return if (hasTime) "$date ${timeLabel(dueAt)}" else date
}

@Composable
fun timeLabel(millis: Long): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return DateTimeFormatter.ofPattern(pattern, locale).format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}

@Composable
fun dateLabel(millis: Long): String {
    val locale = LocalConfiguration.current.locales[0]
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}

@Composable
fun recurrenceLabel(r: Recurrence): String = stringResource(
    when (r) {
        Recurrence.NONE -> R.string.repeat_none
        Recurrence.DAILY -> R.string.repeat_daily
        Recurrence.WEEKLY -> R.string.repeat_weekly
        Recurrence.MONTHLY -> R.string.repeat_monthly
        Recurrence.YEARLY -> R.string.repeat_yearly
    },
)

@Composable
fun reminderLabel(offsetMin: Int?): String = when (offsetMin) {
    null -> stringResource(R.string.reminder_none)
    0 -> stringResource(R.string.reminder_on_time)
    10 -> stringResource(R.string.reminder_10_min)
    60 -> stringResource(R.string.reminder_1_hour)
    1440 -> stringResource(R.string.reminder_1_day)
    else -> stringResource(R.string.reminder_minutes, offsetMin)
}

val ReminderOptions = listOf<Int?>(null, 0, 10, 60, 1440)
