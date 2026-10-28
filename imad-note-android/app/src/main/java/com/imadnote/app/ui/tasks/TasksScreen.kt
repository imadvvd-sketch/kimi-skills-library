package com.imadnote.app.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.imadnote.app.AppContainer
import com.imadnote.app.R
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.domain.TaskRules
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.EmptyState
import com.imadnote.app.ui.components.LoadingState
import com.imadnote.app.ui.components.TaskRow
import com.imadnote.app.ui.home.minuteTicker
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

enum class TaskTab { TODAY, UPCOMING, OVERDUE, ALL, COMPLETED, CALENDAR }

data class TasksState(
    val loading: Boolean = true,
    val tasks: List<TaskEntity> = emptyList(),
    val projectNames: Map<String, String> = emptyMap(),
    val subtaskCounts: Map<String, Pair<Int, Int>> = emptyMap(),
)

class TasksViewModel(private val c: AppContainer) : ViewModel() {
    val state = combine(
        c.tasks.observeTopLevel(),
        c.projects.observeList(),
        c.tasks.observeSubtaskCounts(),
        minuteTicker(),
    ) { tasks, projects, counts, _ ->
        TasksState(
            loading = false,
            tasks = tasks,
            projectNames = projects.associate { it.id to it.name },
            subtaskCounts = counts.associate { it.parentId to (it.done to it.total) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksState())

    fun toggle(task: TaskEntity) {
        viewModelScope.launch { c.tasks.toggleComplete(task) }
    }
}

private val activeOrder = compareBy<TaskEntity>({ it.dueAt == null }, { it.dueAt }, { -it.priority }, { it.createdAt })

fun filterTasks(tasks: List<TaskEntity>, tab: TaskTab, today: LocalDate, zone: ZoneId): List<TaskEntity> = when (tab) {
    TaskTab.TODAY -> tasks.filter { TaskRules.isDueToday(it.timing, today, zone) }.sortedWith(activeOrder)
    TaskTab.UPCOMING -> tasks.filter { TaskRules.isUpcoming(it.timing, today, zone) }.sortedWith(activeOrder)
    TaskTab.OVERDUE -> tasks.filter { TaskRules.isOverdue(it.timing, today, zone) }.sortedWith(activeOrder)
    TaskTab.ALL -> tasks.filter { !it.isCompleted }.sortedWith(activeOrder)
    TaskTab.COMPLETED -> tasks.filter { it.isCompleted }.sortedByDescending { it.completedAt }
    TaskTab.CALENDAR -> tasks
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(onOpenTask: (String) -> Unit, onSearch: () -> Unit) {
    val vm = appViewModel { c, _ -> TasksViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(TaskTab.TODAY) }
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(stringResource(R.string.tab_tasks)) },
                    actions = { IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, stringResource(R.string.action_search)) } },
                )
                ScrollableTabRow(selectedTabIndex = tab.ordinal, edgePadding = Spacing.lg) {
                    TaskTab.entries.forEach { t ->
                        val count = if (t == TaskTab.CALENDAR || t == TaskTab.COMPLETED || state.loading) null
                        else filterTasks(state.tasks, t, today, zone).size
                        Tab(
                            selected = tab == t,
                            onClick = { tab = t },
                            text = { Text(tabLabel(t) + (count?.takeIf { it > 0 }?.let { " $it" } ?: "")) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        if (tab == TaskTab.CALENDAR) {
            CalendarTasks(state, vm, onOpenTask, Modifier.padding(padding))
            return@Scaffold
        }
        val list = filterTasks(state.tasks, tab, today, zone)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spacing.sm, end = Spacing.sm, top = Spacing.sm, bottom = Spacing.fabClearance),
        ) {
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        if (tab == TaskTab.COMPLETED) Icons.Outlined.CheckCircle else Icons.Outlined.TaskAlt,
                        stringResource(emptyTitle(tab)),
                        stringResource(R.string.tasks_empty_body),
                    )
                }
            }
            items(list, key = { it.id }) { t ->
                TaskRow(
                    t,
                    onToggle = { vm.toggle(t) },
                    onClick = { onOpenTask(t.id) },
                    projectName = t.projectId?.let { state.projectNames[it] },
                    subtasks = state.subtaskCounts[t.id],
                )
            }
        }
    }
}

@Composable
private fun tabLabel(t: TaskTab): String = stringResource(
    when (t) {
        TaskTab.TODAY -> R.string.section_today
        TaskTab.UPCOMING -> R.string.section_upcoming
        TaskTab.OVERDUE -> R.string.section_overdue
        TaskTab.ALL -> R.string.tasks_all
        TaskTab.COMPLETED -> R.string.tasks_completed
        TaskTab.CALENDAR -> R.string.tasks_calendar
    },
)

private fun emptyTitle(t: TaskTab): Int = when (t) {
    TaskTab.TODAY -> R.string.tasks_empty_today
    TaskTab.UPCOMING -> R.string.tasks_empty_upcoming
    TaskTab.OVERDUE -> R.string.tasks_empty_overdue
    TaskTab.COMPLETED -> R.string.tasks_empty_completed
    else -> R.string.tasks_empty_all
}

@Composable
private fun CalendarTasks(state: TasksState, vm: TasksViewModel, onOpenTask: (String) -> Unit, modifier: Modifier) {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    var month by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    var selected by rememberSaveable { mutableStateOf(today.toString()) }
    val ym = YearMonth.parse(month)
    val selectedDate = LocalDate.parse(selected)
    val locale = LocalConfiguration.current.locales[0]
    val byDate = state.tasks.filter { it.dueAt != null }.groupBy { TaskRules.dueDate(it.dueAt, zone)!! }
    val dayTasks = byDate[selectedDate].orEmpty().sortedWith(compareBy({ it.isCompleted }, { it.dueAt }))

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = Spacing.sm, end = Spacing.sm, bottom = Spacing.fabClearance)) {
        item("month") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.sm)) {
                IconButton(onClick = { month = ym.minusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.cd_previous_month))
                }
                Text(
                    DateTimeFormatter.ofPattern("LLLL yyyy", locale).format(ym),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
                IconButton(onClick = { month = ym.plusMonths(1).toString() }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.cd_next_month))
                }
            }
        }
        item("grid") {
            val firstDow = TaskRules.firstDayOfWeek(locale)
            val days = (0 until 7).map { firstDow.plus(it.toLong()) }
            Column(Modifier.padding(horizontal = Spacing.sm)) {
                Row {
                    days.forEach { d ->
                        Text(
                            d.getDisplayName(TextStyle.NARROW, locale),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val first = ym.atDay(1)
                val lead = ((first.dayOfWeek.value - firstDow.value) + 7) % 7
                val cells = lead + ym.lengthOfMonth()
                val rows = (cells + 6) / 7
                for (r in 0 until rows) {
                    Row {
                        for (col in 0 until 7) {
                            val dayNum = r * 7 + col - lead + 1
                            Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                                if (dayNum in 1..ym.lengthOfMonth()) {
                                    val date = ym.atDay(dayNum)
                                    DayCell(
                                        date = date,
                                        isToday = date == today,
                                        isSelected = date == selectedDate,
                                        hasOpen = byDate[date].orEmpty().any { !it.isCompleted },
                                        onClick = { selected = date.toString() },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item("day-header") {
            Text(
                com.imadnote.app.ui.components.dateLabel(selectedDate.atStartOfDay(zone).toInstant().toEpochMilli()),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.md, top = Spacing.lg, bottom = Spacing.sm),
            )
        }
        if (dayTasks.isEmpty()) {
            item("day-empty") {
                EmptyState(Icons.Outlined.EventAvailable, stringResource(R.string.calendar_day_empty), stringResource(R.string.calendar_day_empty_body))
            }
        }
        items(dayTasks, key = { it.id }) { t ->
            TaskRow(t, onToggle = { vm.toggle(t) }, onClick = { onOpenTask(t.id) }, projectName = t.projectId?.let { state.projectNames[it] })
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, hasOpen: Boolean, onClick: () -> Unit) {
    val bg = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val fg = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Column(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(bg).clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = fg,
        )
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier.size(4.dp).clip(CircleShape).background(
                if (hasOpen) (if (isSelected) fg else MaterialTheme.colorScheme.secondary) else androidx.compose.ui.graphics.Color.Transparent,
            ),
        )
    }
}

