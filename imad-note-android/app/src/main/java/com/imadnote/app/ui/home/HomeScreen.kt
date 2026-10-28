package com.imadnote.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.imadnote.app.AppContainer
import com.imadnote.app.R
import com.imadnote.app.data.HomeSection
import com.imadnote.app.data.NoteWithDetails
import com.imadnote.app.data.ProjectWithStats
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.domain.TaskRules
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.EmptyState
import com.imadnote.app.ui.components.LoadingState
import com.imadnote.app.ui.components.NoteCard
import com.imadnote.app.ui.components.ProjectCard
import com.imadnote.app.ui.components.SectionHeader
import com.imadnote.app.ui.components.TaskRow
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HomeState(
    val loading: Boolean = true,
    val overdue: List<TaskEntity> = emptyList(),
    val today: List<TaskEntity> = emptyList(),
    val upcoming: List<TaskEntity> = emptyList(),
    val pinned: List<NoteWithDetails> = emptyList(),
    val recent: List<NoteWithDetails> = emptyList(),
    val projects: List<ProjectWithStats> = emptyList(),
    val hidden: Set<HomeSection> = emptySet(),
)

/** Emits every minute so date buckets (today/overdue) roll over without reopening the app. */
internal fun minuteTicker() = flow {
    while (true) {
        emit(Unit)
        delay(60_000)
    }
}

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    val state = combine(
        c.tasks.observeTopLevel(),
        c.notes.observeActive(),
        c.projects.observeActive(),
        c.settings.preferences,
        minuteTicker(),
    ) { tasks, notes, projects, prefs, _ ->
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        HomeState(
            loading = false,
            overdue = tasks.filter { TaskRules.isOverdue(it.timing, today, zone) }.sortedBy { it.dueAt },
            today = tasks.filter { TaskRules.isDueToday(it.timing, today, zone) }
                .sortedWith(compareBy<TaskEntity>({ !it.hasTime }, { it.dueAt }, { -it.priority })),
            upcoming = tasks.filter { TaskRules.isUpcoming(it.timing, today, zone, withinDays = 7) }
                .sortedBy { it.dueAt }.take(5),
            pinned = notes.filter { it.note.isPinned }.take(6),
            recent = notes.filter { !it.note.isPinned }.take(6),
            projects = projects.take(5),
            hidden = prefs.hiddenSections,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun toggle(task: TaskEntity) {
        viewModelScope.launch { c.tasks.toggleComplete(task) }
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
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    val vm = appViewModel { c, _ -> HomeViewModel(c) }
    val state by vm.state.collectAsStateWithLifecycle()
    val container = com.imadnote.app.ui.LocalAppContainer.current
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, stringResource(R.string.action_search)) }
                    IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, stringResource(R.string.settings)) }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        if (state.loading) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val isEmpty = state.overdue.isEmpty() && state.today.isEmpty() && state.upcoming.isEmpty() &&
            state.pinned.isEmpty() && state.recent.isEmpty() && state.projects.isEmpty()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.fabClearance),
        ) {
            if (isEmpty) {
                item {
                    EmptyState(
                        Icons.Outlined.WbSunny,
                        stringResource(R.string.home_empty_title),
                        stringResource(R.string.home_empty_body),
                    )
                }
                return@LazyColumn
            }
            fun visible(s: HomeSection) = s !in state.hidden
            if (visible(HomeSection.OVERDUE) && state.overdue.isNotEmpty()) {
                item("h-overdue") { SectionHeader(stringResource(R.string.section_overdue), count = state.overdue.size) }
                items(state.overdue, key = { "o-" + it.id }) { t ->
                    TaskRow(t, onToggle = { vm.toggle(t) }, onClick = { onOpenTask(t.id) })
                }
            }
            if (visible(HomeSection.TODAY)) {
                item("h-today") {
                    SectionHeader(stringResource(R.string.section_today), count = state.today.size) {
                        TextButton(onClick = onSeeTasks) { Text(stringResource(R.string.action_see_all)) }
                    }
                }
                if (state.today.isEmpty()) {
                    item("today-empty") {
                        Text(
                            stringResource(R.string.today_clear),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = Spacing.sm),
                        )
                    }
                }
                items(state.today, key = { "t-" + it.id }) { t ->
                    TaskRow(t, onToggle = { vm.toggle(t) }, onClick = { onOpenTask(t.id) })
                }
            }
            if (visible(HomeSection.UPCOMING) && state.upcoming.isNotEmpty()) {
                item("h-up") { SectionHeader(stringResource(R.string.section_upcoming)) }
                items(state.upcoming, key = { "u-" + it.id }) { t ->
                    TaskRow(t, onToggle = { vm.toggle(t) }, onClick = { onOpenTask(t.id) })
                }
            }
            if (visible(HomeSection.PINNED) && state.pinned.isNotEmpty()) {
                item("h-pinned") { SectionHeader(stringResource(R.string.section_pinned)) }
                item("pinned-row") { NoteRow(state.pinned, container, onOpenNote) }
            }
            if (visible(HomeSection.RECENT) && state.recent.isNotEmpty()) {
                item("h-recent") {
                    SectionHeader(stringResource(R.string.section_recent_notes)) {
                        TextButton(onClick = onSeeNotes) { Text(stringResource(R.string.action_see_all)) }
                    }
                }
                item("recent-row") { NoteRow(state.recent, container, onOpenNote) }
            }
            if (visible(HomeSection.PROJECTS) && state.projects.isNotEmpty()) {
                item("h-projects") {
                    SectionHeader(stringResource(R.string.section_active_projects)) {
                        TextButton(onClick = onSeeProjects) { Text(stringResource(R.string.action_see_all)) }
                    }
                }
                items(state.projects, key = { "p-" + it.project.id }) { p ->
                    ProjectCard(p, onClick = { onOpenProject(p.project.id) }, modifier = Modifier.padding(bottom = Spacing.sm))
                }
            }
        }
    }
}

@Composable
private fun NoteRow(notes: List<NoteWithDetails>, container: AppContainer, onOpenNote: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        items(notes, key = { it.note.id }) { n ->
            NoteCard(
                n,
                fileFor = container.notes::fileFor,
                onClick = { onOpenNote(n.note.id) },
                modifier = Modifier.width(180.dp),
            )
        }
    }
}
