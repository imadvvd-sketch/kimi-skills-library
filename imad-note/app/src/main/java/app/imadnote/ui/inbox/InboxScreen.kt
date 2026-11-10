package app.imadnote.ui.inbox

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.SkeletonList
import app.imadnote.ui.home.PageCard
import app.imadnote.ui.shell.PageActions
import app.imadnote.ui.shell.PageTree
import app.imadnote.ui.theme.ImadTheme

/** One-step capture: type, send. Captures are ordinary pages flagged as inbox until they are filed. */
@Composable
fun InboxScreen(tree: PageTree?, actions: PageActions, onOpenMenu: (() -> Unit)?, onCapture: (String) -> Unit) {
    var draft by rememberSaveable { mutableStateOf("") }
    fun submit() {
        if (draft.isBlank()) return
        onCapture(draft)
        draft = ""
    }
    ScreenScaffold(title = stringResource(R.string.nav_inbox), onOpenMenu = onOpenMenu) {
        if (tree == null) {
            SkeletonList()
            return@ScreenScaffold
        }
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "capture") {
                AppTextField(
                    draft, { draft = it }, stringResource(R.string.inbox_hint), Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = false, imeAction = ImeAction.Default,
                    trailing = {
                        AppIconButton(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.inbox_add), ::submit, enabled = draft.isNotBlank(), tint = ImadTheme.colors.primary)
                    },
                )
            }
            if (tree.inbox.isEmpty()) {
                item { EmptyState(Icons.Rounded.Inbox, stringResource(R.string.inbox_empty_title), stringResource(R.string.inbox_empty_message)) }
            }
            items(tree.inbox, key = { it.id }) { page ->
                PageCard(
                    page,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIconButton(Icons.AutoMirrored.Rounded.DriveFileMove, stringResource(R.string.inbox_file), { actions.onEdit(page.id) { it.copy(isInbox = false) } })
                            AppIconButton(Icons.Rounded.DeleteOutline, stringResource(R.string.page_move_to_trash), { actions.onTrash(page.id) })
                        }
                    },
                    onClick = { actions.onOpen(page) },
                )
            }
        }
    }
}
