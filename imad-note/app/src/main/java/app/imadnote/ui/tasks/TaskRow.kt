package app.imadnote.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.data.db.TaskEntity
import app.imadnote.ui.components.Pill
import app.imadnote.ui.components.formatDay
import app.imadnote.ui.components.formatTime
import app.imadnote.ui.components.priorityColor
import app.imadnote.ui.components.storedColor
import app.imadnote.ui.theme.ImadTheme
import java.time.LocalDate

/** Round check button tinted by priority, as in most task apps. */
@Composable
fun TaskCheck(done: Boolean, priority: Int, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ImadTheme.colors
    val tint = if (done) colors.textTertiary else priorityColor(priority, colors)
    val label = stringResource(if (done) R.string.task_mark_open else R.string.task_mark_done)
    Box(
        modifier.size(44.dp).clip(CircleShape).clickable(role = Role.Checkbox, onClick = onToggle).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .background(if (done) tint else if (priority > 0) tint.copy(alpha = 0.10f) else Color.Transparent)
                .border(2.dp, tint, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (done) Icon(Icons.Rounded.Check, null, tint = colors.surface, modifier = Modifier.size(14.dp)) }
    }
}

@Composable
fun TaskRow(task: TaskEntity, data: TaskData, onToggle: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier, showProject: Boolean = true) {
    val colors = ImadTheme.colors
    val today = LocalDate.now()
    val subs = data.subtasksOf(task.id)
    val project = task.projectId?.let { data.projectById[it] }
    val labels = data.labelsOf(task.id)
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(colors.surface)
            .clickable(onClick = onOpen).padding(end = 12.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.Top,
    ) {
        TaskCheck(task.isDone, task.priority, onToggle)
        Column(Modifier.weight(1f).padding(top = 11.dp, bottom = 9.dp)) {
            Text(
                task.title, style = MaterialTheme.typography.bodyLarge,
                color = if (task.isDone) colors.textTertiary else colors.text,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            val due = task.due
            val hasMeta = due != null || subs.isNotEmpty() || (showProject && project != null) || labels.isNotEmpty() || task.recurrence != null || task.reminderAt != null
            if (hasMeta) {
                FlowRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (due != null) {
                        val overdue = !task.isDone && due < today
                        val text = formatDay(due, today) + (task.dueTime?.let { " " + formatTime(it) } ?: "")
                        Meta(Icons.Rounded.Event, text, if (overdue) colors.danger else if (due == today) colors.primary else colors.textSecondary)
                    }
                    if (task.recurrence != null) Meta(Icons.Rounded.Repeat, null, colors.textSecondary)
                    if (task.reminderAt != null && !task.isDone) Meta(Icons.Rounded.Alarm, null, colors.textSecondary)
                    if (subs.isNotEmpty()) {
                        val done = subs.count { it.isDone }
                        Meta(Icons.Rounded.SubdirectoryArrowRight, pluralStringResource(R.plurals.subtask_progress, subs.size, done, subs.size), colors.textSecondary)
                    }
                    if (showProject && project != null) Pill("${project.icon} ${project.name}", storedColor(project.color, colors))
                    for (label in labels) Pill(isolate("#${label.name}"), storedColor(label.color, colors))
                }
            }
        }
    }
}

@Composable
private fun Meta(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String?, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        if (text != null) {
            Spacer(Modifier.width(3.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = color, maxLines = 1)
        }
    }
}

/** Wraps text in Unicode isolates so tags like "#work" keep their own direction inside Arabic text. */
fun isolate(text: String) = "\u2068$text\u2069"
