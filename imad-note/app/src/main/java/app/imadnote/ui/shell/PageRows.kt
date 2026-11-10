package app.imadnote.ui.shell

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.data.db.PageEntity
import app.imadnote.data.db.PageKind
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.TextInputDialog
import app.imadnote.ui.theme.ImadTheme

val PageEntity.displayIcon: String get() = icon ?: if (kind == PageKind.FOLDER) "📁" else "📄"

@Composable
fun PageEntity.displayTitle(): String = title.ifBlank { stringResource(R.string.untitled) }

@Composable
fun relativeTime(millis: Long): String =
    DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE).toString()

/** Callbacks shared by every place that lists pages. */
class PageActions(
    val onOpen: (PageEntity) -> Unit,
    val onCreateInside: (parent: PageEntity) -> Unit,
    val onEdit: (id: String, transform: (PageEntity) -> PageEntity) -> Unit,
    val onMove: (id: String, parentId: String?) -> Unit,
    val onDuplicate: (id: String) -> Unit,
    val onTrash: (id: String) -> Unit,
)

/** Overflow button with the page's context menu, rename dialog and move dialog. */
@Composable
fun PageMenuButton(page: PageEntity, tree: PageTree, actions: PageActions, modifier: Modifier = Modifier) {
    val colors = ImadTheme.colors
    var open by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var moving by remember { mutableStateOf(false) }
    Box(modifier) {
        AppIconButton(Icons.Rounded.MoreHoriz, stringResource(R.string.action_more), { open = true })
        DropdownMenu(open, { open = false }, containerColor = colors.surface, shape = MaterialTheme.shapes.medium) {
            @Composable
            fun entry(label: Int, destructive: Boolean = false, onClick: () -> Unit) = DropdownMenuItem(
                text = { Text(stringResource(label), color = if (destructive) colors.danger else colors.text) },
                onClick = { open = false; onClick() },
            )
            entry(R.string.page_add_inside) { actions.onCreateInside(page) }
            entry(R.string.action_rename) { renaming = true }
            entry(if (page.isFavorite) R.string.page_favorite_remove else R.string.page_favorite_add) { actions.onEdit(page.id) { it.copy(isFavorite = !it.isFavorite) } }
            if (page.kind == PageKind.PAGE) {
                entry(if (page.isPinned) R.string.page_unpin else R.string.page_pin) { actions.onEdit(page.id) { it.copy(isPinned = !it.isPinned) } }
                entry(R.string.action_duplicate) { actions.onDuplicate(page.id) }
            }
            entry(R.string.page_move) { moving = true }
            entry(R.string.page_move_to_trash, destructive = true) { actions.onTrash(page.id) }
        }
    }
    if (renaming) {
        TextInputDialog(
            title = stringResource(R.string.action_rename),
            confirmLabel = stringResource(R.string.action_save),
            dismissLabel = stringResource(R.string.action_cancel),
            initial = page.title,
            placeholder = stringResource(R.string.untitled),
            onConfirm = { name -> renaming = false; actions.onEdit(page.id) { it.copy(title = name) } },
            onDismiss = { renaming = false },
        )
    }
    if (moving) {
        MovePageDialog(page, tree, onMove = { parent -> moving = false; actions.onMove(page.id, parent) }, onDismiss = { moving = false })
    }
}

/** Lists every valid destination; a page cannot be moved into itself or its own descendants. */
@Composable
private fun MovePageDialog(page: PageEntity, tree: PageTree, onMove: (String?) -> Unit, onDismiss: () -> Unit) {
    val colors = ImadTheme.colors
    val excluded = remember(page.id, tree) { tree.descendants(page.id) + page.id }
    val targets = remember(excluded, tree) {
        val out = ArrayList<Pair<PageEntity, Int>>()
        fun walk(nodes: List<PageEntity>, depth: Int) {
            for (node in nodes) {
                if (node.id in excluded) continue
                out += node to depth
                walk(tree.children(node.id), depth + 1)
            }
        }
        walk(tree.roots, 0)
        out
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.page_move), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                @Composable
                fun row(label: String, depth: Int, onClick: () -> Unit) = Text(
                    label, style = MaterialTheme.typography.bodyLarge, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(start = (depth * 16).dp, top = 12.dp, bottom = 12.dp),
                )
                row("⤴  " + stringResource(R.string.page_move_top), 0) { onMove(null) }
                for ((target, depth) in targets) row("${target.displayIcon}  ${target.displayTitle()}", depth) { onMove(target.id) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = colors.textSecondary) } },
        containerColor = colors.surface,
        shape = MaterialTheme.shapes.large,
    )
}
