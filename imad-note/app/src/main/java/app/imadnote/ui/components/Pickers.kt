package app.imadnote.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.os.ConfigurationCompat
import app.imadnote.R
import app.imadnote.ui.editor.InlineColor
import app.imadnote.ui.theme.ImadColors
import app.imadnote.ui.theme.ImadTheme
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun currentLocale(): Locale = ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault()

/** "Today", "Tomorrow", "Yesterday", a short weekday date this year, or a full date otherwise. */
@Composable
fun formatDay(date: LocalDate, today: LocalDate = LocalDate.now()): String {
    val locale = currentLocale()
    return when (date) {
        today -> stringResource(R.string.date_today)
        today.plusDays(1) -> stringResource(R.string.date_tomorrow)
        today.minusDays(1) -> stringResource(R.string.date_yesterday)
        else -> remember(date, locale) {
            val pattern = if (date.year == today.year) "EEE d MMM" else "d MMM yyyy"
            date.format(DateTimeFormatter.ofPattern(pattern, locale))
        }
    }
}

@Composable
fun formatTime(minutes: Int): String {
    val locale = currentLocale()
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    return remember(minutes, locale, is24) {
        LocalTime.of(minutes / 60, minutes % 60).format(DateTimeFormatter.ofPattern(if (is24) "HH:mm" else "h:mm a", locale))
    }
}

@Composable
fun formatDateTime(millis: Long): String {
    val dt = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    return formatDay(dt.toLocalDate()) + " · " + formatTime(dt.hour * 60 + dt.minute)
}

@Composable
fun formatLongDate(date: LocalDate): String {
    val locale = currentLocale()
    return remember(date, locale) { date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerModal(initial: LocalDate?, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit, onClear: (() -> Unit)? = null) {
    val colors = ImadTheme.colors
    // The Material date picker works in UTC milliseconds.
    val state = rememberDatePickerState(initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text(stringResource(R.string.action_done)) }
        },
        dismissButton = {
            Row {
                if (onClear != null) TextButton(onClick = { onClear(); onDismiss() }) { Text(stringResource(R.string.action_clear), color = colors.danger) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.textSecondary) }
            }
        },
        colors = DatePickerDefaults.colors(containerColor = colors.surface),
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = colors.surface, selectedDayContainerColor = colors.primary, selectedDayContentColor = colors.onPrimary,
                todayDateBorderColor = colors.primary, todayContentColor = colors.primary,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerModal(initialMinutes: Int?, onPick: (Int) -> Unit, onDismiss: () -> Unit, onClear: (() -> Unit)? = null) {
    val colors = ImadTheme.colors
    val start = initialMinutes ?: (9 * 60)
    val state = rememberTimePickerState(start / 60, start % 60, DateFormat.is24HourFormat(LocalContext.current))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.time_pick), style = MaterialTheme.typography.titleLarge) },
        text = {
            TimePicker(
                state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = colors.surfaceAlt, selectorColor = colors.primary, timeSelectorSelectedContainerColor = colors.primarySoft,
                    timeSelectorSelectedContentColor = colors.primary, periodSelectorSelectedContainerColor = colors.primarySoft,
                ),
            )
        },
        confirmButton = { TextButton(onClick = { onPick(state.hour * 60 + state.minute); onDismiss() }) { Text(stringResource(R.string.action_done)) } },
        dismissButton = {
            Row {
                if (onClear != null) TextButton(onClick = { onClear(); onDismiss() }) { Text(stringResource(R.string.action_clear), color = colors.danger) }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.textSecondary) }
            }
        },
        containerColor = colors.surface,
        shape = MaterialTheme.shapes.large,
    )
}

/** Single-choice list dialog. */
@Composable
fun <T> ChoiceDialog(title: String, options: List<Pair<T, String>>, selected: T?, onPick: (T) -> Unit, onDismiss: () -> Unit) {
    val colors = ImadTheme.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                for ((value, label) in options) {
                    val active = value == selected
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                            .clickable(role = Role.RadioButton) { onPick(value); onDismiss() }
                            .semantics { this.selected = active }.padding(horizontal = 8.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (active) Icon(Icons.Rounded.Check, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.textSecondary) } },
        containerColor = colors.surface,
        shape = MaterialTheme.shapes.large,
    )
}

/** Horizontally scrolling row of selectable chips. */
@Composable
fun <T> ChipRow(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for ((value, label) in options) SelectChip(label, value == selected) { onSelect(value) }
    }
}

@Composable
fun SelectChip(label: String, selected: Boolean, color: Color? = null, onClick: () -> Unit) {
    val colors = ImadTheme.colors
    val accent = color ?: colors.primary
    Text(
        label,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
        color = if (selected) accent else colors.textSecondary,
        maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background(if (selected) accent.copy(alpha = if (colors.isDark) 0.22f else 0.12f) else colors.surface)
            .border(1.dp, if (selected) Color.Transparent else colors.border, RoundedCornerShape(50))
            .clickable(role = Role.Tab, onClick = onClick).semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}

/** A settings-style row with a label and a value; the whole row is tappable. */
@Composable
fun PropertyRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, onClick: () -> Unit, valueColor: Color? = null, trailing: (@Composable () -> Unit)? = null) {
    val colors = ImadTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onClick).padding(horizontal = 4.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.width(112.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = valueColor ?: colors.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing?.invoke()
    }
}

@Composable
fun ColorDot(color: Color, size: androidx.compose.ui.unit.Dp = 10.dp) = Box(Modifier.size(size).clip(CircleShape).background(color))

fun priorityColor(priority: Int, colors: ImadColors): Color = when (priority) {
    3 -> colors.danger
    2 -> colors.warning
    1 -> colors.primary
    else -> colors.textTertiary
}

/** Option/label colour keys shared with inline text colours. */
fun optionColor(key: String?, colors: ImadColors): Color = InlineColor.fromKey(key)?.text(colors) ?: colors.textSecondary

val ProjectPalette = listOf(0xFF527DAA, 0xFF2E9D73, 0xFFE5A43B, 0xFFD9534F, 0xFF7B55C7, 0xFF3A9BB5, 0xFF243B53, 0xFFB7791F)

/** Lightens stored project/label colours in dark mode so they keep contrast. */
fun storedColor(argb: Long, colors: ImadColors): Color {
    val c = Color(argb.toInt())
    return if (colors.isDark) androidx.compose.ui.graphics.lerp(c, Color.White, 0.25f) else c
}
