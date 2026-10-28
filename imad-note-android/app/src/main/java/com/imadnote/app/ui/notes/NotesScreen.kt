package com.imadnote.app.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.imadnote.app.ui.LocalAppContainer
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.EmptyState
import com.imadnote.app.ui.components.NoteCard
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class NoteFilter { ALL, PINNED, CHECKLISTS, IMAGES }

class NotesViewModel(c: AppContainer, archived: Boolean) : ViewModel() {
    val notes = (if (archived) c.notes.observeArchived() else c.notes.observeActive())
        .map<List<NoteWithDetails>, List<NoteWithDetails>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(onOpenNote: (String) -> Unit, onSearch: () -> Unit, onArchive: () -> Unit) {
    val vm = appViewModel { c, _ -> NotesViewModel(c, archived = false) }
    val notes by vm.notes.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(NoteFilter.ALL) }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_notes)) },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, stringResource(R.string.action_search)) }
                    IconButton(onClick = onArchive) { Icon(Icons.Outlined.Archive, stringResource(R.string.archive)) }
                },
            )
        },
    ) { padding ->
        val all = notes
        val list = when (filter) {
            NoteFilter.ALL -> all
            NoteFilter.PINNED -> all?.filter { it.note.isPinned }
            NoteFilter.CHECKLISTS -> all?.filter { it.checklist.isNotEmpty() }
            NoteFilter.IMAGES -> all?.filter { it.attachments.isNotEmpty() }
        }
        NoteGrid(
            notes = list,
            modifier = Modifier.padding(padding),
            header = {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
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
            },
            empty = {
                EmptyState(
                    Icons.Outlined.Lightbulb,
                    stringResource(if (filter == NoteFilter.ALL) R.string.notes_empty_title else R.string.notes_filter_empty_title),
                    stringResource(R.string.notes_empty_body),
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
    val notes by vm.notes.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.archive)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        NoteGrid(
            notes = notes,
            modifier = Modifier.padding(padding),
            header = null,
            empty = {
                EmptyState(Icons.Outlined.Inventory2, stringResource(R.string.archive_empty_title), stringResource(R.string.archive_empty_body))
            },
            onOpenNote = onOpenNote,
        )
    }
}

@Composable
private fun NoteGrid(
    notes: List<NoteWithDetails>?,
    modifier: Modifier,
    header: (@Composable () -> Unit)?,
    empty: @Composable () -> Unit,
    onOpenNote: (String) -> Unit,
) {
    val container = LocalAppContainer.current
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(160.dp),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Spacing.md, end = Spacing.md, top = Spacing.sm, bottom = Spacing.fabClearance),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalItemSpacing = Spacing.sm,
    ) {
        if (header != null) {
            item(key = "header", span = StaggeredGridItemSpan.FullLine) { header() }
        }
        if (notes == null) return@LazyVerticalStaggeredGrid
        if (notes.isEmpty()) {
            item(key = "empty", span = StaggeredGridItemSpan.FullLine) { empty() }
        }
        items(notes, key = { it.note.id }) { n ->
            NoteCard(n, fileFor = container.notes::fileFor, onClick = { onOpenNote(n.note.id) })
        }
    }
}
