package com.imadnote.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.imadnote.app.R
import com.imadnote.app.data.AttachmentEntity
import com.imadnote.app.data.NoteWithDetails
import com.imadnote.app.data.ProjectWithStats
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.domain.Priority
import com.imadnote.app.domain.Recurrence
import com.imadnote.app.domain.TaskRules
import com.imadnote.app.ui.theme.AppTheme
import com.imadnote.app.ui.theme.BorderWidth
import com.imadnote.app.ui.theme.Motion
import com.imadnote.app.ui.theme.Radius
import com.imadnote.app.ui.theme.Spacing
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

// ---------- Surfaces ----------

/** Level-0 card: surface + 1 dp hairline, radius md. Tinted cards drop the border. */
@Composable
fun ImadCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surface,
    bordered: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val border = if (bordered) BorderStroke(BorderWidth, MaterialTheme.colorScheme.outlineVariant) else null
    if (onClick != null) {
        Surface(onClick = onClick, shape = Radius.md, color = color, border = border, modifier = modifier) { content() }
    } else {
        Surface(shape = Radius.md, color = color, border = border, modifier = modifier) { content() }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, count: Int? = null, color: Color = MaterialTheme.colorScheme.onSurfaceVariant, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = Spacing.minTouchTarget).padding(top = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = color, modifier = Modifier.semantics { heading() })
        if (count != null && count > 0) {
            Spacer(Modifier.width(Spacing.sm))
            CountBadge(count)
        }
        Spacer(Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun CountBadge(count: Int) {
    Box(
        Modifier.clip(Radius.full).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Tappable search entry that opens the Search screen (keeps lists free of keyboard focus). */
@Composable
fun SearchPill(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = Radius.md,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth().heightIn(min = Spacing.minTouchTarget),
    ) {
        Row(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(Spacing.md))
            Text(stringResource(R.string.search_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Radius.lg,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun InfoText(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = modifier)
}

// ---------- Colour helpers ----------

@Composable
fun noteBackground(colorIndex: Int): Color {
    val colors = AppTheme.extended.noteColors
    return colors.getOrElse(colorIndex) { colors[0] }
}

@Composable
fun projectColor(colorIndex: Int): Color {
    val colors = AppTheme.extended.projectColors
    return colors.getOrElse(colorIndex) { colors[0] }
}

@Composable
fun ColorSwatches(colors: List<Color>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, names: List<String>? = null) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        colors.forEachIndexed { index, color ->
            val isSelected = index == selected
            val name = names?.getOrNull(index) ?: stringResource(R.string.cd_colour_n, index + 1)
            Box(
                modifier = Modifier
                    .size(Spacing.minTouchTarget)
                    .clip(Radius.full)
                    .clickable(role = Role.RadioButton, onClickLabel = name) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(Radius.full)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.dp else BorderWidth,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = Radius.full,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.cd_selected_colour, name), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ---------- Media ----------

@Composable
fun AttachmentImage(file: File, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    SubcomposeAsyncImage(
        model = file,
        contentDescription = stringResource(R.string.cd_image),
        contentScale = contentScale,
        modifier = modifier,
        loading = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh)) },
        error = {
            Column(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(Spacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.image_load_error),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        },
    )
}

// ---------- Notes ----------

/** The brand motif: a small folded corner on tinted notes. Decorative. */
@Composable
private fun FoldedCorner(modifier: Modifier = Modifier) {
    val shade = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.size(14.dp)) {
        val w = size.width
        val path = Path().apply {
            if (rtl) {
                moveTo(0f, 0f); lineTo(w, 0f); lineTo(w, w); close()
            } else {
                moveTo(0f, 0f); lineTo(w, 0f); lineTo(0f, w); close()
            }
        }
        drawPath(path, shade)
        drawLine(shade.copy(alpha = 0.14f), if (rtl) Offset(0f, 0f) else Offset(w, 0f), if (rtl) Offset(w, w) else Offset(0f, w), 2f)
    }
}

@Composable
fun NoteCard(
    note: NoteWithDetails,
    fileFor: (AttachmentEntity) -> File,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    maxPreviewLines: Int = 6,
) {
    val n = note.note
    val tinted = n.color != 0
    val richStyle = rememberRichTextStyle()
    ImadCard(modifier = modifier.fillMaxWidth(), color = noteBackground(n.color), bordered = !tinted, onClick = onClick) {
        Box {
            Column {
                note.sortedAttachments.firstOrNull()?.let { first ->
                    Box {
                        AttachmentImage(fileFor(first), Modifier.fillMaxWidth().height(120.dp))
                        if (note.attachments.size > 1) {
                            Text(
                                "+${note.attachments.size - 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.align(Alignment.BottomEnd).padding(Spacing.sm)
                                    .clip(Radius.sm).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                Column(Modifier.padding(Spacing.md)) {
                    Row(verticalAlignment = Alignment.Top) {
                        if (n.title.isNotBlank()) {
                            Text(n.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        if (n.isPinned) {
                            Icon(Icons.Filled.PushPin, stringResource(R.string.cd_pinned), Modifier.padding(start = Spacing.xs).size(16.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (n.content.isNotBlank()) {
                        if (n.title.isNotBlank()) Spacer(Modifier.height(Spacing.xs))
                        Text(
                            richPreview(n.content, richStyle),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = maxPreviewLines,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                        )
                    }
                    val items = note.sortedChecklist
                    if (items.isNotEmpty()) {
                        Spacer(Modifier.height(Spacing.xs))
                        items.take(4).forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                Icon(
                                    if (item.isChecked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(Spacing.xs))
                                Text(
                                    item.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        if (items.size > 4) {
                            Text(stringResource(R.string.more_items, items.size - 4), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (n.title.isBlank() && n.content.isBlank() && items.isEmpty() && note.attachments.isEmpty()) {
                        Text(stringResource(R.string.empty_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (tinted && note.attachments.isEmpty()) FoldedCorner(Modifier.align(Alignment.TopEnd))
        }
    }
}

// ---------- Tasks ----------

@Composable
fun priorityColor(priority: Int): Color {
    val ext = AppTheme.extended
    return when (priority) {
        Priority.HIGH -> ext.priorityHigh
        Priority.MEDIUM -> ext.priorityMedium
        Priority.LOW -> ext.priorityLow
        else -> MaterialTheme.colorScheme.outline
    }
}

@Composable
fun PriorityFlag(priority: Int, modifier: Modifier = Modifier) {
    if (priority == Priority.NONE) return
    Icon(Icons.Outlined.Flag, contentDescription = priorityName(priority), tint = priorityColor(priority), modifier = modifier.size(16.dp))
}

@Composable
fun priorityName(priority: Int): String = stringResource(
    when (priority) {
        Priority.HIGH -> R.string.priority_high
        Priority.MEDIUM -> R.string.priority_medium
        Priority.LOW -> R.string.priority_low
        else -> R.string.priority_none
    },
)

/** Rounded-square check tinted by priority inside a 48 dp target; done = filled success. */
@Composable
fun TaskCheck(checked: Boolean, priority: Int, taskTitle: String? = null, onToggle: () -> Unit) {
    val tint = priorityColor(priority)
    val label = when {
        taskTitle == null -> stringResource(if (checked) R.string.cd_mark_not_done else R.string.cd_mark_done)
        checked -> stringResource(R.string.cd_mark_task_not_done, taskTitle)
        else -> stringResource(R.string.cd_mark_task_done, taskTitle)
    }
    IconButton(onClick = onToggle, modifier = Modifier.semantics { contentDescription = label }) {
        SquareCheck(checked, tint)
    }
}

@Composable
fun TaskRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    projectName: String? = null,
    subtasks: Pair<Int, Int>? = null,
) {
    val zone = ZoneId.systemDefault()
    val overdue = TaskRules.isOverdue(task.timing, LocalDate.now(zone), zone)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(Radius.md)
            .clickable(onClick = onClick, role = Role.Button, onClickLabel = stringResource(R.string.action_edit))
            .padding(end = Spacing.md, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheck(task.isCompleted, task.priority, taskTitle = task.title, onToggle = onToggle)
        Column(Modifier.weight(1f)) {
            Text(
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
            val hasMeta = task.dueAt != null || projectName != null || subtasks != null ||
                task.recurrence != Recurrence.NONE || task.description.isNotBlank()
            if (hasMeta) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    task.dueAt?.let { due ->
                        val color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        MetaItem(Icons.Outlined.Schedule, dueLabel(due, task.hasTime), color)
                    }
                    if (task.reminderOffsetMin != null && task.dueAt != null) {
                        Icon(Icons.Outlined.Alarm, stringResource(R.string.cd_has_reminder), Modifier.size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (task.recurrence != Recurrence.NONE) {
                        Icon(Icons.Outlined.Repeat, recurrenceLabel(task.recurrence), Modifier.size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    subtasks?.let { (done, total) -> MetaItem(Icons.Filled.Check, "$done/$total", MaterialTheme.colorScheme.onSurfaceVariant) }
                    projectName?.let { MetaItem(Icons.Outlined.Folder, it, MaterialTheme.colorScheme.onSurfaceVariant) }
                    if (task.description.isNotBlank()) {
                        Icon(Icons.AutoMirrored.Outlined.Notes, stringResource(R.string.cd_has_description), Modifier.size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        PriorityFlag(task.priority)
    }
}

@Composable
private fun MetaItem(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
        Spacer(Modifier.width(2.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
    }
}

// ---------- Projects ----------

@Composable
fun ProjectCard(item: ProjectWithStats, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = item.project
    ImadCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(Radius.full).background(projectColor(p.color)))
                Spacer(Modifier.width(Spacing.sm))
                Text(p.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                val zone = ZoneId.systemDefault()
                val complete = item.totalTasks > 0 && item.doneTasks == item.totalTasks
                val late = p.deadline != null && TaskRules.dueDate(p.deadline, zone)!!.isBefore(LocalDate.now(zone)) && !complete
                when {
                    p.isArchived -> StatusChip(stringResource(R.string.status_archived), Status.INFO)
                    complete -> StatusChip(stringResource(R.string.status_done), Status.SUCCESS)
                    late -> StatusChip(stringResource(R.string.status_late), Status.DANGER)
                    else -> StatusChip(stringResource(R.string.status_active), Status.INFO)
                }
            }
            p.deadline?.let { deadline ->
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    stringResource(R.string.deadline_label, dueLabel(deadline, false)),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (p.description.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(p.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(Spacing.md))
            ProjectProgress(item)
        }
    }
}

@Composable
fun ProjectProgress(item: ProjectWithStats) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { item.progress },
            modifier = Modifier.weight(1f).height(6.dp).clip(Radius.xs),
            color = projectColor(item.project.color),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            drawStopIndicator = {},
            gapSize = 0.dp,
        )
        Spacer(Modifier.width(Spacing.md))
        Text(stringResource(R.string.project_progress, item.doneTasks, item.totalTasks), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Accent used for a note's icon tile; follows the note tint so lists stay colour-coded. */
@Composable
fun noteAccent(colorIndex: Int): Color {
    val ext = AppTheme.extended
    return when (colorIndex) {
        1 -> ext.accent
        2 -> ext.success
        3 -> MaterialTheme.colorScheme.secondary
        4 -> ext.projectColors[3]
        5 -> ext.projectColors[4]
        6 -> ext.projectColors[6]
        else -> MaterialTheme.colorScheme.secondary
    }
}

/** List-layout note row: icon tile, title, one-line preview, date. */
@Composable
fun NoteListRow(note: NoteWithDetails, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val n = note.note
    val richStyle = rememberRichTextStyle()
    val accent = noteAccent(n.color)
    val icon = when {
        note.attachments.isNotEmpty() -> Icons.Outlined.Image
        note.checklist.isNotEmpty() -> Icons.Outlined.CheckBox
        else -> Icons.AutoMirrored.Outlined.Notes
    }
    ImadCard(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(Spacing.md), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(Radius.md).background(accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        n.title.ifBlank { stringResource(R.string.untitled_note) },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (n.isPinned) {
                        Icon(Icons.Filled.PushPin, stringResource(R.string.cd_pinned), Modifier.padding(start = Spacing.xs).size(14.dp), MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val preview = when {
                    n.content.isNotBlank() -> richPreview(n.content.replace('\n', ' '), richStyle)
                    note.checklist.isNotEmpty() -> androidx.compose.ui.text.AnnotatedString(
                        stringResource(R.string.checklist_progress, note.checklist.count { it.isChecked }, note.checklist.size),
                    )
                    else -> null
                }
                if (preview != null) {
                    Text(preview, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(relativeTime(n.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}
