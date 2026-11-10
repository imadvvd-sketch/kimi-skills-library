package app.imadnote.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.rounded.Search
import app.imadnote.ui.tasks.due
import app.imadnote.ui.tasks.isDone
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.data.db.PageEntity
import app.imadnote.data.db.WorkspaceEntity
import app.imadnote.ui.components.AppCard
import app.imadnote.ui.components.EmojiTile
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.SectionHeader
import app.imadnote.ui.components.SkeletonList
import app.imadnote.ui.shell.PageActions
import app.imadnote.ui.shell.PageTree
import app.imadnote.ui.shell.displayIcon
import app.imadnote.ui.shell.displayTitle
import app.imadnote.ui.shell.relativeTime
import app.imadnote.ui.theme.ImadTheme
import java.time.LocalTime

@Composable
fun HomeScreen(
    workspace: WorkspaceEntity?, tree: PageTree?, actions: PageActions, onOpenMenu: (() -> Unit)?, onNewPage: () -> Unit,
    onSearch: () -> Unit = {}, onOpenTask: (String) -> Unit = {}, onOpenTasks: () -> Unit = {},
) {
    val tasksVm = app.imadnote.ui.appViewModel { app.imadnote.ui.tasks.TasksViewModel(it) }
    val taskData by tasksVm.data.collectAsStateWithLifecycle()
    val colors = ImadTheme.colors
    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> R.string.home_greeting_morning
            in 12..17 -> R.string.home_greeting_afternoon
            else -> R.string.home_greeting_evening
        }
    }
    ScreenScaffold(
        title = workspace?.name.orEmpty(),
        onOpenMenu = onOpenMenu,
        actions = { app.imadnote.ui.components.AppIconButton(Icons.Rounded.Search, stringResource(R.string.nav_search), onSearch) },
        fabIcon = Icons.Rounded.Add, fabLabel = stringResource(R.string.page_new), onFab = onNewPage,
    ) {
        if (tree == null) {
            SkeletonList()
            return@ScreenScaffold
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Text(stringResource(greeting), style = MaterialTheme.typography.headlineLarge, color = colors.text, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
            }
            taskData?.let { data ->
                val today = java.time.LocalDate.now()
                val due = data.topLevel.filter { t -> !t.isDone && t.due != null && t.due!! <= today }
                    .sortedWith(compareBy({ it.dueDate }, { it.dueTime ?: Int.MAX_VALUE }, { -it.priority })).take(6)
                if (data.topLevel.isNotEmpty()) {
                    item(key = "today-h") {
                        SectionHeader(stringResource(R.string.home_today_tasks)) {
                            androidx.compose.material3.TextButton(onClick = onOpenTasks) { Text(stringResource(R.string.home_see_all), color = colors.primary) }
                        }
                    }
                    if (due.isEmpty()) item(key = "today-empty") { Text(stringResource(R.string.home_all_clear), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(bottom = 8.dp)) }
                    items(due, key = { "task-${it.id}" }) { task ->
                        app.imadnote.ui.tasks.TaskRow(task, data, onToggle = { tasksVm.setDone(task.id, !task.isDone) }, onOpen = { onOpenTask(task.id) })
                    }
                    item(key = "today-gap") { Spacer(Modifier.height(8.dp)) }
                }
            }
            if (tree.all.isEmpty()) {
                item {
                    EmptyState(
                        Icons.AutoMirrored.Rounded.NoteAdd, stringResource(R.string.home_empty_title), stringResource(R.string.home_empty_message),
                        actionLabel = stringResource(R.string.page_new), onAction = onNewPage,
                    )
                }
            }
            if (tree.pinned.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.home_pinned)) }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(tree.pinned, key = { it.id }) { page -> PinnedCard(page) { actions.onOpen(page) } }
                    }
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            if (tree.recent.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.home_recent)) }
                items(tree.recent, key = { "recent-${it.id}" }) { page -> PageCard(page) { actions.onOpen(page) } }
            }
            if (tree.favorites.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
                item { SectionHeader(stringResource(R.string.sidebar_favorites)) }
                items(tree.favorites, key = { "fav-${it.id}" }) { page -> PageCard(page) { actions.onOpen(page) } }
            }
        }
    }
}

@Composable
private fun PinnedCard(page: PageEntity, onClick: () -> Unit) {
    AppCard(Modifier.width(156.dp), onClick = onClick) {
        EmojiTile(page.displayIcon, size = 40.dp)
        Spacer(Modifier.height(20.dp))
        Text(page.displayTitle(), style = MaterialTheme.typography.titleSmall, color = ImadTheme.colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
    }
}

@Composable
fun PageCard(page: PageEntity, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    AppCard(modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(start = 14.dp, top = 10.dp, bottom = 10.dp, end = if (trailing != null) 4.dp else 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiTile(page.displayIcon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(page.displayTitle(), style = MaterialTheme.typography.titleSmall, color = ImadTheme.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.notes_edited, relativeTime(page.updatedAt)), style = MaterialTheme.typography.bodySmall, color = ImadTheme.colors.textSecondary, maxLines = 1)
            }
            trailing?.invoke()
        }
    }
}
