package com.imadnote.app.ui.calendar

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarViewMonth
import androidx.compose.material.icons.outlined.CalendarViewWeek
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imadnote.app.R
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.domain.TaskRules
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.EmptyState
import com.imadnote.app.ui.components.ErrorState
import com.imadnote.app.ui.components.Illustration
import com.imadnote.app.ui.components.ImadCard
import com.imadnote.app.ui.components.LoadingState
import com.imadnote.app.ui.components.QuietButton
import com.imadnote.app.ui.components.TaskCheck
import com.imadnote.app.ui.components.contentWidth
import com.imadnote.app.ui.components.dayHeaderLabel
import com.imadnote.app.ui.components.priorityColor
import com.imadnote.app.ui.components.projectColor
import com.imadnote.app.ui.components.rememberTaskToggle
import com.imadnote.app.ui.components.timeLabel
import com.imadnote.app.ui.tasks.TasksViewModel
import com.imadnote.app.ui.theme.Radius
import com.imadnote.app.ui.theme.Spacing
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(onBack: () -> Unit, onOpenTask: (String) -> Unit, onNewTask: (date: String) -> Unit) {
    val vm = appViewModel { c, _ -> TasksViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val toggle = rememberTaskToggle(vm.taskActions)
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val locale = LocalConfiguration.current.locales[0]
    var selectedIso by rememberSaveable { mutableStateOf(today.toString()) }
    var monthView by rememberSaveable { mutableStateOf(false) }
    val selected = LocalDate.parse(selectedIso)
    val firstDow = TaskRules.firstDayOfWeek(locale)
    val weekStart = selected.minusDays(((selected.dayOfWeek.value - firstDow.value + 7) % 7).toLong())
    val byDate = state.tasks.filter { it.dueAt != null }.groupBy { TaskRules.dueDate(it.dueAt, zone)!! }
    val dayTasks = byDate[selected].orEmpty().sortedWith(compareBy({ it.isCompleted }, { !it.hasTime }, { it.dueAt }))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(selected)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) } },
                actions = {
                    IconButton(onClick = { monthView = !monthView }) {
                        Icon(
                            if (monthView) Icons.Outlined.CalendarViewWeek else Icons.Outlined.CalendarViewMonth,
                            stringResource(if (monthView) R.string.calendar_week_view else R.string.calendar_month_view),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            state.error -> ErrorState(onRetry = vm::retry, modifier = Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding).contentWidth(),
                contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xxl),
            ) {
                item("picker") {
                    Column(Modifier.animateContentSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedIso = (if (monthView) selected.minusMonths(1) else selected.minusWeeks(1)).toString() }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(if (monthView) R.string.cd_previous_month else R.string.cd_previous_week))
                            }
                            Spacer(Modifier.weight(1f))
                            QuietButton(stringResource(R.string.date_today), onClick = { selectedIso = today.toString() })
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { selectedIso = (if (monthView) selected.plusMonths(1) else selected.plusWeeks(1)).toString() }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(if (monthView) R.string.cd_next_month else R.string.cd_next_week))
                            }
                        }
                        Row {
                            (0 until 7).forEach { i ->
                                Text(
                                    firstDow.plus(i.toLong()).getDisplayName(TextStyle.SHORT, locale),
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                        Spacer(Modifier.height(Spacing.xs))
                        val rows: List<List<LocalDate?>> = if (monthView) {
                            val ym = YearMonth.from(selected)
                            val lead = (ym.atDay(1).dayOfWeek.value - firstDow.value + 7) % 7
                            val cells = List(lead) { null } + (1..ym.lengthOfMonth()).map { ym.atDay(it) }
                            cells.chunked(7).map { it + List(7 - it.size) { null } }
                        } else {
                            listOf((0L..6L).map { weekStart.plusDays(it) })
                        }
                        rows.forEach { week ->
                            Row {
                                week.forEach { date ->
                                    Box(Modifier.weight(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                                        if (date != null) {
                                            DayCell(
                                                date = date,
                                                isToday = date == today,
                                                isSelected = date == selected,
                                                hasOpen = byDate[date].orEmpty().any { !it.isCompleted },
                                                locale = locale,
                                                onClick = { selectedIso = date.toString() },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item("day-header") {
                    Row(Modifier.padding(top = Spacing.lg, bottom = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        Text(dayHeaderLabel(selected), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        QuietButton(stringResource(R.string.action_add), onClick = { onNewTask(selected.toString()) }, icon = Icons.Outlined.Add)
                    }
                }
                if (dayTasks.isEmpty()) {
                    item("empty") { EmptyState(Illustration.CALENDAR, stringResource(R.string.calendar_day_empty), stringResource(R.string.calendar_day_empty_body), compact = true) }
                }
                items(dayTasks, key = { it.id }) { t ->
                    AgendaCard(t, projectColorIndex = null, onToggle = { toggle(t) }, onClick = { onOpenTask(t.id) })
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, hasOpen: Boolean, locale: java.util.Locale, onClick: () -> Unit) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.secondaryContainer
        else -> Color.Transparent
    }
    val fg = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val label = DateTimeFormatter.ofPattern("EEEE d MMMM", locale).format(date)
    Column(
        modifier = Modifier
            .size(width = 44.dp, height = 52.dp)
            .clip(Radius.md)
            .background(bg)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics {
                contentDescription = label
                selected = isSelected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(date.dayOfMonth.toString(), style = MaterialTheme.typography.titleSmall, fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal, color = fg)
        Spacer(Modifier.height(3.dp))
        Box(
            Modifier.size(5.dp).clip(Radius.full).background(
                when {
                    !hasOpen -> Color.Transparent
                    isSelected -> fg
                    else -> MaterialTheme.colorScheme.secondary
                },
            ),
        )
    }
}

/** Agenda entry: a coloured bar on the start edge (project or priority), title and time. */
@Composable
private fun AgendaCard(task: TaskEntity, projectColorIndex: Int?, onToggle: () -> Unit, onClick: () -> Unit) {
    val bar = projectColorIndex?.let { projectColor(it) } ?: priorityColor(task.priority).let { if (task.priority == 0) MaterialTheme.colorScheme.secondary else it }
    ImadCard(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(bottom = Spacing.sm)) {
        Row(Modifier.height(IntrinsicSize.Min).heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(bar))
            Column(Modifier.weight(1f).padding(horizontal = Spacing.md, vertical = Spacing.md)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    if (task.hasTime && task.dueAt != null) timeLabel(task.dueAt) else stringResource(R.string.all_day),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TaskCheck(task.isCompleted, task.priority, taskTitle = task.title, onToggle = onToggle)
        }
    }
}
