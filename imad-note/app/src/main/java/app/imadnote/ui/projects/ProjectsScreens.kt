package app.imadnote.ui.projects

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap as GStrokeCap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.imadnote.AppContainer
import app.imadnote.R
import app.imadnote.data.db.PageEntity
import app.imadnote.data.db.ProjectEntity
import app.imadnote.data.db.ProjectStatus
import app.imadnote.ui.appViewModel
import app.imadnote.ui.components.AppCard
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.ChipRow
import app.imadnote.ui.components.ConfirmDialog
import app.imadnote.ui.components.DatePickerModal
import app.imadnote.ui.components.EmojiTile
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.Pill
import app.imadnote.ui.components.ProjectPalette
import app.imadnote.ui.components.PropertyRow
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.SectionHeader
import app.imadnote.ui.components.SkeletonList
import app.imadnote.ui.components.TextInputDialog
import app.imadnote.ui.components.formatDay
import app.imadnote.ui.components.storedColor
import app.imadnote.ui.editor.EmojiPickerDialog
import app.imadnote.ui.home.PageCard
import app.imadnote.ui.tasks.TaskData
import app.imadnote.ui.tasks.TaskRow
import app.imadnote.ui.tasks.TasksViewModel
import app.imadnote.ui.tasks.due
import app.imadnote.ui.tasks.isDone
import app.imadnote.ui.theme.ImadTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ProjectStats(val total: Int, val done: Int, val overdue: Int) {
    val open get() = total - done
    val progress get() = if (total == 0) 0f else done.toFloat() / total
}

fun TaskData.statsFor(projectId: String, today: LocalDate = LocalDate.now()): ProjectStats {
    val tasks = topLevel.filter { it.projectId == projectId }
    return ProjectStats(tasks.size, tasks.count { it.isDone }, tasks.count { !it.isDone && it.due != null && it.due!! < today })
}

@Composable
fun projectStatusLabel(status: ProjectStatus) = stringResource(
    when (status) {
        ProjectStatus.PLANNED -> R.string.project_status_planned
        ProjectStatus.ACTIVE -> R.string.project_status_active
        ProjectStatus.ON_HOLD -> R.string.project_status_on_hold
        ProjectStatus.DONE -> R.string.project_status_done
    }
)

@Composable
private fun statusColor(status: ProjectStatus) = when (status) {
    ProjectStatus.PLANNED -> ImadTheme.colors.textSecondary
    ProjectStatus.ACTIVE -> ImadTheme.colors.primary
    ProjectStatus.ON_HOLD -> ImadTheme.colors.warning
    ProjectStatus.DONE -> ImadTheme.colors.success
}

@Composable
fun ProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress }, modifier = modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
        color = color, trackColor = ImadTheme.colors.surfaceAlt, strokeCap = GStrokeCap.Round, gapSize = 0.dp, drawStopIndicator = {},
    )
}

@Composable
fun ProjectsScreen(onOpenMenu: (() -> Unit)?, onOpenProject: (String) -> Unit) {
    val tasks = appViewModel { TasksViewModel(it) }
    val data by tasks.data.collectAsStateWithLifecycle()
    val container = app.imadnote.LocalAppContainer.current
    val scope = rememberCoroutineScope()
    var creating by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(R.string.nav_projects), onOpenMenu = onOpenMenu,
        fabIcon = Icons.Rounded.Add, fabLabel = stringResource(R.string.projects_new), onFab = { creating = true },
    ) {
        val current = data
        if (current == null) {
            SkeletonList()
            return@ScreenScaffold
        }
        if (current.projects.isEmpty()) {
            EmptyState(Icons.Rounded.RocketLaunch, stringResource(R.string.projects_empty_title), stringResource(R.string.projects_empty_message),
                actionLabel = stringResource(R.string.projects_new), onAction = { creating = true })
            return@ScreenScaffold
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(current.projects.sortedWith(compareBy({ it.status == ProjectStatus.DONE }, { it.deadline ?: Long.MAX_VALUE })), key = { it.id }) { project ->
                ProjectCard(project, current.statsFor(project.id)) { onOpenProject(project.id) }
            }
        }
    }
    if (creating) {
        TextInputDialog(
            stringResource(R.string.projects_new), stringResource(R.string.action_create), stringResource(R.string.action_cancel),
            placeholder = stringResource(R.string.project_name_hint),
            onConfirm = { name ->
                creating = false
                val ws = data?.workspaceId ?: return@TextInputDialog
                scope.launch {
                    val color = ProjectPalette[(data?.projects?.size ?: 0) % ProjectPalette.size]
                    onOpenProject(container.projects.create(ws, name, "🚀", color).id)
                }
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun ProjectCard(project: ProjectEntity, stats: ProjectStats, onClick: () -> Unit) {
    val colors = ImadTheme.colors
    val accent = storedColor(project.color, colors)
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiTile(project.icon, size = 40.dp, background = accent.copy(alpha = 0.14f))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(project.name, style = MaterialTheme.typography.titleMedium, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.project_task_count, stats.total, stats.done, stats.total), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            Pill(projectStatusLabel(project.status), statusColor(project.status))
        }
        Spacer(Modifier.height(14.dp))
        ProgressBar(stats.progress, accent)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.project_progress, (stats.progress * 100).toInt()), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary, modifier = Modifier.weight(1f))
            project.deadline?.let { deadline ->
                val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.ofEpochDay(deadline)).toInt()
                val late = days < 0 && project.status != ProjectStatus.DONE
                Text(
                    if (late) pluralStringResource(R.plurals.project_days_overdue, -days, -days) else formatDay(LocalDate.ofEpochDay(deadline)),
                    style = MaterialTheme.typography.labelMedium, color = if (late) colors.danger else colors.textSecondary,
                )
            }
        }
    }
}

class ProjectNotesViewModel(container: AppContainer, projectId: String) : ViewModel() {
    val notes = container.pages.observeByProject(projectId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun ProjectDetailScreen(projectId: String, onBack: () -> Unit, onOpenTask: (String) -> Unit, onOpenPage: (String) -> Unit) {
    val tasks = appViewModel { TasksViewModel(it) }
    val notesVm = appViewModel(key = "project-notes-$projectId") { ProjectNotesViewModel(it, projectId) }
    val data by tasks.data.collectAsStateWithLifecycle()
    val notes by notesVm.notes.collectAsStateWithLifecycle()
    val container = app.imadnote.LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val project = data?.projectById?.get(projectId)
    var confirmDelete by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "", onBack = onBack,
        actions = { if (project != null) AppIconButton(Icons.Rounded.DeleteOutline, stringResource(R.string.project_delete), { confirmDelete = true }) },
    ) {
        val current = data ?: return@ScreenScaffold
        if (project == null) {
            EmptyState(Icons.Rounded.Close, stringResource(R.string.project_not_found), "")
            return@ScreenScaffold
        }
        ProjectBody(project, current, notes, tasks, onOpenTask, onOpenPage,
            onEdit = { transform -> scope.launch { container.projects.edit(project.id, transform) } },
            onNewNote = { scope.launch { onOpenPage(container.pages.create(project.workspaceId, projectId = project.id).id) } },
        )
    }
    if (confirmDelete && project != null) {
        ConfirmDialog(
            stringResource(R.string.project_delete), stringResource(R.string.project_delete_message, project.name),
            stringResource(R.string.action_delete), stringResource(R.string.action_cancel), destructive = true,
            onConfirm = { confirmDelete = false; scope.launch { container.projects.delete(project.id) }; onBack() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun ProjectBody(
    project: ProjectEntity,
    data: TaskData,
    notes: List<PageEntity>,
    tasks: TasksViewModel,
    onOpenTask: (String) -> Unit,
    onOpenPage: (String) -> Unit,
    onEdit: ((ProjectEntity) -> ProjectEntity) -> Unit,
    onNewNote: () -> Unit,
) {
    val colors = ImadTheme.colors
    val accent = storedColor(project.color, colors)
    val stats = data.statsFor(project.id)
    var dialog by remember { mutableStateOf<String?>(null) }
    var name by remember(project.id) { mutableStateOf(project.name) }
    var description by remember(project.id) { mutableStateOf(project.description) }
    LaunchedEffect(name, description) {
        delay(400)
        if (name.isNotBlank() && (name != project.name || description != project.description)) onEdit { it.copy(name = name, description = description) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClickLabel = stringResource(R.string.page_choose_icon)) { dialog = "icon" }) {
                EmojiTile(project.icon, size = 56.dp, background = accent.copy(alpha = 0.14f))
            }
            Spacer(Modifier.width(14.dp))
            Box(Modifier.weight(1f)) {
                if (name.isEmpty()) Text(stringResource(R.string.project_name_hint), style = MaterialTheme.typography.headlineMedium, color = colors.textTertiary)
                BasicTextField(name, { name = it.replace("\n", "") }, textStyle = MaterialTheme.typography.headlineMedium.copy(color = colors.text), cursorBrush = SolidColor(colors.primary))
            }
        }
        Box {
            if (description.isEmpty()) Text(stringResource(R.string.project_description_hint), style = MaterialTheme.typography.bodyLarge, color = colors.textTertiary)
            BasicTextField(description, { description = it }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.textSecondary), cursorBrush = SolidColor(colors.primary), modifier = Modifier.fillMaxWidth())
        }
        ChipRow(ProjectStatus.entries.map { it to projectStatusLabel(it) }, project.status, { s -> onEdit { it.copy(status = s) } })

        // Colour swatches
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            for (argb in ProjectPalette) {
                val selected = argb == project.color
                val swatch = storedColor(argb, colors)
                Box(
                    Modifier.size(30.dp).clip(CircleShape).background(swatch)
                        .then(if (selected) Modifier.border(3.dp, colors.surface, CircleShape).border(5.dp, swatch, CircleShape) else Modifier)
                        .clickable(role = Role.RadioButton) { onEdit { it.copy(color = argb) } }
                        .semantics { contentDescription = "color" },
                )
            }
        }

        AppCard(Modifier.fillMaxWidth()) {
            Row {
                Stat(stats.open.toString(), stringResource(R.string.project_open), colors.text, Modifier.weight(1f))
                Stat(stats.done.toString(), stringResource(R.string.status_done), colors.success, Modifier.weight(1f))
                Stat(stats.overdue.toString(), stringResource(R.string.project_overdue_tasks), if (stats.overdue > 0) colors.danger else colors.textTertiary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            ProgressBar(stats.progress, accent)
            Spacer(Modifier.height(6.dp))
            Text(stringResource(R.string.project_progress, (stats.progress * 100).toInt()), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        }

        AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            PropertyRow(Icons.Rounded.Event, stringResource(R.string.project_start), project.startDate?.let { formatDay(LocalDate.ofEpochDay(it)) } ?: stringResource(R.string.date_none), { dialog = "start" })
            PropertyRow(Icons.Rounded.Flag, stringResource(R.string.project_deadline), project.deadline?.let { formatDay(LocalDate.ofEpochDay(it)) } ?: stringResource(R.string.date_none), { dialog = "deadline" })
        }

        SectionHeader(stringResource(R.string.project_timeline))
        Timeline(project, data, accent)

        SectionHeader(stringResource(R.string.project_tasks))
        var draft by remember { mutableStateOf("") }
        fun addTask() { tasks.add(draft, projectId = project.id); draft = "" }
        AppTextField(
            draft, { draft = it }, stringResource(R.string.tasks_add_hint), Modifier.fillMaxWidth(), onDone = ::addTask,
            trailing = { AppIconButton(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.tasks_add), ::addTask, enabled = draft.isNotBlank(), tint = colors.primary) },
        )
        val projectTasks = data.topLevel.filter { it.projectId == project.id }
            .sortedWith(compareBy({ it.isDone }, { it.dueDate ?: Long.MAX_VALUE }, { -it.priority }))
        if (projectTasks.isEmpty()) Text(stringResource(R.string.project_no_tasks), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        for (task in projectTasks) TaskRow(task, data, { tasks.setDone(task.id, !task.isDone) }, { onOpenTask(task.id) }, showProject = false)

        SectionHeader(stringResource(R.string.project_notes)) {
            AppIconButton(Icons.AutoMirrored.Rounded.NoteAdd, stringResource(R.string.project_new_note), onNewNote)
        }
        if (notes.isEmpty()) Text(stringResource(R.string.project_no_notes), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        for (note in notes) PageCard(note) { onOpenPage(note.id) }
    }

    when (dialog) {
        "icon" -> EmojiPickerDialog(stringResource(R.string.page_choose_icon), { e -> dialog = null; if (e != null) onEdit { it.copy(icon = e) } }, { dialog = null }, allowRemove = false)
        "start" -> DatePickerModal(project.startDate?.let(LocalDate::ofEpochDay), { d -> onEdit { it.copy(startDate = d.toEpochDay()) } }, { dialog = null }, onClear = { onEdit { it.copy(startDate = null) } })
        "deadline" -> DatePickerModal(project.deadline?.let(LocalDate::ofEpochDay), { d -> onEdit { it.copy(deadline = d.toEpochDay()) } }, { dialog = null }, onClear = { onEdit { it.copy(deadline = null) } })
    }
}

@Composable
private fun Stat(value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = ImadTheme.colors.textSecondary)
    }
}

/** Start-to-deadline bar with a marker for today and dots for task due dates (filled when done). */
@Composable
private fun Timeline(project: ProjectEntity, data: TaskData, accent: Color) {
    val colors = ImadTheme.colors
    val start = project.startDate
    val end = project.deadline
    if (start == null || end == null || end <= start) {
        Text(stringResource(R.string.project_timeline_empty), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        return
    }
    val today = LocalDate.now().toEpochDay()
    val span = (end - start).toFloat()
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dots = data.topLevel.filter { it.projectId == project.id && it.dueDate != null }.map { ((it.dueDate!! - start) / span).coerceIn(0f, 1f) to it.isDone }
    AppCard(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            fun x(f: Float) = if (rtl) size.width * (1 - f) else size.width * f
            val y = size.height / 2
            drawLine(colors.surfaceAlt, Offset(0f, y), Offset(size.width, y), strokeWidth = 10.dp.toPx(), cap = GStrokeCap.Round)
            val elapsed = ((today - start) / span).coerceIn(0f, 1f)
            drawLine(accent.copy(alpha = 0.55f), Offset(x(0f), y), Offset(x(elapsed), y), strokeWidth = 10.dp.toPx(), cap = GStrokeCap.Round)
            for ((f, done) in dots) {
                drawCircle(colors.surface, 7.dp.toPx(), Offset(x(f), y))
                drawCircle(if (done) colors.success else colors.textSecondary, 5.dp.toPx(), Offset(x(f), y), style = if (done) androidx.compose.ui.graphics.drawscope.Fill else androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            }
            if (today in start..end) drawLine(colors.danger, Offset(x(elapsed), 2f), Offset(x(elapsed), size.height - 2f), strokeWidth = 2.dp.toPx())
        }
        Spacer(Modifier.height(6.dp))
        Row {
            Text(formatDay(LocalDate.ofEpochDay(start)), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary, modifier = Modifier.weight(1f))
            val left = (end - today).toInt()
            Text(
                if (left >= 0) pluralStringResource(R.plurals.project_days_left, left, left) else pluralStringResource(R.plurals.project_days_overdue, -left, -left),
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = if (left < 0) colors.danger else accent,
            )
            Text(formatDay(LocalDate.ofEpochDay(end)), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}
