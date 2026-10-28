package com.imadnote.app.ui.projects

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.imadnote.app.AppContainer
import com.imadnote.app.R
import com.imadnote.app.data.ProjectEntity
import com.imadnote.app.data.ProjectWithStats
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.data.newId
import com.imadnote.app.ui.LocalAppContainer
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.AttachmentImage
import com.imadnote.app.ui.components.ColorSwatches
import com.imadnote.app.ui.components.ConfirmDialog
import com.imadnote.app.ui.components.DatePickerModal
import com.imadnote.app.ui.components.EmptyState
import com.imadnote.app.ui.components.InfoText
import com.imadnote.app.ui.components.LoadingState
import com.imadnote.app.ui.components.NoteCard
import com.imadnote.app.ui.components.ProjectCard
import com.imadnote.app.ui.components.ProjectProgress
import com.imadnote.app.ui.components.SectionHeader
import com.imadnote.app.ui.components.TaskRow
import com.imadnote.app.ui.components.dateLabel
import com.imadnote.app.ui.components.dueLabel
import com.imadnote.app.ui.theme.AppTheme
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// ---------- List ----------

class ProjectsViewModel(c: AppContainer) : ViewModel() {
    val active = c.projects.observeActive().map<List<ProjectWithStats>, List<ProjectWithStats>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val archived = c.projects.observeArchived()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(onOpenProject: (String) -> Unit, onNewProject: () -> Unit, onSearch: () -> Unit) {
    val vm = appViewModel { c, _ -> ProjectsViewModel(c) }
    val active by vm.active.collectAsStateWithLifecycle()
    val archived by vm.archived.collectAsStateWithLifecycle()
    var showArchived by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_projects)) },
                actions = {
                    IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, stringResource(R.string.action_search)) }
                    IconButton(onClick = onNewProject) { Icon(Icons.Outlined.CreateNewFolder, stringResource(R.string.new_project)) }
                },
            )
        },
    ) { padding ->
        val list = if (showArchived) archived else active
        if (list == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, top = Spacing.sm, bottom = Spacing.fabClearance),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            item("filters") {
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    FilterChip(selected = !showArchived, onClick = { showArchived = false }, label = { Text(stringResource(R.string.filter_active)) })
                    FilterChip(selected = showArchived, onClick = { showArchived = true }, label = { Text(stringResource(R.string.filter_archived)) })
                }
            }
            if (list.isEmpty()) {
                item("empty") {
                    Column {
                        EmptyState(
                            Icons.Outlined.CreateNewFolder,
                            stringResource(if (showArchived) R.string.projects_archived_empty else R.string.projects_empty_title),
                            stringResource(R.string.projects_empty_body),
                        )
                        if (!showArchived) {
                            TextButton(onClick = onNewProject, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.new_project))
                            }
                        }
                    }
                }
            }
            items(list, key = { it.project.id }) { p ->
                ProjectCard(p, onClick = { onOpenProject(p.project.id) })
            }
        }
    }
}

// ---------- Detail ----------

class ProjectDetailViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    val projectId: String = handle["id"] ?: ""
    val project = c.projects.observeOne(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val tasks = c.tasks.observeByProject(projectId)
        .map { list -> list.sortedWith(compareBy<TaskEntity>({ it.isCompleted }, { it.dueAt == null }, { it.dueAt }, { -it.priority })) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes = c.notes.observeByProject(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun toggle(task: TaskEntity) {
        viewModelScope.launch { c.tasks.toggleComplete(task) }
    }

    fun setArchived(archived: Boolean) {
        viewModelScope.launch { c.projects.setArchived(projectId, archived) }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            c.projects.delete(projectId)
            onDone()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    onOpenTask: (String) -> Unit,
    onOpenNote: (String) -> Unit,
    onNewTask: (projectId: String) -> Unit,
    onNewNote: (projectId: String) -> Unit,
    onOpenImage: (noteId: String, index: Int) -> Unit,
) {
    val vm = appViewModel { c, h -> ProjectDetailViewModel(c, h) }
    val project by vm.project.collectAsStateWithLifecycle()
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val container = LocalAppContainer.current
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.project?.name.orEmpty(), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    IconButton(onClick = { onEdit(vm.projectId) }) { Icon(Icons.Outlined.Edit, stringResource(R.string.action_edit)) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            val archived = project?.project?.isArchived == true
                            DropdownMenuItem(
                                text = { Text(stringResource(if (archived) R.string.action_unarchive else R.string.action_archive)) },
                                leadingIcon = { Icon(if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null) },
                                onClick = { menu = false; vm.setArchived(!archived) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                onClick = { menu = false; confirmDelete = true },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        val p = project
        if (p == null) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        val images = notes.flatMap { n -> n.sortedAttachments.mapIndexed { i, a -> Triple(n.note.id, i, a) } }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xxl),
        ) {
            item("header") {
                Column(Modifier.padding(vertical = Spacing.sm)) {
                    if (p.project.description.isNotBlank()) {
                        Text(p.project.description, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(Spacing.md))
                    }
                    p.project.deadline?.let {
                        InfoText(stringResource(R.string.deadline_label, dueLabel(it, false)))
                        Spacer(Modifier.height(Spacing.sm))
                    }
                    ProjectProgress(p)
                    if (p.project.isArchived) {
                        Spacer(Modifier.height(Spacing.sm))
                        InfoText(stringResource(R.string.project_archived_hint))
                    }
                }
            }
            item("tasks-h") {
                SectionHeader(stringResource(R.string.tab_tasks), count = tasks.size) {
                    TextButton(onClick = { onNewTask(vm.projectId) }) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.action_add))
                    }
                }
            }
            if (tasks.isEmpty()) item("tasks-empty") { InfoText(stringResource(R.string.project_no_tasks)) }
            items(tasks, key = { "t" + it.id }) { t ->
                TaskRow(t, onToggle = { vm.toggle(t) }, onClick = { onOpenTask(t.id) })
            }
            item("notes-h") {
                SectionHeader(stringResource(R.string.tab_notes), count = notes.size) {
                    TextButton(onClick = { onNewNote(vm.projectId) }) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.xs))
                        Text(stringResource(R.string.action_add))
                    }
                }
            }
            if (notes.isEmpty()) item("notes-empty") { InfoText(stringResource(R.string.project_no_notes)) }
            items(notes, key = { "n" + it.note.id }) { n ->
                NoteCard(n, fileFor = container.notes::fileFor, onClick = { onOpenNote(n.note.id) }, modifier = Modifier.padding(bottom = Spacing.sm))
            }
            if (images.isNotEmpty()) {
                item("img-h") { SectionHeader(stringResource(R.string.project_attachments), count = images.size) }
                item("img-row") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        items(images, key = { it.third.id }) { (noteId, index, a) ->
                            AttachmentImage(
                                container.notes.fileFor(a),
                                Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Image) { onOpenImage(noteId, index) },
                            )
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_project_title),
            text = stringResource(R.string.delete_project_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { vm.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}

// ---------- Edit ----------

data class ProjectForm(
    val loaded: Boolean = false,
    val exists: Boolean = false,
    val name: String = "",
    val description: String = "",
    val color: Int = 0,
    val deadline: LocalDate? = null,
)

class ProjectEditViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val argId: String = handle["id"] ?: "new"
    private val projectId: String = handle.get<String>("draftId") ?: (if (argId == "new") newId() else argId).also { handle["draftId"] = it }
    private var original: ProjectEntity? = null
    private val _form = MutableStateFlow(ProjectForm())
    val form = _form.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = c.projects.get(projectId)
            original = existing
            _form.value = if (existing == null) {
                ProjectForm(loaded = true)
            } else {
                ProjectForm(
                    loaded = true,
                    exists = true,
                    name = existing.name,
                    description = existing.description,
                    color = existing.color,
                    deadline = existing.deadline?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() },
                )
            }
        }
    }

    fun update(transform: (ProjectForm) -> ProjectForm) = _form.update(transform)

    fun save(onDone: (String) -> Unit) {
        val f = _form.value
        if (f.name.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            c.projects.save(
                ProjectEntity(
                    id = projectId,
                    name = f.name.trim(),
                    description = f.description.trim(),
                    color = f.color,
                    deadline = f.deadline?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli(),
                    isArchived = original?.isArchived ?: false,
                    createdAt = original?.createdAt ?: now,
                    updatedAt = now,
                ),
            )
            onDone(projectId)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectEditScreen(onBack: () -> Unit, onSaved: (id: String, isNew: Boolean) -> Unit) {
    val vm = appViewModel { c, h -> ProjectEditViewModel(c, h) }
    val form by vm.form.collectAsStateWithLifecycle()
    var showDate by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (form.exists) R.string.edit_project else R.string.new_project)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    IconButton(onClick = { vm.save { id -> onSaved(id, !form.exists) } }, enabled = form.name.isNotBlank()) {
                        Icon(Icons.Filled.Check, stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        if (!form.loaded) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            OutlinedTextField(
                value = form.name,
                onValueChange = { v -> vm.update { it.copy(name = v) } },
                label = { Text(stringResource(R.string.project_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.description,
                onValueChange = { v -> vm.update { it.copy(description = v) } },
                label = { Text(stringResource(R.string.project_description)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            Text(stringResource(R.string.project_color), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ColorSwatches(AppTheme.extended.projectColors, form.color, { i -> vm.update { it.copy(color = i) } })
            Text(stringResource(R.string.project_deadline), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val deadline = form.deadline
            if (deadline == null) {
                AssistChip(
                    onClick = { showDate = true },
                    label = { Text(stringResource(R.string.add_deadline)) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) },
                )
            } else {
                InputChip(
                    selected = true,
                    onClick = { showDate = true },
                    label = { Text(dateLabel(deadline.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli())) },
                    leadingIcon = { Icon(Icons.Outlined.CalendarToday, null) },
                    trailingIcon = {
                        IconButton(onClick = { vm.update { it.copy(deadline = null) } }) {
                            Icon(Icons.Outlined.Delete, stringResource(R.string.action_clear))
                        }
                    },
                )
            }
        }
    }
    if (showDate) {
        DatePickerModal(form.deadline, onDismiss = { showDate = false }) { d -> vm.update { it.copy(deadline = d) } }
    }
}
