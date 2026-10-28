package com.imadnote.app.ui.notes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.imadnote.app.AppContainer
import com.imadnote.app.R
import com.imadnote.app.data.NoteWithDetails
import com.imadnote.app.data.NotesLayout
import com.imadnote.app.data.UserPreferences
import com.imadnote.app.ui.LocalAppContainer
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.EmptyState
import com.imadnote.app.ui.components.ErrorState
import com.imadnote.app.ui.components.Illustration
import com.imadnote.app.ui.components.LoadingState
import com.imadnote.app.ui.components.NoteCard
import com.imadnote.app.ui.components.NoteListRow
import com.imadnote.app.ui.components.SearchPill
import com.imadnote.app.ui.components.contentWidth
import com.imadnote.app.ui.home.retrying
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class NoteFilter { ALL, PINNED, CHECKLISTS, IMAGES }

/** null = loading; [error] = failed to load. */
data class NotesState(val notes: List<NoteWithDetails>? = null, val error: Boolean = false)

class NotesViewModel(c: AppContainer, archived: Boolean) : ViewModel() {
    private val retry = MutableStateFlow(0)
    val state = retrying(retry, onError = { NotesState(error = true) }) {
        (if (archived) c.notes.observeArchived() else c.notes.observeActive()).map { NotesState(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesState())
    val prefs = c.settings.preferences.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserPreferences())

    fun retry() {
        retry.value++
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(onOpenNote: (String) -> Unit, onNewNote: () -> Unit, onSearch: () -> Unit, onArchive: () -> Unit) {
    val vm = appViewModel { c, _ -> NotesViewModel(c, archived = false) }
    val state by vm.state.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(NoteFilter.ALL) }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_notes)) },
                actions = { IconButton(onClick = onArchive) { Icon(Icons.Outlined.Archive, stringResource(R.string.archive)) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        val all = state.notes
        val list = when (filter) {
            NoteFilter.ALL -> all
            NoteFilter.PINNED -> all?.filter { it.note.isPinned }
            NoteFilter.CHECKLISTS -> all?.filter { it.checklist.isNotEmpty() }
            NoteFilter.IMAGES -> all?.filter { it.attachments.isNotEmpty() }
        }
        NoteGrid(
            notes = list,
            error = state.error,
            onRetry = vm::retry,
            layout = prefs.notesLayout,
            modifier = Modifier.padding(padding),
            header = {
                Column {
                    SearchPill(onClick = onSearch, modifier = Modifier.padding(bottom = Spacing.sm))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        NoteFilter.entries.forEach { f ->
                            FilterChip(
                                selected = filter == f,
                                onClick = { filter = f },
                                label = {
                                    Text(
                                        stringResource(
                                            when (f) {
                                                NoteFilter.ALL -> R.string.filter_all
                                                NoteFilter.PINNED -> R.string.filter_pinned
                                                NoteFilter.CHECKLISTS -> R.string.filter_checklists
                                                NoteFilter.IMAGES -> R.string.filter_images
                                            },
                                        ),
                                    )
                                },
                            )
                        }
                    }
                }
            },
            empty = {
                EmptyState(
                    Illustration.PLANT,
                    stringResource(if (filter == NoteFilter.ALL) R.string.notes_empty_title else R.string.notes_filter_empty_title),
                    stringResource(R.string.notes_empty_body),
                    actionLabel = if (filter == NoteFilter.ALL) stringResource(R.string.add_note) else null,
                    onAction = onNewNote,
                )
            },
            onOpenNote = onOpenNote,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(onBack: () -> Unit, onOpenNote: (String) -> Unit) {
    val vm = appViewModel { c, _ -> NotesViewModel(c, archived = true) }
    val state by vm.state.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archive)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        NoteGrid(
            notes = state.notes,
            error = state.error,
            onRetry = vm::retry,
            layout = prefs.notesLayout,
            modifier = Modifier.padding(padding),
            header = null,
            empty = { EmptyState(Illustration.ARCHIVE, stringResource(R.string.archive_empty_title), stringResource(R.string.archive_empty_body)) },
            onOpenNote = onOpenNote,
        )
    }
}

@Composable
private fun NoteGrid(
    notes: List<NoteWithDetails>?,
    error: Boolean,
    onRetry: () -> Unit,
    layout: NotesLayout,
    modifier: Modifier,
    header: (@Composable () -> Unit)?,
    empty: @Composable () -> Unit,
    onOpenNote: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    LazyVerticalStaggeredGrid(
        columns = if (layout == NotesLayout.GRID) StaggeredGridCells.Adaptive(160.dp) else StaggeredGridCells.Fixed(1),
        modifier = modifier.fillMaxSize().contentWidth(),
        contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.xs, bottom = Spacing.fabClearance),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalItemSpacing = Spacing.sm,
    ) {
        if (header != null) item(key = "header", span = StaggeredGridItemSpan.FullLine) { header() }
        when {
            error -> item(key = "error", span = StaggeredGridItemSpan.FullLine) { ErrorState(onRetry) }
            notes == null -> item(key = "loading", span = StaggeredGridItemSpan.FullLine) { LoadingState(rows = 4) }
            notes.isEmpty() -> item(key = "empty", span = StaggeredGridItemSpan.FullLine) { empty() }
            else -> items(notes, key = { it.note.id }) { n ->
                if (layout == NotesLayout.LIST) {
                    NoteListRow(n, onClick = { onOpenNote(n.note.id) })
                } else {
                    NoteCard(n, fileFor = container.notes::fileFor, onClick = { onOpenNote(n.note.id) })
                }
            }
        }
    }
}
