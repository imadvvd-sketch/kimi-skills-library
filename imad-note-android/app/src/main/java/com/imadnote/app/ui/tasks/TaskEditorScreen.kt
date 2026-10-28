package com.imadnote.app.ui.tasks

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.imadnote.app.AppContainer
import com.imadnote.app.R
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.data.newId
import com.imadnote.app.domain.Priority
import com.imadnote.app.domain.Recurrence
import com.imadnote.app.reminders.Notifications
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.ConfirmDialog
import com.imadnote.app.ui.components.DatePickerModal
import com.imadnote.app.ui.components.InfoText
import com.imadnote.app.ui.components.LoadingState
import com.imadnote.app.ui.components.ReminderOptions
import com.imadnote.app.ui.components.TaskCheck
import com.imadnote.app.ui.components.TimePickerModal
import com.imadnote.app.ui.components.dateLabel
import com.imadnote.app.ui.components.priorityName
import com.imadnote.app.ui.components.recurrenceLabel
import com.imadnote.app.ui.components.reminderLabel
import com.imadnote.app.ui.components.timeLabel
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class SubtaskDraft(val id: String = newId(), val title: String, val done: Boolean = false)

data class TaskEditorState(
    val loaded: Boolean = false,
    val exists: Boolean = false,
    val title: String = "",
    val description: String = "",
    val priority: Int = Priority.NONE,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val reminderOffset: Int? = null,
    val recurrence: Recurrence = Recurrence.NONE,
    val projectId: String? = null,
    val noteId: String? = null,
    val completedAt: Long? = null,
    val subtasks: List<SubtaskDraft> = emptyList(),
)

class TaskEditorViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val argId: String = handle["id"] ?: "new"
    val taskId: String = handle.get<String>("draftId") ?: (if (argId == "new") newId() else argId).also { handle["draftId"] = it }
    private var original: TaskEntity? = null
    private var originalSubtasks: Map<String, TaskEntity> = emptyMap()
    private var dirty = false
    private var finished = false

    private val _state = MutableStateFlow(TaskEditorState())
    val state = _state.asStateFlow()
    val projects = c.projects.observeList().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notes = c.notes.observeTitles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        val presetProject: String? = handle.get<String>("projectId")?.takeIf { it.isNotEmpty() }
        val presetNote: String? = handle.get<String>("noteId")?.takeIf { it.isNotEmpty() }
        val presetToday = handle.get<Boolean>("today") ?: false
        viewModelScope.launch {
            val existing = c.tasks.get(taskId)
            if (existing == null) {
                _state.value = TaskEditorState(
                    loaded = true,
                    projectId = presetProject,
                    noteId = presetNote,
                    dueDate = if (presetToday) LocalDate.now() else null,
                )
                return@launch
            }
            original = existing
            val subs = c.tasks.subtasks(existing.id)
            originalSubtasks = subs.associateBy { it.id }
            val zone = ZoneId.systemDefault()
            val due = existing.dueAt?.let { Instant.ofEpochMilli(it).atZone(zone) }
            _state.value = TaskEditorState(
                loaded = true,
                exists = true,
                title = existing.title,
                description = existing.description,
                priority = existing.priority,
                dueDate = due?.toLocalDate(),
                dueTime = if (existing.hasTime) due?.toLocalTime() else null,
                reminderOffset = existing.reminderOffsetMin,
                recurrence = existing.recurrence,
                projectId = existing.projectId,
                noteId = existing.noteId,
                completedAt = existing.completedAt,
                subtasks = subs.map { SubtaskDraft(it.id, it.title, it.isCompleted) },
            )
        }
    }

    fun update(transform: (TaskEditorState) -> TaskEditorState) {
        dirty = true
        _state.update(transform)
    }

    fun addSubtask() = update { it.copy(subtasks = it.subtasks + SubtaskDraft(title = "")) }

    private fun buildEntities(s: TaskEditorState): Pair<TaskEntity, List<TaskEntity>> {
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val dueAt = s.dueDate?.atTime(s.dueTime ?: LocalTime.MIDNIGHT)?.atZone(zone)?.toInstant()?.toEpochMilli()
        val base = original
        val task = TaskEntity(
            id = taskId,
            title = s.title.trim(),
            description = s.description.trim(),
            priority = s.priority,
            dueAt = dueAt,
            hasTime = dueAt != null && s.dueTime != null,
            reminderOffsetMin = if (dueAt != null) s.reminderOffset else null,
            recurrence = if (dueAt != null) s.recurrence else Recurrence.NONE,
            completedAt = s.completedAt,
            projectId = s.projectId,
            parentId = null,
            noteId = s.noteId,
            position = base?.position ?: 0,
            createdAt = base?.createdAt ?: now,
            updatedAt = now,
        )
        val subs = s.subtasks.filter { it.title.isNotBlank() }.map { d ->
            val prev = originalSubtasks[d.id]
            TaskEntity(
                id = d.id,
                title = d.title.trim(),
                completedAt = if (d.done) (prev?.completedAt ?: now) else null,
                parentId = taskId,
                projectId = s.projectId,
                createdAt = prev?.createdAt ?: now,
                updatedAt = now,
            )
        }
        return task to subs
    }

    /** Persists the draft. A task without a title is never saved (and an existing one keeps its old title). */
    private suspend fun persist() {
        val s = _state.value
        if (!s.loaded || !dirty || s.title.isBlank()) return
        val (task, subs) = buildEntities(s)
        c.tasks.save(task, subs)
        original = task
        originalSubtasks = subs.associateBy { it.id }
        dirty = false
    }

    fun saveNow(onDone: () -> Unit) {
        viewModelScope.launch {
            persist()
            finished = true
            onDone()
        }
    }

    fun toggleComplete(onDone: () -> Unit) {
        viewModelScope.launch {
            persist()
            val fresh = c.tasks.get(taskId) ?: return@launch
            c.tasks.toggleComplete(fresh)
            finished = true
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            finished = true
            c.tasks.get(taskId)?.let { c.tasks.delete(it) }
            onDone()
        }
    }

    override fun onCleared() {
        // Leaving the screen any other way (system back, gesture) still saves the draft.
        if (!finished) c.appScope.launch { persist() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(onBack: () -> Unit, onOpenNote: (String) -> Unit) {
    val vm = appViewModel { c, h -> TaskEditorViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val notes by vm.notes.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied = !granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.exists) R.string.edit_task else R.string.new_task)) },
                navigationIcon = {
                    IconButton(onClick = { vm.saveNow(onBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (state.exists) {
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete)) }
                    }
                    IconButton(onClick = { vm.saveNow(onBack) }, enabled = state.title.isNotBlank()) {
                        Icon(Icons.Filled.Check, stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        if (!state.loaded) {
            LoadingState(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.exists) {
                    TaskCheck(state.completedAt != null, state.priority) { vm.toggleComplete(onBack) }
                }
                TextField(
                    value = state.title,
                    onValueChange = { v -> vm.update { it.copy(title = v) } },
                    placeholder = { Text(stringResource(R.string.task_title_hint)) },
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        textDecoration = if (state.completedAt != null) TextDecoration.LineThrough else null,
                    ),
                    colors = transparentFieldColors(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = state.description,
                onValueChange = { v -> vm.update { it.copy(description = v) } },
                label = { Text(stringResource(R.string.task_description)) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            )

            FieldLabel(stringResource(R.string.field_priority))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                listOf(Priority.NONE, Priority.LOW, Priority.MEDIUM, Priority.HIGH).forEach { p ->
                    FilterChip(
                        selected = state.priority == p,
                        onClick = { vm.update { it.copy(priority = p) } },
                        label = { Text(priorityName(p)) },
                    )
                }
            }

            FieldLabel(stringResource(R.string.field_schedule))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                ValueChip(
                    icon = Icons.Outlined.CalendarToday,
                    text = state.dueDate?.let { dateLabel(it.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
                        ?: stringResource(R.string.add_due_date),
                    onClick = { showDate = true },
                    onClear = if (state.dueDate != null) {
                        { vm.update { it.copy(dueDate = null, dueTime = null, reminderOffset = null, recurrence = Recurrence.NONE) } }
                    } else null,
                )
                if (state.dueDate != null) {
                    ValueChip(
                        icon = Icons.Outlined.Schedule,
                        text = state.dueTime?.let { t ->
                            timeLabel(LocalDate.now().atTime(t).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                        } ?: stringResource(R.string.add_time),
                        onClick = { showTime = true },
                        onClear = if (state.dueTime != null) {
                            { vm.update { it.copy(dueTime = null) } }
                        } else null,
                    )
                }
            }
            if (state.dueDate == null) {
                InfoText(stringResource(R.string.schedule_hint), Modifier.padding(top = Spacing.xs))
            } else {
                Row(Modifier.padding(top = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    MenuChip(
                        icon = Icons.Outlined.Alarm,
                        current = reminderLabel(state.reminderOffset),
                        options = ReminderOptions.map { it to reminderLabel(it) },
                        onSelect = { opt ->
                            vm.update { it.copy(reminderOffset = opt) }
                            if (opt != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !Notifications.canPost(context)) {
                                notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                    MenuChip(
                        icon = Icons.Outlined.Repeat,
                        current = recurrenceLabel(state.recurrence),
                        options = Recurrence.entries.map { it to recurrenceLabel(it) },
                        onSelect = { opt -> vm.update { it.copy(recurrence = opt) } },
                    )
                }
                if (permissionDenied) {
                    InfoText(
                        stringResource(R.string.notification_permission_denied),
                        Modifier.padding(top = Spacing.xs),
                    )
                }
            }

            FieldLabel(stringResource(R.string.field_links))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                MenuChip(
                    icon = Icons.Outlined.Folder,
                    current = projects.firstOrNull { it.id == state.projectId }?.name ?: stringResource(R.string.no_project),
                    options = listOf<Pair<String?, String>>(null to stringResource(R.string.no_project)) + projects.map { it.id to it.name },
                    onSelect = { opt -> vm.update { it.copy(projectId = opt) } },
                )
                val untitled = stringResource(R.string.untitled_note)
                MenuChip(
                    icon = Icons.AutoMirrored.Outlined.StickyNote2,
                    current = notes.firstOrNull { it.id == state.noteId }?.let { it.title.ifBlank { untitled } }
                        ?: stringResource(R.string.no_note),
                    options = listOf<Pair<String?, String>>(null to stringResource(R.string.no_note)) +
                        notes.take(50).map { it.id to it.title.ifBlank { untitled } },
                    onSelect = { opt -> vm.update { it.copy(noteId = opt) } },
                )
                state.noteId?.let { id ->
                    TextButton(onClick = { vm.saveNow { onOpenNote(id) } }) { Text(stringResource(R.string.open_note)) }
                }
            }

            FieldLabel(stringResource(R.string.field_subtasks))
            state.subtasks.forEachIndexed { index, sub ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TaskCheck(sub.done, Priority.NONE) {
                        vm.update { s -> s.copy(subtasks = s.subtasks.map { if (it.id == sub.id) it.copy(done = !it.done) else it }) }
                    }
                    TextField(
                        value = sub.title,
                        onValueChange = { v ->
                            vm.update { s -> s.copy(subtasks = s.subtasks.map { if (it.id == sub.id) it.copy(title = v) else it }) }
                        },
                        placeholder = { Text(stringResource(R.string.subtask_hint)) },
                        colors = transparentFieldColors(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (sub.done) TextDecoration.LineThrough else null,
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Sentences),
                        keyboardActions = KeyboardActions(onNext = { if (index == state.subtasks.lastIndex) vm.addSubtask() }),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.update { s -> s.copy(subtasks = s.subtasks.filterNot { it.id == sub.id }) } }) {
                        Icon(Icons.Filled.Close, stringResource(R.string.action_remove))
                    }
                }
            }
            TextButton(onClick = { vm.addSubtask() }) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(Spacing.sm))
                Text(stringResource(R.string.add_subtask))
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }

    if (showDate) {
        DatePickerModal(state.dueDate, onDismiss = { showDate = false }) { d -> vm.update { it.copy(dueDate = d) } }
    }
    if (showTime) {
        TimePickerModal(state.dueTime, onDismiss = { showTime = false }) { t -> vm.update { it.copy(dueTime = t) } }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_task_title),
            text = stringResource(R.string.delete_task_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { vm.delete(onBack) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
)

@Composable
private fun FieldLabel(text: String) {
    Column {
        Spacer(Modifier.height(Spacing.lg))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(Spacing.md))
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(Spacing.sm))
    }
}

@Composable
private fun ValueChip(icon: ImageVector, text: String, onClick: () -> Unit, onClear: (() -> Unit)?) {
    if (onClear == null) {
        AssistChip(onClick = onClick, label = { Text(text) }, leadingIcon = { Icon(icon, null) })
    } else {
        InputChip(
            selected = true,
            onClick = onClick,
            label = { Text(text) },
            leadingIcon = { Icon(icon, null) },
            trailingIcon = {
                IconButton(onClick = onClear) {
                    Icon(Icons.Filled.Close, stringResource(R.string.action_clear))
                }
            },
        )
    }
}

@Composable
fun <T> MenuChip(icon: ImageVector, current: String, options: List<Pair<T, String>>, onSelect: (T) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        AssistChip(onClick = { open = true }, label = { Text(current) }, leadingIcon = { Icon(icon, null) })
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (value, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onSelect(value); open = false })
            }
        }
    }
}
