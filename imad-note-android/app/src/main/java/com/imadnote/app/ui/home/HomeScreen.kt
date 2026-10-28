package com.imadnote.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.imadnote.app.AppContainer
import com.imadnote.app.R
import com.imadnote.app.data.HomeSection
import com.imadnote.app.data.NoteWithDetails
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.domain.TaskRules
import com.imadnote.app.ui.LocalAppContainer
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.EmptyState
import com.imadnote.app.ui.components.ErrorState
import com.imadnote.app.ui.components.ImadCard
import com.imadnote.app.ui.components.Illustration
import com.imadnote.app.ui.components.LoadingState
import com.imadnote.app.ui.components.NoteCard
import com.imadnote.app.ui.components.QuietButton
import com.imadnote.app.ui.components.SearchPill
import com.imadnote.app.ui.components.SectionHeader
import com.imadnote.app.ui.components.TaskActions
import com.imadnote.app.ui.components.TaskRow
import com.imadnote.app.ui.components.contentWidth
import com.imadnote.app.ui.components.relativeTime
import com.imadnote.app.ui.components.rememberTaskToggle
import com.imadnote.app.ui.theme.AppTheme
import com.imadnote.app.ui.theme.Radius
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** One line of "recent activity", derived from note and task timestamps. */
data class Activity(val key: String, val kind: Kind, val title: String, val at: Long, val targetId: String) {
    enum class Kind { NOTE_EDITED, TASK_ADDED, TASK_DONE }
}

data class HomeState(
    val loading: Boolean = true,
    val error: Boolean = false,
    val name: String = "",
    val overdue: List<TaskEntity> = emptyList(),
    val today: List<TaskEntity> = emptyList(),
    val todayDone: Int = 0,
    val todayTotal: Int = 0,
    val noteCount: Int = 0,
    val projectCount: Int = 0,
    val pinned: List<NoteWithDetails> = emptyList(),
    val activity: List<Activity> = emptyList(),
    val hidden: Set<HomeSection> = emptySet(),
)

/** Emits every minute so date buckets (today/overdue) and "x min ago" stay current. */
internal fun minuteTicker() = flow {
    while (true) {
        emit(Unit)
        delay(60_000)
    }
}

/** Re-subscribes [block] each time [trigger] changes; used for "Retry" on error states. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> retrying(trigger: MutableStateFlow<Int>, onError: (Throwable) -> T, block: () -> Flow<T>): Flow<T> =
    trigger.flatMapLatest { block().catch { emit(onError(it)) } }

internal fun buildActivity(notes: List<NoteWithDetails>, tasks: List<TaskEntity>, limit: Int = 6): List<Activity> {
    val noteItems = notes.map { Activity("n" + it.note.id, Activity.Kind.NOTE_EDITED, it.note.title, it.note.updatedAt, it.note.id) }
    val taskItems = tasks.map { t ->
        if (t.completedAt != null) Activity("t" + t.id, Activity.Kind.TASK_DONE, t.title, t.completedAt, t.id)
        else Activity("t" + t.id, Activity.Kind.TASK_ADDED, t.title, t.updatedAt, t.id)
    }
    return (noteItems + taskItems).sortedByDescending { it.at }.take(limit)
}

class HomeViewModel(c: AppContainer) : ViewModel() {
    private val retry = MutableStateFlow(0)
    val taskActions = TaskActions(c.tasks, viewModelScope)

    val state = retrying(retry, onError = { HomeState(loading = false, error = true) }) {
        combine(
            c.tasks.observeTopLevel(),
            c.notes.observeActive(),
            c.projects.observeActive(),
            c.settings.preferences,
            minuteTicker(),
        ) { tasks, notes, projects, prefs, _ ->
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now(zone)
            val dueToday = tasks.filter { TaskRules.dueDate(it.dueAt, zone) == today }
            HomeState(
                loading = false,
                name = prefs.displayName,
                overdue = tasks.filter { TaskRules.isOverdue(it.timing, today, zone) }.sortedBy { it.dueAt },
                today = dueToday.filter { !it.isCompleted }.sortedWith(compareBy<TaskEntity>({ !it.hasTime }, { it.dueAt }, { -it.priority })),
                todayDone = dueToday.count { it.isCompleted },
                todayTotal = dueToday.size,
                noteCount = notes.size,
                projectCount = projects.size,
                pinned = notes.filter { it.note.isPinned }.take(8),
                activity = buildActivity(notes, tasks),
                hidden = prefs.hiddenSections,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun retry() {
        retry.value++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTask: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenProject: (String) -> Unit,
    onSeeTasks: () -> Unit,
    onSeeNotes: () -> Unit,
    onSeeProjects: () -> Unit,
    onCalendar: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val vm = appViewModel { c, _ -> HomeViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val toggle = rememberTaskToggle(vm.taskActions)
    val container = LocalAppContainer.current
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_brand_mark), contentDescription = null, modifier = Modifier.size(28.dp).clip(Radius.sm))
                        Spacer(Modifier.width(Spacing.sm))
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    }
                },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, stringResource(R.string.action_search)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))
            state.error -> ErrorState(onRetry = vm::retry, modifier = Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).contentWidth(),
                contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.fabClearance),
            ) {
                item("greeting") { Greeting(state.name) }
                item("search") { SearchPill(onClick = onSearch, modifier = Modifier.padding(bottom = Spacing.lg)) }
                item("stats") {
                    StatsGrid(
                        state = state,
                        onTasks = onSeeTasks,
                        onNotes = onSeeNotes,
                        onProjects = onSeeProjects,
                        onCalendar = onCalendar,
                    )
                }
                fun visible(s: HomeSection) = s !in state.hidden
                if (visible(HomeSection.OVERDUE) && state.overdue.isNotEmpty()) {
                    item("h-overdue") { SectionHeader(stringResource(R.string.section_overdue), count = state.overdue.size, color = MaterialTheme.colorScheme.error) }
                    items(state.overdue, key = { "o-" + it.id }) { t -> TaskRow(t, onToggle = { toggle(t) }, onClick = { onOpenTask(t.id) }) }
                }
                if (visible(HomeSection.TODAY) && state.today.isNotEmpty()) {
                    item("h-today") {
                        SectionHeader(stringResource(R.string.section_today), count = state.today.size) {
                            QuietButton(stringResource(R.string.action_see_all), onSeeTasks)
                        }
                    }
                    items(state.today, key = { "t-" + it.id }) { t -> TaskRow(t, onToggle = { toggle(t) }, onClick = { onOpenTask(t.id) }) }
                }
                if (visible(HomeSection.PINNED) && state.pinned.isNotEmpty()) {
                    item("h-pinned") { SectionHeader(stringResource(R.string.section_pinned)) }
                    item("pinned-row") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            items(state.pinned, key = { it.note.id }) { n ->
                                NoteCard(n, fileFor = container.notes::fileFor, onClick = { onOpenNote(n.note.id) }, modifier = Modifier.width(176.dp), maxPreviewLines = 4)
                            }
                        }
                    }
                }
                if (visible(HomeSection.ACTIVITY)) {
                    item("h-activity") { SectionHeader(stringResource(R.string.section_activity)) }
                    if (state.activity.isEmpty()) {
                        item("empty") {
                            EmptyState(Illustration.ALL_CLEAR, stringResource(R.string.home_empty_title), stringResource(R.string.home_empty_body), compact = true)
                        }
                    } else {
                        item("activity") {
                            ImadCard {
                                Column {
                                    state.activity.forEachIndexed { i, a ->
                                        ActivityRow(a, onClick = {
                                            if (a.kind == Activity.Kind.NOTE_EDITED) onOpenNote(a.targetId) else onOpenTask(a.targetId)
                                        })
                                        if (i < state.activity.lastIndex) {
                                            androidx.compose.material3.HorizontalDivider(
                                                Modifier.padding(start = 64.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Greeting(name: String) {
    val hour = LocalTime.now().hour
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> R.string.greeting_morning
            in 12..17 -> R.string.greeting_afternoon
            else -> R.string.greeting_evening
        },
    )
    val title = if (name.isBlank()) greeting else stringResource(R.string.greeting_with_name, greeting, name)
    Column(Modifier.padding(top = Spacing.xs, bottom = Spacing.lg)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.greeting_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatsGrid(state: HomeState, onTasks: () -> Unit, onNotes: () -> Unit, onProjects: () -> Unit, onCalendar: () -> Unit) {
    val ext = AppTheme.extended
    val locale = LocalConfiguration.current.locales[0]
    val dateText = DateTimeFormatter.ofPattern(if (locale.language == "ar") "EEEE d MMMM" else "EEE, d MMM", locale).format(LocalDate.now())
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.padding(bottom = Spacing.sm)) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatCard(Icons.Outlined.TaskAlt, ext.accent, stringResource(R.string.stat_today_tasks), "${state.todayDone}/${state.todayTotal}", Modifier.weight(1f), onTasks)
            StatCard(Icons.AutoMirrored.Outlined.StickyNote2, ext.success, stringResource(R.string.tab_notes), state.noteCount.toString(), Modifier.weight(1f), onNotes)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            StatCard(Icons.Outlined.Folder, MaterialTheme.colorScheme.secondary, stringResource(R.string.tab_projects), state.projectCount.toString(), Modifier.weight(1f), onProjects)
            StatCard(Icons.Outlined.CalendarMonth, MaterialTheme.colorScheme.primary, stringResource(R.string.calendar), dateText, Modifier.weight(1f), onCalendar, smallValue = true)
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    tint: Color,
    label: String,
    value: String,
    modifier: Modifier,
    onClick: () -> Unit,
    smallValue: Boolean = false,
) {
    ImadCard(onClick = onClick, modifier = modifier.heightIn(min = 96.dp)) {
        Column(
            Modifier
                .padding(Spacing.md)
                .clearAndSetSemantics {
                    contentDescription = "$label: $value"
                    role = Role.Button
                },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = Radius.sm, color = tint.copy(alpha = 0.14f), modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) }
                }
                Spacer(Modifier.width(Spacing.sm))
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(Spacing.md))
            Text(
                value,
                style = if (smallValue) MaterialTheme.typography.titleSmall else MaterialTheme.typography.headlineSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ActivityRow(a: Activity, onClick: () -> Unit) {
    val ext = AppTheme.extended
    val (icon, tint, action) = when (a.kind) {
        Activity.Kind.NOTE_EDITED -> Triple(Icons.Outlined.EditNote, MaterialTheme.colorScheme.secondary, stringResource(R.string.activity_note))
        Activity.Kind.TASK_ADDED -> Triple(Icons.Outlined.AddTask, ext.accent, stringResource(R.string.activity_task))
        Activity.Kind.TASK_DONE -> Triple(Icons.Outlined.CheckCircle, ext.success, stringResource(R.string.activity_task_done))
    }
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(horizontal = Spacing.md, vertical = Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = Radius.sm, color = tint.copy(alpha = 0.14f), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(a.title.ifBlank { stringResource(R.string.untitled_note) }, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$action · ${relativeTime(a.at)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}
