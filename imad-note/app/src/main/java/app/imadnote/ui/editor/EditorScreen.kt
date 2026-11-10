package app.imadnote.ui.editor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.imadnote.LocalAppContainer
import app.imadnote.R
import app.imadnote.data.db.PageEntity
import app.imadnote.domain.editor.BlockType
import app.imadnote.ui.appViewModel
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.SkeletonList
import app.imadnote.ui.theme.ImadTheme
import coil.compose.AsyncImage

@Composable
fun EditorScreen(pageId: String, onBack: () -> Unit, onOpenPage: (String) -> Unit) {
    val viewModel = appViewModel(key = "editor-$pageId") { EditorViewModel(pageId, it) }
    val page by viewModel.page.collectAsStateWithLifecycle()
    val state = viewModel.state
    val doc = state.doc
    val colors = ImadTheme.colors
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.flush() }

    val visible = remember(doc) { doc?.visibleBlocks().orEmpty() }
    val numbering = remember(doc) { doc?.numbering().orEmpty() }
    val headings = remember(doc) { doc?.blocks?.filter { it.type.isHeading }.orEmpty() }

    // Bring the block that is about to receive focus on screen; its row then takes the focus itself.
    val focus = viewModel.focus
    LaunchedEffect(focus?.nonce) {
        val target = focus ?: return@LaunchedEffect
        val index = visible.indexOfFirst { it.id == target.blockId }
        if (index >= 0 && listState.layoutInfo.visibleItemsInfo.none { it.key == target.blockId }) listState.scrollToItem(index + HEADER_ITEMS)
    }

    val actions = remember(viewModel, focus, visible) {
        BlockActions(
            focus = focus,
            onFocusConsumed = viewModel::consumeFocus,
            onTextChange = viewModel::onTextChange,
            onSelectionChange = viewModel::onSelectionChange,
            onBackspaceAtStart = viewModel::onBackspaceAtStart,
            onSelectBlock = viewModel::selectBlock,
            onUpdateProps = viewModel::updateProps,
            onAttach = viewModel::attachFile,
            onJumpTo = viewModel::focusBlock,
        )
    }

    Column(Modifier.fillMaxSize().background(colors.background)) {
        EditorTopBar(
            page = page,
            state = state,
            onBack = { viewModel.flush(); onBack() },
            onUndo = viewModel::undo,
            onRedo = viewModel::redo,
            onToggleFavorite = { viewModel.updatePage { it.copy(isFavorite = !it.isFavorite) } },
            onTogglePin = { viewModel.updatePage { it.copy(isPinned = !it.isPinned) } },
            onDuplicate = { viewModel.duplicate(onOpenPage) },
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(page?.title.orEmpty(), viewModel.exportText()))
            },
            onShare = {
                val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, viewModel.exportText())
                context.startActivity(Intent.createChooser(send, null))
            },
            onTrash = { viewModel.trash(onBack) },
        )

        if (doc == null) {
            SkeletonList(6, Modifier.weight(1f))
        } else {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.widthIn(max = 760.dp).fillMaxSize().testTag("editor-list"),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                ) {
                    item(key = "header") { PageHeader(page, viewModel) }
                    items(visible, key = { it.id }) { block ->
                        BlockRow(
                            block = block,
                            number = numbering[block.id],
                            selected = state.focusedBlockId == block.id,
                            showPlaceholder = state.focusedBlockId == block.id || visible.size == 1,
                            headings = if (block.type == BlockType.TABLE_OF_CONTENTS) headings else emptyList(),
                            actions = actions,
                        )
                    }
                    item(key = "tail") {
                        // Tapping the empty space below the content continues writing at the end.
                        Box(
                            Modifier.fillMaxWidth().height(240.dp)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = viewModel::focusEnd),
                        )
                    }
                }
            }
        }

        val slashQuery = state.focusedBlock?.takeIf { it.type == BlockType.PARAGRAPH }?.text
            ?.takeIf { it.startsWith("/") && ' ' !in it && it.length <= 20 }?.drop(1)
        Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))) {
            if (slashQuery != null) SlashMenu(slashQuery, onPick = { type -> state.focusedBlockId?.let { viewModel.applySlashCommand(it, type) } })
            if (doc != null && state.focusedBlockId != null) EditorToolbar(state, viewModel)
        }
    }
}

/** Cover and title occupy one lazy item before the blocks. */
private const val HEADER_ITEMS = 1

@Composable
private fun EditorTopBar(
    page: PageEntity?,
    state: EditorUiState,
    onBack: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onDuplicate: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onTrash: () -> Unit,
) {
    val colors = ImadTheme.colors
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onBack, tint = colors.text)
        Text(
            stringResource(if (state.saved) R.string.page_saved else R.string.page_saving),
            style = MaterialTheme.typography.labelMedium, color = colors.textTertiary,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp).testTag("save-status"),
        )
        AppIconButton(Icons.AutoMirrored.Rounded.Undo, stringResource(R.string.action_undo), onUndo, enabled = state.canUndo)
        AppIconButton(Icons.AutoMirrored.Rounded.Redo, stringResource(R.string.action_redo), onRedo, enabled = state.canRedo)
        val favorite = page?.isFavorite == true
        AppIconButton(
            if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
            stringResource(if (favorite) R.string.page_favorite_remove else R.string.page_favorite_add),
            onToggleFavorite, tint = if (favorite) colors.warning else colors.textSecondary,
        )
        Box {
            AppIconButton(Icons.Rounded.MoreVert, stringResource(R.string.action_more), { menu = true })
            DropdownMenu(menu, { menu = false }, containerColor = colors.surface, shape = MaterialTheme.shapes.medium) {
                DropdownMenuItem(text = { Text(stringResource(if (page?.isPinned == true) R.string.page_unpin else R.string.page_pin)) }, onClick = { menu = false; onTogglePin() })
                DropdownMenuItem(text = { Text(stringResource(R.string.action_duplicate)) }, onClick = { menu = false; onDuplicate() })
                DropdownMenuItem(text = { Text(stringResource(R.string.page_copy_text)) }, onClick = { menu = false; onCopy() })
                DropdownMenuItem(text = { Text(stringResource(R.string.page_share)) }, onClick = { menu = false; onShare() })
                DropdownMenuItem(text = { Text(stringResource(R.string.page_move_to_trash), color = colors.danger) }, onClick = { menu = false; onTrash() })
            }
        }
    }
}

@Composable
private fun PageHeader(page: PageEntity?, viewModel: EditorViewModel) {
    val colors = ImadTheme.colors
    val attachments = LocalAppContainer.current.attachments
    val cover = rememberAttachment(page?.coverUri)
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) viewModel.setCover(uri) }
    var pickingIcon by remember { mutableStateOf(false) }
    fun pickCover() = coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    Column(Modifier.fillMaxWidth()) {
        if (cover != null) {
            AsyncImage(
                model = attachments.file(cover),
                contentDescription = stringResource(R.string.page_cover),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(148.dp).clip(RoundedCornerShape(16.dp)).background(colors.surfaceAlt),
            )
        }
        Spacer(Modifier.height(if (cover != null) 12.dp else 4.dp))
        if (page?.icon != null) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.page_icon)) { pickingIcon = true },
                contentAlignment = Alignment.Center,
            ) { Text(page.icon, fontSize = 44.sp) }
        }
        Row {
            if (page?.icon == null) HeaderAction(stringResource(R.string.page_add_icon)) { pickingIcon = true }
            if (cover == null) HeaderAction(stringResource(R.string.page_add_cover), ::pickCover)
            else {
                HeaderAction(stringResource(R.string.page_change_cover), ::pickCover)
                HeaderAction(stringResource(R.string.page_remove_cover)) { viewModel.setCover(null) }
            }
        }

        // The title is local state seeded once per page so that typing is never overwritten by the database echo.
        var title by remember(page?.id) { mutableStateOf(page?.title.orEmpty()) }
        Box(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp)) {
            if (title.isEmpty()) Text(stringResource(R.string.page_title_hint), style = MaterialTheme.typography.displaySmall, color = colors.textTertiary)
            BasicTextField(
                value = title,
                onValueChange = { value ->
                    title = value.replace("\n", "")
                    viewModel.setTitle(title)
                },
                textStyle = MaterialTheme.typography.displaySmall.copy(color = colors.text),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { viewModel.focusEnd() }),
                modifier = Modifier.fillMaxWidth().testTag("page-title"),
            )
        }
    }
    if (pickingIcon) {
        EmojiPickerDialog(
            title = stringResource(R.string.page_choose_icon),
            onPick = { emoji -> pickingIcon = false; viewModel.updatePage { it.copy(icon = emoji) } },
            onDismiss = { pickingIcon = false },
            allowRemove = page?.icon != null,
        )
    }
}

@Composable
private fun HeaderAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = ImadTheme.colors.textTertiary)
    }
}
