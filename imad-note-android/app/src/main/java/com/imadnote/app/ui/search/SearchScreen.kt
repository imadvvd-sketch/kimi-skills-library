package com.imadnote.app.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.imadnote.app.AppContainer
import com.imadnote.app.R
import com.imadnote.app.data.NoteWithDetails
import com.imadnote.app.data.ProjectWithStats
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.ui.LocalAppContainer
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.EmptyState
import com.imadnote.app.ui.components.Illustration
import com.imadnote.app.ui.components.TaskActions
import com.imadnote.app.ui.components.contentWidth
import com.imadnote.app.ui.components.rememberTaskToggle
import com.imadnote.app.ui.components.NoteCard
import com.imadnote.app.ui.components.ProjectCard
import com.imadnote.app.ui.components.SectionHeader
import com.imadnote.app.ui.components.TaskRow
import com.imadnote.app.ui.tasks.transparentFieldColors
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

enum class SearchScope { ALL, NOTES, TASKS, PROJECTS }

data class SearchResults(
    val query: String = "",
    val notes: List<NoteWithDetails> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val projects: List<ProjectWithStats> = emptyList(),
) {
    val isEmpty get() = notes.isEmpty() && tasks.isEmpty() && projects.isEmpty()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(private val c: AppContainer, private val handle: SavedStateHandle) : ViewModel() {
    val query = handle.getStateFlow("q", "")
    val scope = handle.getStateFlow("scope", SearchScope.ALL.name)

    val results = query.debounce(200).flatMapLatest { raw ->
        val q = raw.trim()
        if (q.isEmpty()) {
            flowOf(SearchResults())
        } else {
            combine(c.notes.search(q), c.tasks.search(q), c.projects.search(q)) { n, t, p -> SearchResults(q, n, t, p) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    fun setQuery(q: String) { handle["q"] = q }
    fun setScope(s: SearchScope) { handle["scope"] = s.name }
    val taskActions = TaskActions(c.tasks, viewModelScope)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenProject: (String) -> Unit,
) {
    val vm = appViewModel { c, h -> SearchViewModel(c, h) }
    val query by vm.query.collectAsStateWithLifecycle()
    val scopeName by vm.scope.collectAsStateWithLifecycle()
    val scope = SearchScope.valueOf(scopeName)
    val results by vm.results.collectAsStateWithLifecycle()
    val container = LocalAppContainer.current
    val toggle = rememberTaskToggle(vm.taskActions)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
                title = {
                    TextField(
                        value = query,
                        onValueChange = vm::setQuery,
                        placeholder = { Text(stringResource(R.string.search_hint)) },
                        singleLine = true,
                        colors = transparentFieldColors(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                },
                actions = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Filled.Close, stringResource(R.string.action_clear)) }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding().contentWidth(),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xxl),
        ) {
            item("scopes") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SearchScope.entries.forEach { s ->
                        val count = when (s) {
                            SearchScope.ALL -> null
                            SearchScope.NOTES -> results.notes.size
                            SearchScope.TASKS -> results.tasks.size
                            SearchScope.PROJECTS -> results.projects.size
                        }
                        FilterChip(
                            selected = scope == s,
                            onClick = { vm.setScope(s) },
                            label = {
                                val label = stringResource(
                                    when (s) {
                                        SearchScope.ALL -> R.string.filter_all
                                        SearchScope.NOTES -> R.string.tab_notes
                                        SearchScope.TASKS -> R.string.tab_tasks
                                        SearchScope.PROJECTS -> R.string.tab_projects
                                    },
                                )
                                Text(if (count != null && results.query.isNotEmpty()) "$label $count" else label)
                            },
                        )
                    }
                }
            }
            if (query.isBlank()) {
                item("hint") { EmptyState(Illustration.SEARCH, stringResource(R.string.search_start_title), stringResource(R.string.search_start_body)) }
                return@LazyColumn
            }
            if (results.query.isNotEmpty() && results.isEmpty) {
                item("none") { EmptyState(Illustration.SEARCH, stringResource(R.string.search_none_title), stringResource(R.string.search_none_body, results.query)) }
            }
            val showNotes = scope == SearchScope.ALL || scope == SearchScope.NOTES
            val showTasks = scope == SearchScope.ALL || scope == SearchScope.TASKS
            val showProjects = scope == SearchScope.ALL || scope == SearchScope.PROJECTS
            if (showTasks && results.tasks.isNotEmpty()) {
                item("th") { SectionHeader(stringResource(R.string.tab_tasks), count = results.tasks.size) }
                items(results.tasks, key = { "t" + it.id }) { t ->
                    TaskRow(t, onToggle = { toggle(t) }, onClick = { onOpenTask(t.id) })
                }
            }
            if (showNotes && results.notes.isNotEmpty()) {
                item("nh") { SectionHeader(stringResource(R.string.tab_notes), count = results.notes.size) }
                items(results.notes, key = { "n" + it.note.id }) { n ->
                    NoteCard(n, fileFor = container.notes::fileFor, onClick = { onOpenNote(n.note.id) }, modifier = Modifier.padding(bottom = Spacing.sm))
                }
            }
            if (showProjects && results.projects.isNotEmpty()) {
                item("ph") { SectionHeader(stringResource(R.string.tab_projects), count = results.projects.size) }
                items(results.projects, key = { "p" + it.project.id }) { p ->
                    ProjectCard(p, onClick = { onOpenProject(p.project.id) }, modifier = Modifier.padding(bottom = Spacing.sm))
                }
            }
        }
    }
}

