package com.imadnote.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.imadnote.app.R
import com.imadnote.app.data.NoteWithDetails
import com.imadnote.app.data.ProjectWithStats
import com.imadnote.app.data.TaskEntity
import com.imadnote.app.domain.Priority
import com.imadnote.app.domain.Recurrence
import com.imadnote.app.domain.TaskRules
import com.imadnote.app.ui.theme.AppTheme
import com.imadnote.app.ui.theme.Spacing
import java.io.File
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(Spacing.lg))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.xs))
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 320.dp),
        )
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, count: Int? = null, action: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = Spacing.lg, bottom = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (count != null) {
            Spacer(Modifier.width(Spacing.sm))
            Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.weight(1f))
        action?.invoke()
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
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

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
fun AttachmentImage(file: File, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    SubcomposeAsyncImage(
        model = file,
        contentDescription = stringResource(R.string.cd_image),
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh))
        },
        error = {
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.image_load_error),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(Spacing.sm),
                )
            }
        },
    )
}

@Composable
fun NoteCard(
    note: NoteWithDetails,
    fileFor: (com.imadnote.app.data.AttachmentEntity) -> File,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val n = note.note
    val bg = noteBackground(n.color)
    val border = if (n.color == 0) MaterialTheme.colorScheme.outlineVariant else Color.Transparent
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier.fillMaxWidth().border(1.dp, border, RoundedCornerShape(12.dp)),
    ) {
        Column {
            note.sortedAttachments.firstOrNull()?.let { first ->
                AttachmentImage(fileFor(first), Modifier.fillMaxWidth().height(120.dp))
            }
            Column(Modifier.padding(Spacing.md)) {
                Row(verticalAlignment = Alignment.Top) {
                    if (n.title.isNotBlank()) {
                        Text(
                            n.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (n.isPinned) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = stringResource(R.string.cd_pinned),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (n.content.isNotBlank()) {
                    if (n.title.isNotBlank()) Spacer(Modifier.height(Spacing.xs))
                    Text(
                        n.content,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
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
                            )
                        }
                    }
                    if (items.size > 4) {
                        Text(
                            stringResource(R.string.more_items, items.size - 4),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (n.title.isBlank() && n.content.isBlank() && items.isEmpty() && note.attachments.isEmpty()) {
                    Text(
                        stringResource(R.string.empty_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityFlag(priority: Int, modifier: Modifier = Modifier) {
    if (priority == Priority.NONE) return
    val ext = AppTheme.extended
    val color = when (priority) {
        Priority.HIGH -> ext.priorityHigh
        Priority.MEDIUM -> ext.priorityMedium
        else -> ext.priorityLow
    }
    Icon(Icons.Outlined.Flag, contentDescription = priorityName(priority), tint = color, modifier = modifier.size(16.dp))
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

@Composable
fun TaskCheck(checked: Boolean, priority: Int, onToggle: () -> Unit) {
    val ext = AppTheme.extended
    val tint = when {
        checked -> MaterialTheme.colorScheme.outline
        priority == Priority.HIGH -> ext.priorityHigh
        priority == Priority.MEDIUM -> ext.priorityMedium
        priority == Priority.LOW -> ext.priorityLow
        else -> MaterialTheme.colorScheme.outline
    }
    val label = stringResource(if (checked) R.string.cd_mark_not_done else R.string.cd_mark_done)
    IconButton(onClick = onToggle) {
        Icon(
            if (checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = label,
            tint = tint,
        )
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
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick, role = Role.Button)
            .padding(end = Spacing.md, top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TaskCheck(task.isCompleted, task.priority, onToggle)
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
                    subtasks?.let { (done, total) ->
                        MetaItem(Icons.Filled.Check, "$done/$total", MaterialTheme.colorScheme.onSurfaceVariant)
                    }
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

@Composable
fun ProjectCard(item: ProjectWithStats, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = item.project
    OutlinedCard(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(projectColor(p.color)))
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    p.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                p.deadline?.let { deadline ->
                    val zone = ZoneId.systemDefault()
                    val late = TaskRules.dueDate(deadline, zone)!!.isBefore(LocalDate.now(zone)) && item.doneTasks < item.totalTasks
                    Text(
                        dueLabel(deadline, false),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (late) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (p.description.isNotBlank()) {
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    p.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
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
            modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = projectColor(item.project.color),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            drawStopIndicator = {},
        )
        Spacer(Modifier.width(Spacing.md))
        Text(
            stringResource(R.string.project_progress, item.doneTasks, item.totalTasks),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun ColorSwatches(colors: List<Color>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        colors.forEachIndexed { index, color ->
            val isSelected = index == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .clickable(role = Role.RadioButton) { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.cd_selected), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun InfoText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
