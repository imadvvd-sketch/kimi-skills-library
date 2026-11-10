package app.imadnote.ui.tasks

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.imadnote.LocalAppContainer
import app.imadnote.R
import app.imadnote.data.db.AttachmentOwner
import app.imadnote.data.db.TaskEntity
import app.imadnote.data.db.TaskStatus
import app.imadnote.data.reminders.AlarmReminders
import app.imadnote.domain.tasks.Recurrence
import app.imadnote.ui.appViewModel
import app.imadnote.ui.components.AppCard
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.ChipRow
import app.imadnote.ui.components.ChoiceDialog
import app.imadnote.ui.components.ConfirmDialog
import app.imadnote.ui.components.DatePickerModal
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.PropertyRow
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.SectionHeader
import app.imadnote.ui.components.SelectChip
import app.imadnote.ui.components.TextInputDialog
import app.imadnote.ui.components.TimePickerModal
import app.imadnote.ui.components.formatDateTime
import app.imadnote.ui.components.formatDay
import app.imadnote.ui.components.formatTime
import app.imadnote.ui.components.priorityColor
import app.imadnote.ui.components.storedColor
import app.imadnote.ui.editor.openAttachment
import app.imadnote.ui.theme.ImadTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Reminder presets, relative to the due date and time. */
enum class ReminderPreset { NONE, AT_TIME, MIN_10, HOUR_1, DAY_1, MORNING }

fun reminderAt(task: TaskEntity, preset: ReminderPreset, zone: ZoneId = ZoneId.systemDefault()): Long? {
    val date = task.due ?: return null
    val minutes = task.dueTime ?: (9 * 60)
    val base = date.atStartOfDay(zone).plusMinutes(minutes.toLong()).toInstant().toEpochMilli()
    return when (preset) {
        ReminderPreset.NONE -> null
        ReminderPreset.AT_TIME -> base
        ReminderPreset.MIN_10 -> base - 10 * 60_000
        ReminderPreset.HOUR_1 -> base - 60 * 60_000
        ReminderPreset.DAY_1 -> base - 24 * 60 * 60_000
        ReminderPreset.MORNING -> date.atStartOfDay(zone).plusHours(9).toInstant().toEpochMilli()
    }
}

@Composable
fun TaskDetailScreen(taskId: String, onBack: () -> Unit, onOpenTask: (String) -> Unit) {
    val viewModel = appViewModel { TasksViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val task = data?.tasks?.firstOrNull { it.id == taskId }
    var confirmDelete by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = "",
        onBack = onBack,
        actions = { if (task != null) AppIconButton(Icons.Rounded.DeleteOutline, stringResource(R.string.task_delete), { confirmDelete = true }) },
    ) {
        val current = data ?: return@ScreenScaffold
        if (task == null) {
            EmptyState(Icons.Rounded.Close, stringResource(R.string.task_not_found), "")
            return@ScreenScaffold
        }
        TaskEditor(task, current, viewModel, onOpenTask)
    }
    if (confirmDelete && task != null) {
        ConfirmDialog(
            stringResource(R.string.task_delete), stringResource(R.string.task_delete_message, task.title),
            stringResource(R.string.action_delete), stringResource(R.string.action_cancel), destructive = true,
            onConfirm = { confirmDelete = false; viewModel.delete(task.id); onBack() }, onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun TaskEditor(task: TaskEntity, data: TaskData, viewModel: TasksViewModel, onOpenTask: (String) -> Unit) {
    val colors = ImadTheme.colors
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val attachmentsRepo = LocalAppContainer.current.attachments
    var dialog by remember { mutableStateOf<String?>(null) }
    var pendingReminder by remember { mutableStateOf<ReminderPreset?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pendingReminder?.let { preset -> viewModel.edit(task.id) { t -> t.copy(reminderAt = reminderAt(t, preset)) } }
        pendingReminder = null
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch { attachmentsRepo.import(uri, task.workspaceId, AttachmentOwner.TASK, task.id) }
    }
    val attachments by remember(task.id) { attachmentsRepo.observeFor(AttachmentOwner.TASK, task.id) }.collectAsStateWithLifecycle(emptyList())

    // Title and notes are local state seeded once, saved after a short pause in typing.
    var title by remember(task.id) { mutableStateOf(task.title) }
    var notes by remember(task.id) { mutableStateOf(task.description) }
    LaunchedEffect(title, notes) {
        delay(400)
        if (title.isNotBlank() && (title != task.title || notes != task.description)) viewModel.edit(task.id) { it.copy(title = title, description = notes) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            TaskCheck(task.isDone, task.priority, { viewModel.setDone(task.id, !task.isDone) })
            Box(Modifier.weight(1f).padding(top = 6.dp)) {
                if (title.isEmpty()) Text(stringResource(R.string.task_title_hint), style = MaterialTheme.typography.headlineSmall, color = colors.textTertiary)
                BasicTextField(
                    title, { title = it.replace("\n", "") },
                    textStyle = MaterialTheme.typography.headlineSmall.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.primary), modifier = Modifier.fillMaxWidth().testTag("task-title"),
                )
            }
        }
        if (task.isDone && task.completedAt != null) {
            Text(stringResource(R.string.task_completed_on, formatDateTime(task.completedAt)), style = MaterialTheme.typography.bodySmall, color = colors.success, modifier = Modifier.padding(start = 44.dp))
        }

        ChipRow(
            listOf(TaskStatus.TODO to stringResource(R.string.status_todo), TaskStatus.IN_PROGRESS to stringResource(R.string.status_in_progress), TaskStatus.DONE to stringResource(R.string.status_done)),
            task.status,
            { status -> if (status == TaskStatus.DONE) viewModel.setDone(task.id, true) else viewModel.edit(task.id) { it.copy(status = status) } },
        )

        AppCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            val due = task.due
            PropertyRow(Icons.Rounded.Event, stringResource(R.string.task_due), due?.let { formatDay(it) } ?: stringResource(R.string.date_none), { dialog = "date" },
                valueColor = if (due != null && !task.isDone && due < LocalDate.now()) colors.danger else null)
            if (due != null) PropertyRow(Icons.Rounded.Schedule, stringResource(R.string.task_time), task.dueTime?.let { formatTime(it) } ?: stringResource(R.string.time_none), { dialog = "time" })
            PropertyRow(Icons.Rounded.Alarm, stringResource(R.string.task_reminder), task.reminderAt?.let { formatDateTime(it) } ?: stringResource(R.string.reminder_none), { dialog = "reminder" })
            PropertyRow(Icons.Rounded.Repeat, stringResource(R.string.task_repeat), repeatLabel(task.recurrence), { dialog = "repeat" })
            PropertyRow(Icons.Rounded.Flag, stringResource(R.string.priority), priorityLabel(task.priority), { dialog = "priority" }, valueColor = if (task.priority > 0) priorityColor(task.priority, colors) else null)
            val project = task.projectId?.let { data.projectById[it] }
            PropertyRow(Icons.Rounded.Folder, stringResource(R.string.task_project), project?.let { "${it.icon} ${it.name}" } ?: stringResource(R.string.tasks_no_project), { dialog = "project" })
        }

        SectionHeader(stringResource(R.string.task_labels))
        val attached = data.labelsOf(task.id).map { it.id }.toSet()
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (label in data.labels) {
                SelectChip(isolate("#${label.name}"), label.id in attached, storedColor(label.color, colors)) {
                    viewModel.setLabels(task.id, if (label.id in attached) attached - label.id else attached + label.id)
                }
            }
            SelectChip("+ " + stringResource(R.string.label_new), false) { dialog = "label" }
        }

        SectionHeader(stringResource(R.string.task_description_hint))
        Box(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(colors.surface).padding(14.dp)) {
            if (notes.isEmpty()) Text(stringResource(R.string.task_description_hint), style = MaterialTheme.typography.bodyLarge, color = colors.textTertiary)
            BasicTextField(notes, { notes = it }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text), cursorBrush = SolidColor(colors.primary), modifier = Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min))
        }

        if (task.parentId == null) {
            SectionHeader(stringResource(R.string.task_subtasks))
            val subs = data.subtasksOf(task.id).sortedBy { it.sortIndex }
            AppCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(4.dp)) {
                for (sub in subs) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TaskCheck(sub.isDone, sub.priority, { viewModel.setDone(sub.id, !sub.isDone) })
                        Text(
                            sub.title, style = MaterialTheme.typography.bodyLarge,
                            color = if (sub.isDone) colors.textTertiary else colors.text,
                            textDecoration = if (sub.isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                            modifier = Modifier.weight(1f).clickable { onOpenTask(sub.id) }.padding(vertical = 10.dp), maxLines = 2, overflow = TextOverflow.Ellipsis,
                        )
                        AppIconButton(Icons.Rounded.Close, stringResource(R.string.action_delete), { viewModel.delete(sub.id) })
                    }
                }
                var subDraft by remember { mutableStateOf("") }
                fun addSub() { viewModel.add(subDraft, parentId = task.id, projectId = task.projectId); subDraft = "" }
                AppTextField(
                    subDraft, { subDraft = it }, stringResource(R.string.task_add_subtask), Modifier.fillMaxWidth().padding(4.dp),
                    onDone = ::addSub,
                    trailing = { AppIconButton(Icons.Rounded.Add, stringResource(R.string.action_add), ::addSub, enabled = subDraft.isNotBlank(), tint = colors.primary) },
                )
            }
        }

        SectionHeader(stringResource(R.string.task_attachments)) {
            AppIconButton(Icons.Rounded.AttachFile, stringResource(R.string.task_add_attachment), { filePicker.launch(arrayOf("*/*")) })
        }
        for (attachment in attachments) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surface)
                    .clickable(role = Role.Button) { openAttachment(context, attachmentsRepo.shareUri(attachment), attachment.mimeType) }.padding(start = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Rounded.InsertDriveFile, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text(attachment.fileName, style = MaterialTheme.typography.bodyMedium, color = colors.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                AppIconButton(Icons.Rounded.Close, stringResource(R.string.action_remove), { scope.launch { attachmentsRepo.delete(attachment.id) } })
            }
        }
        HorizontalDivider(color = colors.border)
    }

    when (dialog) {
        "date" -> DatePickerModal(task.due, onPick = { day -> viewModel.edit(task.id) { it.copy(dueDate = day.toEpochDay()) } }, onDismiss = { dialog = null },
            onClear = { viewModel.edit(task.id) { it.copy(dueDate = null, dueTime = null, reminderAt = null, recurrence = null) } })
        "time" -> TimePickerModal(task.dueTime, onPick = { m -> viewModel.edit(task.id) { it.copy(dueTime = m) } }, onDismiss = { dialog = null },
            onClear = { viewModel.edit(task.id) { it.copy(dueTime = null) } })
        "reminder" -> ChoiceDialog<ReminderPreset>(
            stringResource(R.string.task_reminder),
            buildList<Pair<ReminderPreset, String>> {
                add(ReminderPreset.NONE to stringResource(R.string.reminder_none))
                if (task.due != null) {
                    if (task.dueTime != null) {
                        add(ReminderPreset.AT_TIME to stringResource(R.string.reminder_at_time))
                        add(ReminderPreset.MIN_10 to stringResource(R.string.reminder_10m))
                        add(ReminderPreset.HOUR_1 to stringResource(R.string.reminder_1h))
                    }
                    add(ReminderPreset.MORNING to stringResource(R.string.reminder_morning))
                    add(ReminderPreset.DAY_1 to stringResource(R.string.reminder_1d))
                }
            },
            null,
            onPick = { preset ->
                if (preset != ReminderPreset.NONE && Build.VERSION.SDK_INT >= 33 && !AlarmReminders.canNotify(context)) {
                    pendingReminder = preset
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else viewModel.edit(task.id) { t -> t.copy(reminderAt = reminderAt(t, preset)) }
            },
            onDismiss = { dialog = null },
        )
        "repeat" -> ChoiceDialog(
            stringResource(R.string.task_repeat),
            listOf<Pair<String?, String>>(null to stringResource(R.string.repeat_none)) + Recurrence.Unit.entries.map { Recurrence(it).encode() to repeatLabel(Recurrence(it).encode()) },
            task.recurrence,
            onPick = { rule -> viewModel.edit(task.id) { it.copy(recurrence = rule, dueDate = it.dueDate ?: if (rule != null) LocalDate.now().toEpochDay() else null) } },
            onDismiss = { dialog = null },
        )
        "priority" -> ChoiceDialog(
            stringResource(R.string.priority), listOf(3, 2, 1, 0).map { it to priorityLabel(it) }, task.priority,
            onPick = { p -> viewModel.edit(task.id) { it.copy(priority = p) } }, onDismiss = { dialog = null },
        )
        "project" -> ChoiceDialog(
            stringResource(R.string.task_project),
            listOf<Pair<String?, String>>(null to stringResource(R.string.tasks_no_project)) + data.projects.map { it.id to "${it.icon} ${it.name}" },
            task.projectId,
            onPick = { id -> viewModel.edit(task.id) { it.copy(projectId = id) } }, onDismiss = { dialog = null },
        )
        "label" -> TextInputDialog(
            stringResource(R.string.label_new), stringResource(R.string.action_create), stringResource(R.string.action_cancel),
            placeholder = stringResource(R.string.label_name_hint),
            onConfirm = { name -> dialog = null; viewModel.createLabel(name.removePrefix("#"), task.id) }, onDismiss = { dialog = null },
        )
    }
}

@Composable
fun priorityLabel(level: Int) = stringResource(
    when (level) {
        3 -> R.string.priority_high
        2 -> R.string.priority_medium
        1 -> R.string.priority_low
        else -> R.string.priority_none
    }
)

@Composable
fun repeatLabel(raw: String?): String = when (Recurrence.parse(raw)?.unit) {
    null -> stringResource(R.string.repeat_none)
    Recurrence.Unit.DAY -> stringResource(R.string.repeat_daily)
    Recurrence.Unit.WEEK -> stringResource(R.string.repeat_weekly)
    Recurrence.Unit.MONTH -> stringResource(R.string.repeat_monthly)
    Recurrence.Unit.YEAR -> stringResource(R.string.repeat_yearly)
    Recurrence.Unit.WEEKDAY -> stringResource(R.string.repeat_weekdays)
}
