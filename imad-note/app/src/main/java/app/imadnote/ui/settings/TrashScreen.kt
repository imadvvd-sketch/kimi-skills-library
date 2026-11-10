package app.imadnote.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.imadnote.R
import app.imadnote.data.db.PageEntity
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.ConfirmDialog
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.home.PageCard
import app.imadnote.ui.shell.ShellViewModel
import app.imadnote.ui.shell.displayTitle
import app.imadnote.ui.theme.ImadTheme

@Composable
fun TrashScreen(viewModel: ShellViewModel, onBack: () -> Unit) {
    val trashed by viewModel.trashed.collectAsStateWithLifecycle()
    var deleting by remember { mutableStateOf<PageEntity?>(null) }
    // Only the page that was deleted is listed; the pages nested inside it are restored or removed with it.
    val ids = trashed.mapTo(HashSet()) { it.id }
    val tops = trashed.filter { it.parentId == null || it.parentId !in ids }

    ScreenScaffold(title = stringResource(R.string.nav_trash), onBack = onBack) {
        if (tops.isEmpty()) {
            EmptyState(Icons.Rounded.DeleteOutline, stringResource(R.string.trash_empty_title), stringResource(R.string.trash_empty_message))
            return@ScreenScaffold
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tops, key = { it.id }) { page ->
                PageCard(
                    page,
                    trailing = {
                        Row {
                            AppIconButton(Icons.Rounded.RestoreFromTrash, stringResource(R.string.trash_restore), { viewModel.restorePage(page.id) })
                            AppIconButton(Icons.Rounded.DeleteForever, stringResource(R.string.trash_delete_forever), { deleting = page }, tint = ImadTheme.colors.danger)
                        }
                    },
                    onClick = { viewModel.restorePage(page.id) },
                )
            }
        }
    }
    deleting?.let { page ->
        ConfirmDialog(
            title = stringResource(R.string.trash_delete_forever), message = stringResource(R.string.trash_delete_message, page.displayTitle()),
            confirmLabel = stringResource(R.string.action_delete), dismissLabel = stringResource(R.string.action_cancel), destructive = true,
            onConfirm = { deleting = null; viewModel.deletePageForever(page.id) }, onDismiss = { deleting = null },
        )
    }
}
