package app.imadnote.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.data.db.PageEntity
import app.imadnote.data.db.PageKind
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.SkeletonList
import app.imadnote.ui.components.TextInputDialog
import app.imadnote.ui.home.PageCard
import app.imadnote.ui.shell.PageActions
import app.imadnote.ui.shell.PageMenuButton
import app.imadnote.ui.shell.PageTree
import app.imadnote.ui.theme.ImadTheme

/** All pages of the workspace as an indented tree; typing in the filter flattens it to matching titles. */
@Composable
fun NotesScreen(tree: PageTree?, actions: PageActions, onOpenMenu: (() -> Unit)?, onNewPage: () -> Unit, onNewFolder: (String) -> Unit) {
    var filter by rememberSaveable { mutableStateOf("") }
    var namingFolder by remember { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(setOf<String>()) }

    ScreenScaffold(
        title = stringResource(R.string.nav_notes),
        onOpenMenu = onOpenMenu,
        actions = { AppIconButton(Icons.Rounded.CreateNewFolder, stringResource(R.string.folder_new), { namingFolder = true }) },
        fabIcon = Icons.Rounded.Add, fabLabel = stringResource(R.string.page_new), onFab = onNewPage,
    ) {
        if (tree == null) {
            SkeletonList()
            return@ScreenScaffold
        }
        val pages = tree.all.filter { !it.isInbox }
        if (pages.isEmpty()) {
            EmptyState(
                Icons.AutoMirrored.Rounded.Notes, stringResource(R.string.notes_empty_title), stringResource(R.string.notes_empty_message),
                actionLabel = stringResource(R.string.page_new), onAction = onNewPage,
            )
            return@ScreenScaffold
        }
        val query = filter.trim()
        val rows: List<Pair<PageEntity, Int>> = remember(tree, query, expanded) {
            if (query.isNotEmpty()) {
                pages.filter { it.title.contains(query, ignoreCase = true) }.sortedByDescending { it.updatedAt }.map { it to 0 }
            } else {
                buildList {
                    fun walk(nodes: List<PageEntity>, depth: Int) {
                        for (node in nodes) {
                            add(node to depth)
                            if (node.id in expanded) walk(tree.children(node.id), depth + 1)
                        }
                    }
                    walk(tree.roots, 0)
                }
            }
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "filter") {
                AppTextField(
                    filter, { filter = it }, stringResource(R.string.notes_filter_hint), Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    leading = { Icon(Icons.Rounded.Search, null, tint = ImadTheme.colors.textTertiary, modifier = Modifier.size(20.dp)) },
                    trailing = { if (filter.isNotEmpty()) AppIconButton(Icons.Rounded.Close, stringResource(R.string.action_close), { filter = "" }, Modifier.size(24.dp)) },
                )
            }
            if (rows.isEmpty()) {
                item { Text(stringResource(R.string.notes_no_match, query), style = MaterialTheme.typography.bodyMedium, color = ImadTheme.colors.textSecondary, modifier = Modifier.padding(vertical = 24.dp)) }
            }
            items(rows, key = { it.first.id }) { (page, depth) ->
                val childCount = tree.children(page.id).size
                Column(Modifier.padding(start = (depth * 18).dp)) {
                    PageCard(
                        page,
                        trailing = { PageMenuButton(page, tree, actions) },
                        onClick = {
                            // Folders, and pages with children, expand in place; a second tap on a page opens it.
                            if (page.kind == PageKind.FOLDER) expanded = if (page.id in expanded) expanded - page.id else expanded + page.id
                            else {
                                if (childCount > 0 && page.id !in expanded && query.isEmpty()) expanded = expanded + page.id
                                actions.onOpen(page)
                            }
                        },
                    )
                    if (childCount > 0 && query.isEmpty() && page.id !in expanded) {
                        Text(
                            pluralStringResource(R.plurals.page_count, childCount, childCount),
                            style = MaterialTheme.typography.labelSmall, color = ImadTheme.colors.textTertiary, modifier = Modifier.padding(start = 16.dp, top = 4.dp),
                        )
                    }
                }
            }
        }
    }
    if (namingFolder) {
        TextInputDialog(
            title = stringResource(R.string.folder_new),
            confirmLabel = stringResource(R.string.action_create),
            dismissLabel = stringResource(R.string.action_cancel),
            placeholder = stringResource(R.string.folder_name_hint),
            onConfirm = { name -> namingFolder = false; onNewFolder(name) },
            onDismiss = { namingFolder = false },
        )
    }
}
