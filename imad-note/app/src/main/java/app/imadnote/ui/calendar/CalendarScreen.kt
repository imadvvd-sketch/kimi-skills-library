package app.imadnote.ui.calendar

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.imadnote.AppContainer
import app.imadnote.R
import app.imadnote.data.db.EventEntity
import app.imadnote.data.db.ProjectEntity
import app.imadnote.data.db.TaskEntity
import app.imadnote.data.reminders.AlarmReminders
import app.imadnote.ui.appViewModel
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.ChipRow
import app.imadnote.ui.components.ChoiceDialog
import app.imadnote.ui.components.DatePickerModal
import app.imadnote.ui.components.PrimaryButton
import app.imadnote.ui.components.ProjectPalette
import app.imadnote.ui.components.PropertyRow
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.TimePickerModal
import app.imadnote.ui.components.currentLocale
import app.imadnote.ui.components.formatDateTime
import app.imadnote.ui.components.formatDay
import app.imadnote.ui.components.formatLongDate
import app.imadnote.ui.components.formatTime
import app.imadnote.ui.components.priorityColor
import app.imadnote.ui.components.storedColor
import app.imadnote.ui.tasks.TaskCheck
import app.imadnote.ui.tasks.due
import app.imadnote.ui.tasks.isDone
import app.imadnote.ui.theme.ImadTheme
import app.imadnote.ui.workspaceId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/** Anything shown on the calendar. */
sealed interface CalItem {
    data class Event(val event: EventEntity) : CalItem
    data class Task(val task: TaskEntity) : CalItem
    data class Deadline(val project: ProjectEntity) : CalItem
}

data class CalendarData(val workspaceId: String, val events: List<EventEntity>, val tasks: List<TaskEntity>, val projects: List<ProjectEntity>) {
    private val zone = ZoneId.systemDefault()
    private fun EventEntity.days(): ClosedRange<LocalDate> {
        val start = Instant.ofEpochMilli(startAt).atZone(zone).toLocalDate()
        val end = Instant.ofEpochMilli(maxOf(endAt - 1, startAt)).atZone(zone).toLocalDate()
        return start..maxOf(start, end)
    }

    fun on(date: LocalDate): List<CalItem> = buildList {
        events.filter { date in it.days() }.sortedWith(compareBy({ !it.allDay }, { it.startAt })).forEach { add(CalItem.Event(it)) }
        projects.filter { it.deadline == date.toEpochDay() }.forEach { add(CalItem.Deadline(it)) }
        tasks.filter { it.parentId == null && it.due == date }.sortedWith(compareBy({ it.isDone }, { it.dueTime ?: Int.MAX_VALUE })).forEach { add(CalItem.Task(it)) }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val container: AppContainer) : ViewModel() {
    /** Loaded event window; widened when the user navigates. */
    private val window = MutableStateFlow(LocalDate.now().let { it.minusMonths(2) to it.plusMonths(3) })

    val data: StateFlow<CalendarData?> = container.workspaceId().flatMapLatest { ws ->
        if (ws == null) flowOf(null)
        else window.flatMapLatest { (from, to) ->
            val zone = ZoneId.systemDefault()
            combine(
                container.events.observeBetween(ws, from.atStartOfDay(zone).toInstant().toEpochMilli(), to.atStartOfDay(zone).toInstant().toEpochMilli()),
                container.tasks.observeAll(ws), container.projects.observeAll(ws),
            ) { e, t, p -> CalendarData(ws, e, t, p) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun ensureVisible(date: LocalDate) {
        val (from, to) = window.value
        if (date.minusMonths(1) < from || date.plusMonths(2) > to) window.value = date.minusMonths(2) to date.plusMonths(3)
    }

    fun save(event: EventEntity) { viewModelScope.launch { container.events.save(event) } }
    fun delete(id: String) { viewModelScope.launch { container.events.delete(id) } }
    fun toggleTask(task: TaskEntity) { viewModelScope.launch { container.tasks.setCompleted(task.id, !task.isDone) } }
}

enum class CalMode { MONTH, WEEK, DAY, AGENDA }

@Composable
fun CalendarScreen(onOpenMenu: (() -> Unit)?, onOpenTask: (String) -> Unit, onOpenProject: (String) -> Unit) {
    val viewModel = appViewModel { CalendarViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    var mode by rememberSaveable { mutableStateOf(CalMode.MONTH) }
    var selectedEpoch by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
    val selected = LocalDate.ofEpochDay(selectedEpoch)
    var editing by remember { mutableStateOf<EventEntity?>(null) }
    val locale = currentLocale()
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    LaunchedEffect(selected) { viewModel.ensureVisible(selected) }

    fun select(d: LocalDate) { selectedEpoch = d.toEpochDay() }
    fun newEvent(day: LocalDate) {
        val ws = data?.workspaceId ?: return
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).plusHours(if (day == LocalDate.now()) (java.time.LocalTime.now().hour + 1).coerceAtMost(23).toLong() else 9)
        editing = EventEntity("", ws, "", startAt = start.toInstant().toEpochMilli(), endAt = start.plusHours(1).toInstant().toEpochMilli(), createdAt = 0, updatedAt = 0)
    }
    val onItem: (CalItem) -> Unit = { item ->
        when (item) {
            is CalItem.Event -> editing = item.event
            is CalItem.Task -> onOpenTask(item.task.id)
            is CalItem.Deadline -> onOpenProject(item.project.id)
        }
    }

    ScreenScaffold(
        title = stringResource(R.string.nav_calendar), onOpenMenu = onOpenMenu,
        actions = { TextButton(onClick = { select(LocalDate.now()) }) { Text(stringResource(R.string.cal_today), color = ImadTheme.colors.primary) } },
        fabIcon = Icons.Rounded.Add, fabLabel = stringResource(R.string.cal_new_event), onFab = { newEvent(selected) },
    ) {
        // While loading, the grid is drawn empty so the layout does not jump.
        val current = data ?: CalendarData("", emptyList(), emptyList(), emptyList())
        Column(Modifier.fillMaxSize()) {
            ChipRow(
                listOf(CalMode.MONTH to stringResource(R.string.cal_month), CalMode.WEEK to stringResource(R.string.cal_week), CalMode.DAY to stringResource(R.string.cal_day), CalMode.AGENDA to stringResource(R.string.cal_agenda)),
                mode, { mode = it }, Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            val title = when (mode) {
                CalMode.MONTH -> YearMonth.from(selected).let { it.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { c -> c.titlecase(locale) } + " " + it.year }
                CalMode.WEEK -> selected.with(firstDay).let { formatDay(it) + " – " + formatDay(it.plusDays(6)) }
                else -> formatLongDate(selected)
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val step: (Long) -> LocalDate = { n ->
                    when (mode) {
                        CalMode.MONTH -> selected.plusMonths(n)
                        CalMode.WEEK -> selected.plusWeeks(n)
                        else -> selected.plusDays(n)
                    }
                }
                AppIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.cal_previous), { select(step(-1)) }, tint = ImadTheme.colors.text)
                Text(title, style = MaterialTheme.typography.titleMedium, color = ImadTheme.colors.text, textAlign = TextAlign.Center, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                AppIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.cal_next), { select(step(1)) }, tint = ImadTheme.colors.text)
            }
            when (mode) {
                CalMode.MONTH -> Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 96.dp)) {
                    MonthGrid(selected, firstDay, current, ::select)
                    HorizontalDivider(color = ImadTheme.colors.border, modifier = Modifier.padding(vertical = 8.dp))
                    DayList(selected, current, onItem, viewModel::toggleTask)
                }
                CalMode.WEEK -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    val start = selected.with(firstDay)
                    items((0L..6L).map { start.plusDays(it) }, key = { it.toEpochDay() }) { day -> DayList(day, current, onItem, viewModel::toggleTask, showHeader = true, onHeader = { select(day); mode = CalMode.DAY }) }
                }
                CalMode.DAY -> DayTimeline(selected, current, onItem, viewModel::toggleTask)
                CalMode.AGENDA -> LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
                    val days = (0L until 30L).map { selected.plusDays(it) }.filter { current.on(it).isNotEmpty() }
                    if (days.isEmpty()) item { Text(stringResource(R.string.cal_agenda_empty), style = MaterialTheme.typography.bodyMedium, color = ImadTheme.colors.textSecondary, modifier = Modifier.padding(24.dp)) }
                    items(days, key = { it.toEpochDay() }) { day -> DayList(day, current, onItem, viewModel::toggleTask, showHeader = true) }
                }
            }
        }
    }
    editing?.let { event ->
        EventEditor(event, onSave = { viewModel.save(it); editing = null }, onDelete = { viewModel.delete(event.id); editing = null }, onDismiss = { editing = null })
    }
}

@Composable
private fun MonthGrid(selected: LocalDate, firstDay: java.time.DayOfWeek, data: CalendarData, onSelect: (LocalDate) -> Unit) {
    val colors = ImadTheme.colors
    val locale = currentLocale()
    val today = LocalDate.now()
    val month = YearMonth.from(selected)
    val gridStart = month.atDay(1).let { d -> d.minusDays(((d.dayOfWeek.value - firstDay.value + 7) % 7).toLong()) }
    val weeks = ((month.atEndOfMonth().toEpochDay() - gridStart.toEpochDay()) / 7 + 1).toInt()
    Column(Modifier.padding(horizontal = 8.dp)) {
        Row {
            for (i in 0 until 7) {
                Text(
                    firstDay.plus(i.toLong()).getDisplayName(TextStyle.NARROW_STANDALONE, locale), style = MaterialTheme.typography.labelMedium,
                    color = colors.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                )
            }
        }
        for (w in 0 until weeks) {
            Row {
                for (d in 0 until 7) {
                    val day = gridStart.plusDays((w * 7 + d).toLong())
                    val inMonth = day.month == month.month
                    val isSelected = day == selected
                    val items = data.on(day)
                    Column(
                        Modifier.weight(1f).aspectRatio(0.9f).padding(2.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) colors.primarySoft else Color.Transparent)
                            .clickable(role = Role.Button) { onSelect(day) }.semantics { this.selected = isSelected },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(if (day == today) colors.primary else Color.Transparent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                day.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (day == today || isSelected) FontWeight.SemiBold else FontWeight.Normal),
                                color = when {
                                    day == today -> colors.onPrimary
                                    !inMonth -> colors.textTertiary.copy(alpha = 0.6f)
                                    else -> colors.text
                                },
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            for (item in items.take(4)) Box(Modifier.size(5.dp).clip(CircleShape).background(itemColor(item)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun itemColor(item: CalItem): Color {
    val colors = ImadTheme.colors
    return when (item) {
        is CalItem.Event -> storedColor(item.event.color, colors)
        is CalItem.Task -> if (item.task.isDone) colors.textTertiary else priorityColor(item.task.priority, colors).takeIf { item.task.priority > 0 } ?: colors.navy
        is CalItem.Deadline -> colors.danger
    }
}

@Composable
private fun DayList(day: LocalDate, data: CalendarData, onItem: (CalItem) -> Unit, onToggle: (TaskEntity) -> Unit, showHeader: Boolean = false, onHeader: (() -> Unit)? = null) {
    val colors = ImadTheme.colors
    val items = data.on(day)
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        if (showHeader) {
            Text(
                formatDay(day), style = MaterialTheme.typography.titleSmall, color = if (day == LocalDate.now()) colors.primary else colors.textSecondary,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp).then(if (onHeader != null) Modifier.clickable(onClick = onHeader) else Modifier),
            )
        }
        if (items.isEmpty()) {
            Text(stringResource(R.string.cal_nothing), style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary, modifier = Modifier.padding(vertical = 8.dp))
        }
        for (item in items) ItemRow(item, onItem, onToggle)
    }
}

@Composable
private fun ItemRow(item: CalItem, onItem: (CalItem) -> Unit, onToggle: (TaskEntity) -> Unit) {
    val colors = ImadTheme.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface)
            .clickable { onItem(item) }.padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (item) {
            is CalItem.Task -> TaskCheck(item.task.isDone, item.task.priority, { onToggle(item.task) })
            else -> Box(Modifier.padding(horizontal = 14.dp).width(4.dp).height(32.dp).clip(RoundedCornerShape(2.dp)).background(itemColor(item)))
        }
        Column(Modifier.weight(1f).padding(vertical = 10.dp)) {
            val (title, sub) = when (item) {
                is CalItem.Event -> item.event.title.ifBlank { stringResource(R.string.untitled) } to
                    if (item.event.allDay) stringResource(R.string.cal_all_day) else formatTime(minutes(item.event.startAt)) + " – " + formatTime(minutes(item.event.endAt))
                is CalItem.Task -> item.task.title to item.task.dueTime?.let { formatTime(it) }
                is CalItem.Deadline -> "${item.project.icon} ${item.project.name}" to stringResource(R.string.project_deadline)
            }
            Text(
                title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = if (item is CalItem.Task && item.task.isDone) colors.textTertiary else colors.text,
                textDecoration = if (item is CalItem.Task && item.task.isDone) TextDecoration.LineThrough else null,
            )
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }
    }
}

private fun minutes(millis: Long): Int = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).let { it.hour * 60 + it.minute }

/** Hour grid for one day; overlapping events share the width side by side. */
@Composable
private fun DayTimeline(day: LocalDate, data: CalendarData, onItem: (CalItem) -> Unit, onToggle: (TaskEntity) -> Unit) {
    val colors = ImadTheme.colors
    val items = data.on(day)
    val zone = ZoneId.systemDefault()
    val dayStart = day.atStartOfDay(zone).toInstant().toEpochMilli()
    val timed = items.filterIsInstance<CalItem.Event>().map { it.event }.filter { !it.allDay }
    val untimed = items.filter { it !is CalItem.Event || it.event.allDay }
    val hour = 56.dp
    val scroll = rememberScrollState()
    LaunchedEffect(day) { scroll.scrollTo((hour.value * 7 * 2.6f).toInt()) }
    Column(Modifier.fillMaxSize()) {
        if (untimed.isNotEmpty()) Column(Modifier.padding(horizontal = 16.dp)) { for (item in untimed) ItemRow(item, onItem, onToggle) }
        BoxWithConstraints(Modifier.fillMaxSize().verticalScroll(scroll).padding(bottom = 96.dp)) {
            val laneWidth = maxWidth - 64.dp
            Column {
                for (h in 0 until 24) {
                    Row(Modifier.height(hour)) {
                        Text(formatTime(h * 60), style = MaterialTheme.typography.labelSmall, color = colors.textTertiary, modifier = Modifier.width(56.dp).padding(start = 8.dp))
                        Column { HorizontalDivider(color = colors.border) }
                    }
                }
            }
            // Greedy column assignment for overlaps.
            val sorted = timed.sortedBy { it.startAt }
            val columnEnds = ArrayList<Long>()
            val placed = sorted.map { e ->
                val col = columnEnds.indexOfFirst { it <= e.startAt }.let { if (it < 0) { columnEnds += e.endAt; columnEnds.lastIndex } else { columnEnds[it] = e.endAt; it } }
                e to col
            }
            val cols = maxOf(1, columnEnds.size)
            for ((event, col) in placed) {
                val top = ((event.startAt - dayStart).coerceAtLeast(0) / 60_000f / 60f)
                val length = ((minOf(event.endAt, dayStart + 86_400_000) - maxOf(event.startAt, dayStart)) / 60_000f / 60f).coerceAtLeast(0.4f)
                val width = laneWidth / cols
                Column(
                    Modifier.offset(x = 60.dp + width * col, y = hour * top).width(width - 4.dp).height(hour * length - 2.dp)
                        .clip(RoundedCornerShape(8.dp)).background(storedColor(event.color, colors).copy(alpha = 0.18f))
                        .border(1.dp, storedColor(event.color, colors).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable { onItem(CalItem.Event(event)) }.padding(6.dp),
                ) {
                    Text(event.title.ifBlank { stringResource(R.string.untitled) }, style = MaterialTheme.typography.labelLarge, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(formatTime(minutes(event.startAt)), style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                }
            }
            if (day == LocalDate.now()) {
                val now = java.time.LocalTime.now().let { it.hour + it.minute / 60f }
                Box(Modifier.offset(x = 56.dp, y = hour * now).fillMaxWidth().height(2.dp).background(colors.danger))
            }
        }
    }
}

private enum class EventReminder { NONE, AT_TIME, MIN_10, HOUR_1, DAY_1 }

@Composable
fun EventEditor(initial: EventEntity, onSave: (EventEntity) -> Unit, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val colors = ImadTheme.colors
    val context = LocalContext.current
    val zone = ZoneId.systemDefault()
    var event by remember(initial.id) { mutableStateOf(initial) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var reminder by remember(initial.id) {
        mutableStateOf(
            when (initial.reminderAt?.let { initial.startAt - it }) {
                null -> EventReminder.NONE
                0L -> EventReminder.AT_TIME
                600_000L -> EventReminder.MIN_10
                3_600_000L -> EventReminder.HOUR_1
                86_400_000L -> EventReminder.DAY_1
                else -> EventReminder.AT_TIME
            }
        )
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val start = Instant.ofEpochMilli(event.startAt).atZone(zone)
    val end = Instant.ofEpochMilli(event.endAt).atZone(zone)
    fun withStart(date: LocalDate = start.toLocalDate(), minutes: Int = start.hour * 60 + start.minute) {
        val duration = event.endAt - event.startAt
        val newStart = date.atStartOfDay(zone).plusMinutes(minutes.toLong()).toInstant().toEpochMilli()
        event = event.copy(startAt = newStart, endAt = newStart + duration)
    }
    fun save() {
        val offset = when (reminder) {
            EventReminder.NONE -> null
            EventReminder.AT_TIME -> 0L
            EventReminder.MIN_10 -> 600_000L
            EventReminder.HOUR_1 -> 3_600_000L
            EventReminder.DAY_1 -> 86_400_000L
        }
        val normalized = if (event.allDay) {
            val d = start.toLocalDate()
            event.copy(startAt = d.atStartOfDay(zone).toInstant().toEpochMilli(), endAt = maxOf(end.toLocalDate(), d).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1)
        } else event
        val reminderBase = if (normalized.allDay) start.toLocalDate().atStartOfDay(zone).plusHours(9).toInstant().toEpochMilli() else normalized.startAt
        onSave(normalized.copy(reminderAt = offset?.let { reminderBase - it }))
    }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(if (initial.id.isEmpty()) R.string.cal_new_event else R.string.cal_edit_event), style = MaterialTheme.typography.titleLarge, color = colors.text)
            AppTextField(event.title, { event = event.copy(title = it) }, stringResource(R.string.event_title_hint), Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.cal_all_day), style = MaterialTheme.typography.bodyLarge, color = colors.text, modifier = Modifier.weight(1f))
                Switch(event.allDay, { event = event.copy(allDay = it) }, colors = SwitchDefaults.colors(checkedTrackColor = colors.primary, checkedThumbColor = colors.onPrimary))
            }
            PropertyRow(Icons.Rounded.Event, stringResource(R.string.event_start), formatDay(start.toLocalDate()), { dialog = "startDate" },
                trailing = { if (!event.allDay) TextButton(onClick = { dialog = "startTime" }) { Text(formatTime(start.hour * 60 + start.minute)) } })
            PropertyRow(Icons.Rounded.Schedule, stringResource(R.string.event_end), formatDay(end.toLocalDate()), { dialog = "endDate" },
                trailing = { if (!event.allDay) TextButton(onClick = { dialog = "endTime" }) { Text(formatTime(end.hour * 60 + end.minute)) } })
            PropertyRow(Icons.Rounded.Alarm, stringResource(R.string.task_reminder), reminderLabel(reminder), { dialog = "reminder" })
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                for (argb in ProjectPalette) {
                    val swatch = storedColor(argb, colors)
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(swatch)
                            .then(if (argb == event.color) Modifier.border(3.dp, colors.surface, CircleShape).border(5.dp, swatch, CircleShape) else Modifier)
                            .clickable(role = Role.RadioButton) { event = event.copy(color = argb) },
                    )
                }
            }
            AppTextField(event.notes, { event = event.copy(notes = it) }, stringResource(R.string.event_notes_hint), Modifier.fillMaxWidth(), singleLine = false)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (initial.id.isNotEmpty()) TextButton(onClick = onDelete) { Text(stringResource(R.string.event_delete), color = colors.danger) }
                Spacer(Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.action_save), {
                    if (reminder != EventReminder.NONE && Build.VERSION.SDK_INT >= 33 && !AlarmReminders.canNotify(context)) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    save()
                }, enabled = event.title.isNotBlank())
            }
        }
    }
    when (dialog) {
        "startDate" -> DatePickerModal(start.toLocalDate(), { withStart(date = it) }, { dialog = null })
        "startTime" -> TimePickerModal(start.hour * 60 + start.minute, { withStart(minutes = it) }, { dialog = null })
        "endDate" -> DatePickerModal(end.toLocalDate(), { d ->
            val t = d.atStartOfDay(zone).plusMinutes((end.hour * 60 + end.minute).toLong()).toInstant().toEpochMilli()
            event = event.copy(endAt = maxOf(t, event.startAt))
        }, { dialog = null })
        "endTime" -> TimePickerModal(end.hour * 60 + end.minute, { m ->
            val t = end.toLocalDate().atStartOfDay(zone).plusMinutes(m.toLong()).toInstant().toEpochMilli()
            event = event.copy(endAt = maxOf(t, event.startAt))
        }, { dialog = null })
        "reminder" -> ChoiceDialog(stringResource(R.string.task_reminder), EventReminder.entries.map { it to reminderLabel(it) }, reminder, { reminder = it }, { dialog = null })
    }
}

@Composable
private fun reminderLabel(r: EventReminder) = stringResource(
    when (r) {
        EventReminder.NONE -> R.string.reminder_none
        EventReminder.AT_TIME -> R.string.reminder_at_time
        EventReminder.MIN_10 -> R.string.reminder_10m
        EventReminder.HOUR_1 -> R.string.reminder_1h
        EventReminder.DAY_1 -> R.string.reminder_1d
    }
)
