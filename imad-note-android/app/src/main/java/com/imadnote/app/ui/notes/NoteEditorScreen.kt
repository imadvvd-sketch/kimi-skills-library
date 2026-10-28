package com.imadnote.app.ui.notes

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Title
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imadnote.app.R
import com.imadnote.app.domain.RichText
import com.imadnote.app.ui.LocalAppContainer
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.AttachmentImage
import com.imadnote.app.ui.components.ColorSwatches
import com.imadnote.app.ui.components.ConfirmDialog
import com.imadnote.app.ui.components.InfoText
import com.imadnote.app.ui.components.LoadingState
import com.imadnote.app.ui.components.LocalFeedback
import com.imadnote.app.ui.components.RichTextTransformation
import com.imadnote.app.ui.components.SectionHeader
import com.imadnote.app.ui.components.TaskRow
import com.imadnote.app.ui.components.contentWidth
import com.imadnote.app.ui.components.noteBackground
import com.imadnote.app.ui.components.rememberRichTextStyle
import com.imadnote.app.ui.components.rememberTaskToggle
import com.imadnote.app.ui.components.timeLabel
import com.imadnote.app.ui.tasks.transparentFieldColors
import com.imadnote.app.ui.theme.AppTheme
import com.imadnote.app.ui.theme.Motion
import com.imadnote.app.ui.theme.Radius
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    onBack: () -> Unit,
    onOpenImage: (noteId: String, index: Int) -> Unit,
    onOpenTask: (String) -> Unit,
    onNewLinkedTask: (noteId: String) -> Unit,
) {
    val vm = appViewModel { c, h -> NoteEditorViewModel(c, h) }
    val state by vm.state.collectAsStateWithLifecycle()
    val projects by vm.projects.collectAsStateWithLifecycle()
    val linkedTasks by vm.linkedTasks.collectAsStateWithLifecycle()
    val container = LocalAppContainer.current
    val feedback = LocalFeedback.current
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val toggleTask = rememberTaskToggle(vm.taskActions)
    val richStyle = rememberRichTextStyle()
    val transformation = remember(richStyle) { RichTextTransformation(richStyle) }

    var showColors by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var pickProject by remember { mutableStateOf(false) }
    var focusItemId by remember { mutableStateOf<String?>(null) }
    var pendingCameraPath by rememberSaveable { mutableStateOf<String?>(null) }
    var autoPickDone by rememberSaveable { mutableStateOf(false) }
    // Body keeps its own selection so formatting can act on it; the text itself lives in the ViewModel.
    var body by remember { mutableStateOf(TextFieldValue("")) }
    var bodyReady by remember { mutableStateOf(false) }
    val bodyFocus = remember { FocusRequester() }
    var bodyFocused by remember { mutableStateOf(false) }

    LaunchedEffect(state.loaded) {
        if (state.loaded && !bodyReady) {
            body = TextFieldValue(state.content, TextRange(state.content.length))
            bodyReady = true
        }
    }
    fun setBody(value: TextFieldValue) {
        body = value
        if (value.text != state.content) vm.update { it.copy(content = value.text) }
    }
    fun applyEdit(edit: RichText.Edit) {
        setBody(TextFieldValue(edit.text, TextRange(edit.selectionStart, edit.selectionEnd)))
        if (!bodyFocused) bodyFocus.requestFocus()
    }

    val pickImages = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris -> vm.addImages(uris) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingCameraPath
        pendingCameraPath = null
        if (path != null) {
            if (ok) vm.addCameraImage(File(path)) else File(path).delete()
        }
    }
    fun launchPicker() = pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    fun launchCamera() {
        val file = vm.newCameraFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingCameraPath = file.absolutePath
        try {
            takePicture.launch(uri)
        } catch (_: ActivityNotFoundException) {
            pendingCameraPath = null
            file.delete()
            vm.update { it.copy(errorRes = R.string.error_no_camera) }
        }
    }

    LaunchedEffect(state.loaded) {
        if (state.loaded && vm.mode == NoteEditorViewModel.MODE_IMAGE && !autoPickDone) {
            autoPickDone = true
            launchPicker()
        }
    }
    val errorText = state.errorRes?.let { stringResource(it) }
    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbar.showSnackbar(errorText)
            vm.clearError()
        }
    }
    BackHandler { vm.close(onBack) }

    val archivedMsg = stringResource(R.string.note_archived)
    val restoredMsg = stringResource(R.string.note_restored)
    val deletedMsg = stringResource(R.string.note_deleted)
    val undoLabel = stringResource(R.string.action_undo)
    val bg = noteBackground(state.color)

    Scaffold(
        containerColor = bg,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg, scrolledContainerColor = bg),
                title = {
                    Column {
                        Text(
                            stringResource(if (vm.isNew) R.string.new_note else R.string.edit_note),
                            style = MaterialTheme.typography.titleSmall,
                        )
                        state.updatedAt?.let {
                            Text(
                                stringResource(R.string.edited_at, timeLabel(it)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { vm.close(onBack) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
                actions = {
                    if (!state.archived) {
                        IconButton(onClick = { vm.update { it.copy(pinned = !it.pinned) } }) {
                            Icon(
                                if (state.pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                stringResource(if (state.pinned) R.string.action_unpin else R.string.action_pin),
                            )
                        }
                    }
                    IconButton(onClick = {
                        val archiving = !state.archived
                        val id = vm.noteId
                        vm.setArchived(archiving) {
                            onBack()
                            feedback.show(if (archiving) archivedMsg else restoredMsg, undoLabel) {
                                container.appScope.launch { container.notes.setArchived(id, !archiving) }
                            }
                        }
                    }) {
                        Icon(
                            if (state.archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                            stringResource(if (state.archived) R.string.action_unarchive else R.string.action_archive),
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more)) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }, shape = Radius.sm) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.move_to_project)) },
                                leadingIcon = { Icon(Icons.Outlined.Folder, null) },
                                onClick = { menuOpen = false; pickProject = true },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.add_linked_task)) },
                                leadingIcon = { Icon(Icons.Outlined.AddTask, null) },
                                onClick = { menuOpen = false; vm.prepareLinkedTask(onNewLinkedTask) },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = { menuOpen = false; confirmDelete = true },
                            )
                        }
                    }
                    FilledIconButton(
                        onClick = { vm.close(onBack) },
                        modifier = Modifier.padding(end = Spacing.sm),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Icon(Icons.Filled.Check, stringResource(R.string.action_done))
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Column(Modifier.navigationBarsPadding().imePadding()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    AnimatedVisibility(
                        visible = showColors,
                        enter = expandVertically(tween(Motion.STANDARD, easing = Motion.EmphasizedDecelerate)),
                        exit = shrinkVertically(tween(Motion.FAST)),
                    ) {
                        ColorSwatches(
                            colors = AppTheme.extended.noteColors,
                            selected = state.color,
                            onSelect = { i -> vm.update { it.copy(color = i) } },
                            modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.xs),
                            names = noteColorNames(),
                        )
                    }
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ToolButton(Icons.Outlined.Title, R.string.format_heading) {
                            applyEdit(RichText.cycleHeading(body.text, body.selection.start, body.selection.end))
                        }
                        ToolButton(Icons.Outlined.FormatBold, R.string.format_bold) {
                            applyEdit(RichText.toggleInline(body.text, body.selection.start, body.selection.end, "**"))
                        }
                        ToolButton(Icons.Outlined.FormatItalic, R.string.format_italic) {
                            applyEdit(RichText.toggleInline(body.text, body.selection.start, body.selection.end, "_"))
                        }
                        ToolButton(Icons.AutoMirrored.Outlined.FormatListBulleted, R.string.format_bullets) {
                            applyEdit(RichText.toggleBlock(body.text, body.selection.start, body.selection.end, RichText.Block.BULLET))
                        }
                        ToolButton(Icons.Outlined.FormatListNumbered, R.string.format_numbers) {
                            applyEdit(RichText.toggleBlock(body.text, body.selection.start, body.selection.end, RichText.Block.NUMBER))
                        }
                        ToolButton(Icons.Outlined.FormatQuote, R.string.format_quote) {
                            applyEdit(RichText.toggleBlock(body.text, body.selection.start, body.selection.end, RichText.Block.QUOTE))
                        }
                        VerticalDivider(Modifier.height(24.dp).padding(horizontal = Spacing.xs), color = MaterialTheme.colorScheme.outlineVariant)
                        ToolButton(Icons.Outlined.CheckBox, R.string.add_checklist_item) { focusItemId = vm.addChecklistItem() }
                        ToolButton(Icons.Outlined.Image, R.string.add_image, enabled = !state.busy) { launchPicker() }
                        ToolButton(Icons.Outlined.PhotoCamera, R.string.take_photo, enabled = !state.busy) { launchCamera() }
                        ToolButton(Icons.Outlined.Palette, R.string.note_color) { showColors = !showColors }
                    }
                }
            }
        },
    ) { padding ->
        if (!state.loaded) {
            LoadingState(Modifier.padding(padding), rows = 3)
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .contentWidth(),
        ) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (state.attachments.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                    contentPadding = PaddingValues(horizontal = Spacing.lg),
                    modifier = Modifier.padding(vertical = Spacing.sm),
                ) {
                    itemsIndexed(state.attachments, key = { _, a -> a.id }) { index, a ->
                        Box {
                            AttachmentImage(
                                container.notes.fileFor(a),
                                Modifier
                                    .size(if (state.attachments.size == 1) 240.dp else 160.dp)
                                    .clip(Radius.lg)
                                    .clickable(role = Role.Image, onClickLabel = stringResource(R.string.cd_view_image)) { onOpenImage(vm.noteId, index) },
                            )
                            IconButton(
                                onClick = { vm.removeAttachment(a) },
                                modifier = Modifier.align(Alignment.TopEnd),
                            ) {
                                Box(Modifier.size(28.dp).clip(Radius.full).background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Close, stringResource(R.string.remove_image), tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            TextField(
                value = state.title,
                onValueChange = { v -> vm.update { it.copy(title = v) } },
                placeholder = { Text(stringResource(R.string.note_title_hint), style = MaterialTheme.typography.headlineSmall) },
                textStyle = MaterialTheme.typography.headlineSmall,
                colors = transparentFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { bodyFocus.requestFocus() }),
                modifier = Modifier.fillMaxWidth(),
            )
            TextField(
                value = body,
                onValueChange = { v ->
                    val continued = if (v.selection.collapsed) RichText.continueList(body.text, v.text, v.selection.start) else null
                    setBody(if (continued != null) TextFieldValue(continued.text, TextRange(continued.selectionStart)) else v)
                },
                placeholder = {
                    Text(stringResource(if (vm.mode == NoteEditorViewModel.MODE_IDEA) R.string.idea_hint else R.string.note_body_hint))
                },
                textStyle = MaterialTheme.typography.bodyLarge,
                visualTransformation = transformation,
                colors = transparentFieldColors(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(bodyFocus)
                    .onFocusChanged { bodyFocused = it.isFocused },
                minLines = if (state.checklist.isEmpty()) 8 else 1,
            )
            if (state.checklist.isNotEmpty()) {
                val doneCount = state.checklist.count { it.checked }
                Text(
                    stringResource(R.string.checklist_progress, doneCount, state.checklist.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                )
            }
            state.checklist.forEach { item ->
                ChecklistRow(
                    item = item,
                    requestFocus = focusItemId == item.id,
                    onFocused = { focusItemId = null },
                    onToggle = { vm.updateChecklistItem(item.id) { it.copy(checked = !it.checked) } },
                    onText = { t -> vm.updateChecklistItem(item.id) { it.copy(text = t) } },
                    onNext = { focusItemId = vm.addChecklistItem(afterId = item.id) },
                    onRemove = { vm.removeChecklistItem(item.id) },
                )
            }
            if (state.checklist.isNotEmpty()) {
                TextButton(onClick = { focusItemId = vm.addChecklistItem() }, modifier = Modifier.padding(start = Spacing.sm)) {
                    Text(stringResource(R.string.add_checklist_item))
                }
            }
            if (linkedTasks.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm), color = MaterialTheme.colorScheme.outlineVariant)
                SectionHeader(stringResource(R.string.linked_tasks), Modifier.padding(horizontal = Spacing.lg), count = linkedTasks.size)
                linkedTasks.forEach { t ->
                    TaskRow(t, onToggle = { toggleTask(t) }, onClick = { onOpenTask(t.id) }, modifier = Modifier.padding(horizontal = Spacing.sm))
                }
            }
            state.projectId?.let { pid ->
                projects.firstOrNull { it.id == pid }?.let { p ->
                    InfoText(stringResource(R.string.in_project, p.name), Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md))
                }
            }
            if (state.archived) {
                InfoText(stringResource(R.string.note_archived_hint), Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm))
            }
            Spacer(Modifier.height(Spacing.xxl))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_note_title),
            text = stringResource(R.string.delete_note_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = {
                vm.delete {
                    onBack()
                    feedback.show(deletedMsg)
                }
            },
            onDismiss = { confirmDelete = false },
        )
    }
    if (pickProject) {
        AlertDialog(
            onDismissRequest = { pickProject = false },
            shape = Radius.lg,
            title = { Text(stringResource(R.string.move_to_project)) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    val options = listOf<Pair<String?, String>>(null to stringResource(R.string.no_project)) + projects.map { it.id to it.name }
                    if (projects.isEmpty()) InfoText(stringResource(R.string.no_projects_yet), Modifier.padding(bottom = Spacing.sm))
                    options.forEach { (id, name) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(Radius.sm)
                                .clickable(role = Role.RadioButton) {
                                    vm.update { it.copy(projectId = id) }
                                    pickProject = false
                                }
                                .padding(vertical = Spacing.sm),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = state.projectId == id, onClick = null)
                            Spacer(Modifier.width(Spacing.sm))
                            Text(name)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickProject = false }) { Text(stringResource(R.string.action_close)) } },
        )
    }
}

@Composable
private fun noteColorNames(): List<String> = listOf(
    stringResource(R.string.colour_none), stringResource(R.string.colour_sand), stringResource(R.string.colour_sage),
    stringResource(R.string.colour_mist), stringResource(R.string.colour_lilac), stringResource(R.string.colour_blush),
    stringResource(R.string.colour_slate),
)

@Composable
private fun ToolButton(icon: ImageVector, label: Int, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) { Icon(icon, contentDescription = stringResource(label)) }
}

@Composable
private fun ChecklistRow(
    item: ChecklistDraft,
    requestFocus: Boolean,
    onFocused: () -> Unit,
    onToggle: () -> Unit,
    onText: (String) -> Unit,
    onNext: () -> Unit,
    onRemove: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            focus.requestFocus()
            onFocused()
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = Spacing.xs)) {
        IconButton(onClick = onToggle) {
            Icon(
                if (item.checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                stringResource(if (item.checked) R.string.cd_mark_not_done else R.string.cd_mark_done),
                tint = if (item.checked) AppTheme.extended.success else MaterialTheme.colorScheme.outline,
            )
        }
        TextField(
            value = item.text,
            onValueChange = onText,
            placeholder = { Text(stringResource(R.string.list_item_hint)) },
            colors = transparentFieldColors(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, capitalization = KeyboardCapitalization.Sentences),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            modifier = Modifier.weight(1f).focusRequester(focus),
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Close, stringResource(R.string.action_remove), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
