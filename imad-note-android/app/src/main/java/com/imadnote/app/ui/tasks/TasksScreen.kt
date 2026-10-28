package com.imadnote.app.ui.tasks

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.imadnote.app.AppContainer
import com.imadnote.app.R
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.domain.TaskRules
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.EmptyState
import com.imadnote.app.ui.components.ErrorState
import com.imadnote.app.ui.components.Illustration
import com.imadnote.app.ui.components.LoadingState
import com.imadnote.app.ui.components.SecondaryButton
import com.imadnote.app.ui.components.SectionHeader
import com.imadnote.app.ui.components.SegmentedTabs
import com.imadnote.app.ui.components.TaskActions
import com.imadnote.app.ui.components.TaskRow
import com.imadnote.app.ui.components.contentWidth
import com.imadnote.app.ui.components.dayHeaderLabel
import com.imadnote.app.ui.components.rememberTaskToggle
import com.imadnote.app.ui.home.minuteTicker
import com.imadnote.app.ui.home.retrying
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId

enum class TaskTab { TODAY, WEEK, ALL }

data class TasksState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val tasks: List<TaskEntity> = emptyList(),
    val projectNames: Map<String, String> = emptyMap(),
    val subtaskCounts: Map<String, Pair<Int, Int>> = emptyMap(),
)

class TasksViewModel(c: AppContainer) : ViewModel() {
    private val retry = MutableStateFlow(0)
    val taskActions = TaskActions(c.tasks, viewModelScope)

    val state = retrying(retry, onError = { TasksState(loading = false, error = true) }) {
        combine(
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
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksState())

    fun retry() {
        retry.value++
    }
}

/** A titled group of tasks within a tab. [danger] marks the overdue group. */
data class TaskGroup(val key: String, val title: String?, val tasks: List<TaskEntity>, val danger: Boolean = false)

private val activeOrder = compareBy<TaskEntity>({ it.dueAt == null }, { it.dueAt }, { -it.priority }, { it.createdAt })

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(onOpenTask: (String) -> Unit, onNewTask: () -> Unit, onCalendar: () -> Unit) {
    val vm = appViewModel { c, _ -> TasksViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(TaskTab.TODAY) }
    val toggle = rememberTaskToggle(vm.taskActions)
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)

    val overdueTitle = stringResource(R.string.section_overdue)
    val completedTitle = stringResource(R.string.tasks_completed)
    val noDateTitle = stringResource(R.string.tasks_no_date)
    val groups: List<TaskGroup> = when (tab) {
        TaskTab.TODAY -> listOf(
            TaskGroup("overdue", overdueTitle, state.tasks.filter { TaskRules.isOverdue(it.timing, today, zone) }.sortedWith(activeOrder), danger = true),
            TaskGroup("today", null, state.tasks.filter { TaskRules.isDueToday(it.timing, today, zone) }.sortedWith(activeOrder)),
        )
        TaskTab.WEEK -> (0L..6L).map { offset ->
            val day = today.plusDays(offset)
            TaskGroup(
                "d$offset",
                dayHeaderLabel(day),
                state.tasks.filter { !it.isCompleted && TaskRules.dueDate(it.dueAt, zone) == day }.sortedWith(activeOrder),
            )
        }
        TaskTab.ALL -> listOf(
            TaskGroup("open", null, state.tasks.filter { !it.isCompleted && it.dueAt != null }.sortedWith(activeOrder)),
            TaskGroup("nodate", noDateTitle, state.tasks.filter { !it.isCompleted && it.dueAt == null }.sortedWith(activeOrder)),
            TaskGroup("done", completedTitle, state.tasks.filter { it.isCompleted }.sortedByDescending { it.completedAt }.take(50)),
        )
    }.filter { it.tasks.isNotEmpty() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_tasks)) },
                actions = { IconButton(onClick = onCalendar) { Icon(Icons.Outlined.CalendarMonth, stringResource(R.string.calendar)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        bottomBar = {
            SecondaryButton(
                text = stringResource(R.string.add_task),
                onClick = onNewTask,
                icon = Icons.Outlined.Add,
                modifier = Modifier.contentWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).contentWidth()) {
            SegmentedTabs(
                options = listOf(stringResource(R.string.section_today), stringResource(R.string.tasks_week), stringResource(R.string.tasks_all)),
                selected = tab.ordinal,
                onSelect = { tab = TaskTab.entries[it] },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
            when {
                state.loading -> LoadingState()
                state.error -> ErrorState(onRetry = vm::retry)
                groups.isEmpty() -> EmptyState(
                    Illustration.ALL_CLEAR,
                    stringResource(
                        when (tab) {
                            TaskTab.TODAY -> R.string.tasks_empty_today
                            TaskTab.WEEK -> R.string.tasks_empty_week
                            TaskTab.ALL -> R.string.tasks_empty_all
                        },
                    ),
                    stringResource(R.string.tasks_empty_body),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = Spacing.sm, end = Spacing.sm, bottom = Spacing.fabClearance),
                ) {
                    groups.forEach { group ->
                        if (group.title != null) {
                            item("h-" + group.key) {
                                SectionHeader(
                                    group.title,
                                    Modifier.padding(horizontal = Spacing.sm),
                                    count = group.tasks.size,
                                    color = if (group.danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        items(group.tasks, key = { group.key + it.id }) { t ->
                            TaskRow(
                                t,
                                onToggle = { toggle(t) },
                                onClick = { onOpenTask(t.id) },
                                projectName = t.projectId?.let { state.projectNames[it] },
                                subtasks = state.subtaskCounts[t.id],
                            )
                        }
                    }
                }
            }
        }
    }
}
