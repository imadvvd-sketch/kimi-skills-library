package app.imadnote.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.imadnote.R
import app.imadnote.data.db.TaskEntity
import app.imadnote.ui.appViewModel
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.ChipRow
import app.imadnote.ui.components.ColorDot
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.SkeletonList
import app.imadnote.ui.components.formatDay
import app.imadnote.ui.components.priorityColor
import app.imadnote.ui.components.storedColor
import app.imadnote.ui.theme.ImadTheme
import java.time.LocalDate

enum class TaskFilter { TODAY, UPCOMING, ALL, COMPLETED, PRIORITY, PROJECT }

/** A titled section of tasks; [accent] colours the header dot. */
data class TaskSection(val key: String, val title: String, val tasks: List<TaskEntity>, val accent: androidx.compose.ui.graphics.Color? = null)

@Composable
fun TasksScreen(onOpenMenu: (() -> Unit)?, onOpenTask: (String) -> Unit, onSearch: () -> Unit) {
    val viewModel = appViewModel { TasksViewModel(it) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf(TaskFilter.TODAY) }
    var draft by rememberSaveable { mutableStateOf("") }
    val today = LocalDate.now()

    fun submit() {
        viewModel.add(draft, defaultDue = if (filter == TaskFilter.TODAY) today else null)
        draft = ""
    }

    ScreenScaffold(
        title = stringResource(R.string.nav_tasks),
        onOpenMenu = onOpenMenu,
        actions = { AppIconButton(Icons.Rounded.Search, stringResource(R.string.nav_search), onSearch) },
    ) {
        val current = data
        if (current == null) {
            SkeletonList()
            return@ScreenScaffold
        }
        val sections = buildSections(current, filter, today)
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item(key = "add") {
                AppTextField(
                    draft, { draft = it }, stringResource(R.string.tasks_add_hint), Modifier.fillMaxWidth().testTag("task-add"),
                    onDone = ::submit,
                    trailing = { AppIconButton(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.tasks_add), ::submit, enabled = draft.isNotBlank(), tint = ImadTheme.colors.primary) },
                )
            }
            item(key = "filters") {
                ChipRow(
                    listOf(
                        TaskFilter.TODAY to stringResource(R.string.tasks_today), TaskFilter.UPCOMING to stringResource(R.string.tasks_upcoming),
                        TaskFilter.ALL to stringResource(R.string.tasks_all), TaskFilter.PRIORITY to stringResource(R.string.tasks_by_priority),
                        TaskFilter.PROJECT to stringResource(R.string.tasks_by_project), TaskFilter.COMPLETED to stringResource(R.string.tasks_completed),
                    ),
                    filter, { filter = it }, Modifier.padding(vertical = 6.dp),
                )
            }
            if (sections.all { it.tasks.isEmpty() }) {
                item(key = "empty") {
                    EmptyState(
                        Icons.Rounded.TaskAlt,
                        stringResource(if (filter == TaskFilter.TODAY) R.string.tasks_empty_today else R.string.cal_nothing),
                        stringResource(if (filter == TaskFilter.COMPLETED) R.string.tasks_empty_completed else R.string.tasks_empty_message),
                    )
                }
            }
            for (section in sections) {
                if (section.tasks.isEmpty()) continue
                if (section.title.isNotEmpty()) {
                    item(key = "h-${section.key}") {
                        Row(Modifier.padding(top = 12.dp, bottom = 2.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            section.accent?.let { ColorDot(it); Spacer(Modifier.width(8.dp)) }
                            Text(section.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold), color = section.accent ?: ImadTheme.colors.textSecondary)
                            Spacer(Modifier.width(6.dp))
                            Text("${section.tasks.size}", style = MaterialTheme.typography.labelMedium, color = ImadTheme.colors.textTertiary)
                        }
                    }
                }
                items(section.tasks, key = { "${section.key}-${it.id}" }) { task ->
                    TaskRow(task, current, onToggle = { viewModel.setDone(task.id, !task.isDone) }, onOpen = { onOpenTask(task.id) }, showProject = filter != TaskFilter.PROJECT)
                }
            }
        }
    }
}

@Composable
private fun buildSections(data: TaskData, filter: TaskFilter, today: LocalDate): List<TaskSection> {
    val colors = ImadTheme.colors
    val open = data.topLevel.filter { !it.isDone }
    val byDue = compareBy<TaskEntity>({ it.dueDate ?: Long.MAX_VALUE }, { it.dueTime ?: Int.MAX_VALUE }, { -it.priority }, { it.sortIndex })
    return when (filter) {
        TaskFilter.TODAY -> listOf(
            TaskSection("overdue", stringResource(R.string.tasks_overdue), open.filter { it.due != null && it.due!! < today }.sortedWith(byDue), colors.danger),
            TaskSection("today", stringResource(R.string.tasks_today), open.filter { it.due == today }.sortedWith(compareBy<TaskEntity>({ it.dueTime ?: Int.MAX_VALUE }, { -it.priority }, { it.sortIndex })), colors.primary),
        )
        TaskFilter.UPCOMING -> open.filter { it.due != null && it.due!! > today }.sortedWith(byDue).groupBy { it.due!! }
            .map { (day, tasks) -> TaskSection(day.toString(), formatDay(day, today), tasks) }
        TaskFilter.ALL -> listOf(
            TaskSection("overdue", stringResource(R.string.tasks_overdue), open.filter { it.due != null && it.due!! < today }.sortedWith(byDue), colors.danger),
            TaskSection("dated", stringResource(R.string.tasks_upcoming), open.filter { it.due != null && it.due!! >= today }.sortedWith(byDue), colors.primary),
            TaskSection("nodate", stringResource(R.string.date_none), open.filter { it.due == null }.sortedWith(compareBy({ -it.priority }, { it.sortIndex }))),
        )
        TaskFilter.COMPLETED -> data.topLevel.filter { it.isDone }.sortedByDescending { it.completedAt ?: 0 }
            .groupBy { java.time.Instant.ofEpochMilli(it.completedAt ?: 0).atZone(java.time.ZoneId.systemDefault()).toLocalDate() }
            .map { (day, tasks) -> TaskSection("done-$day", formatDay(day, today), tasks) }
        TaskFilter.PRIORITY -> listOf(3 to R.string.priority_high, 2 to R.string.priority_medium, 1 to R.string.priority_low, 0 to R.string.priority_none).map { (level, label) ->
            TaskSection("p$level", stringResource(label), open.filter { it.priority == level }.sortedWith(byDue), if (level > 0) priorityColor(level, colors) else null)
        }
        TaskFilter.PROJECT -> data.projects.map { project ->
            TaskSection(project.id, "${project.icon} ${project.name}", open.filter { it.projectId == project.id }.sortedWith(byDue), storedColor(project.color, colors))
        } + TaskSection("none", stringResource(R.string.tasks_no_project), open.filter { it.projectId == null }.sortedWith(byDue))
    }
}
