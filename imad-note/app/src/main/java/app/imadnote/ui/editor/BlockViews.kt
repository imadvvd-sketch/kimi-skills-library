package app.imadnote.ui.editor

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.imadnote.LocalAppContainer
import app.imadnote.R
import app.imadnote.data.db.AttachmentEntity
import app.imadnote.domain.editor.Block
import app.imadnote.domain.editor.BlockProps
import app.imadnote.domain.editor.BlockType
import app.imadnote.domain.editor.Equation
import app.imadnote.domain.editor.TextAlignment
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.PrimaryButton
import app.imadnote.ui.theme.ImadTheme
import coil.compose.AsyncImage

/** Everything a block row needs to talk to the editor. */
class BlockActions(
    val focus: FocusRequest?,
    val onFocusConsumed: (Long) -> Unit,
    val onTextChange: (blockId: String, text: String, selStart: Int, selEnd: Int) -> Unit,
    val onSelectionChange: (blockId: String, start: Int, end: Int) -> Unit,
    val onBackspaceAtStart: (blockId: String) -> Unit,
    val onSelectBlock: (blockId: String) -> Unit,
    val onUpdateProps: (blockId: String, typing: Boolean, transform: (BlockProps) -> BlockProps) -> Unit,
    val onAttach: (blockId: String, uri: Uri) -> Unit,
    val onJumpTo: (blockId: String) -> Unit,
)

@Composable
fun BlockRow(
    block: Block,
    number: Int?,
    selected: Boolean,
    showPlaceholder: Boolean,
    headings: List<Block>,
    actions: BlockActions,
    modifier: Modifier = Modifier,
) {
    val colors = ImadTheme.colors
    val typography = MaterialTheme.typography
    val align = when (block.props.align) {
        TextAlignment.START -> TextAlign.Start
        TextAlignment.CENTER -> TextAlign.Center
        TextAlignment.END -> TextAlign.End
    }
    val baseStyle = when (block.type) {
        BlockType.HEADING_1 -> typography.headlineLarge
        BlockType.HEADING_2 -> typography.headlineMedium
        BlockType.HEADING_3 -> typography.headlineSmall
        BlockType.QUOTE -> typography.bodyLarge.copy(fontStyle = FontStyle.Italic)
        BlockType.CODE, BlockType.EQUATION -> typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
        else -> typography.bodyLarge
    }.copy(textAlign = align)
    val topSpace = when (block.type) {
        BlockType.HEADING_1 -> 20.dp
        BlockType.HEADING_2 -> 16.dp
        BlockType.HEADING_3 -> 12.dp
        else -> 3.dp
    }

    @Composable
    fun field(style: TextStyle = baseStyle, placeholder: String = "", richText: Boolean = true, fieldModifier: Modifier = Modifier) {
        BlockTextField(
            block = block,
            textStyle = style,
            placeholder = placeholder,
            focus = actions.focus,
            onFocusConsumed = actions.onFocusConsumed,
            onTextChange = { text, start, end -> actions.onTextChange(block.id, text, start, end) },
            onSelectionChange = { start, end -> actions.onSelectionChange(block.id, start, end) },
            onBackspaceAtStart = { actions.onBackspaceAtStart(block.id) },
            modifier = fieldModifier,
            richText = richText,
        )
    }

    Row(
        modifier.fillMaxWidth().padding(start = (block.indent * 22).dp, top = topSpace, bottom = 3.dp)
            .then(
                if (selected && !block.type.hasText) Modifier.border(1.5.dp, colors.primary, RoundedCornerShape(10.dp)) else Modifier
            ),
        verticalAlignment = Alignment.Top,
    ) {
        when (block.type) {
            BlockType.PARAGRAPH, BlockType.HEADING_1, BlockType.HEADING_2, BlockType.HEADING_3 -> {
                val placeholder = when {
                    block.type.isHeading -> stringResource(block.type.labelRes())
                    showPlaceholder -> stringResource(R.string.editor_placeholder)
                    else -> ""
                }
                field(placeholder = placeholder, fieldModifier = Modifier.weight(1f))
            }

            BlockType.BULLET -> {
                Box(Modifier.width(26.dp).height(26.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(colors.text))
                }
                field(placeholder = stringResource(R.string.block_bullet), fieldModifier = Modifier.weight(1f))
            }

            BlockType.NUMBERED -> {
                Text("${number ?: 1}.", style = typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.width(30.dp).padding(end = 4.dp), textAlign = TextAlign.End)
                field(placeholder = stringResource(R.string.block_numbered), fieldModifier = Modifier.weight(1f))
            }

            BlockType.TODO -> {
                val label = stringResource(R.string.editor_checked)
                Checkbox(
                    checked = block.props.checked,
                    onCheckedChange = { checked -> actions.onUpdateProps(block.id, false) { it.copy(checked = checked) } },
                    modifier = Modifier.size(26.dp).padding(end = 4.dp).semantics { contentDescription = label },
                    colors = CheckboxDefaults.colors(checkedColor = colors.primary, uncheckedColor = colors.textTertiary, checkmarkColor = colors.onPrimary),
                )
                Spacer(Modifier.width(6.dp))
                val style = if (block.props.checked) baseStyle.copy(color = colors.textTertiary, textDecoration = TextDecoration.LineThrough) else baseStyle
                field(style = style, placeholder = stringResource(R.string.block_todo), fieldModifier = Modifier.weight(1f))
            }

            BlockType.TOGGLE -> {
                val collapsed = block.props.collapsed
                val label = stringResource(if (collapsed) R.string.editor_toggle_expand else R.string.editor_toggle_collapse)
                Box(
                    Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button, onClickLabel = label) { actions.onUpdateProps(block.id, false) { it.copy(collapsed = !collapsed) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (collapsed) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.Rounded.KeyboardArrowDown,
                        label, tint = colors.textSecondary, modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(2.dp))
                field(style = baseStyle.copy(fontWeight = FontWeight.Medium), placeholder = stringResource(R.string.block_toggle), fieldModifier = Modifier.weight(1f))
            }

            BlockType.QUOTE -> Row(Modifier.weight(1f).height(IntrinsicSize.Min)) {
                Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(colors.navy))
                Spacer(Modifier.width(12.dp))
                field(placeholder = stringResource(R.string.block_quote), fieldModifier = Modifier.weight(1f).padding(vertical = 2.dp))
            }

            BlockType.CALLOUT -> CalloutBlock(block, actions) { field(placeholder = stringResource(R.string.block_callout), fieldModifier = Modifier.weight(1f)) }

            BlockType.CODE -> Column(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(colors.surfaceAlt).padding(12.dp),
            ) {
                InlinePropField(
                    value = block.props.language.orEmpty(),
                    placeholder = stringResource(R.string.editor_code_language),
                    style = typography.labelSmall.copy(color = colors.textSecondary),
                    onChange = { value -> actions.onUpdateProps(block.id, true) { it.copy(language = value.ifBlank { null }) } },
                )
                Spacer(Modifier.height(6.dp))
                field(placeholder = stringResource(R.string.block_code), richText = false)
            }

            BlockType.EQUATION -> Column(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(colors.surfaceAlt).padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (block.text.isNotBlank()) {
                    Text(
                        Equation.render(block.text),
                        style = typography.titleLarge.copy(fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Normal),
                        color = colors.text, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    )
                }
                field(style = baseStyle.copy(color = colors.textSecondary), placeholder = stringResource(R.string.editor_equation_hint), richText = false)
            }

            BlockType.DIVIDER -> Box(
                Modifier.weight(1f).heightIn(min = 28.dp).clickable(role = Role.Button, onClickLabel = stringResource(R.string.block_divider)) { actions.onSelectBlock(block.id) },
                contentAlignment = Alignment.Center,
            ) { HorizontalDivider(color = colors.border, thickness = 1.5.dp) }

            BlockType.TABLE -> TableBlock(block, actions, Modifier.weight(1f))
            BlockType.IMAGE -> ImageBlock(block, actions, Modifier.weight(1f))
            BlockType.VIDEO, BlockType.AUDIO, BlockType.FILE -> FileBlock(block, actions, Modifier.weight(1f))
            BlockType.BOOKMARK -> BookmarkBlock(block, actions, Modifier.weight(1f))
            BlockType.TABLE_OF_CONTENTS -> TocBlock(block, headings, actions, Modifier.weight(1f))
        }
    }
}

@Composable
private fun InlinePropField(value: String, placeholder: String, style: TextStyle, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    Box(modifier) {
        if (value.isEmpty()) Text(placeholder, style = style, color = ImadTheme.colors.textTertiary)
        BasicTextField(value, onChange, textStyle = style, singleLine = true, cursorBrush = SolidColor(ImadTheme.colors.primary), modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.CalloutBlock(block: Block, actions: BlockActions, field: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit) {
    var picking by remember { mutableStateOf(false) }
    Row(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(ImadTheme.colors.primarySoft).padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        val label = stringResource(R.string.editor_callout_icon)
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).clickable(role = Role.Button, onClickLabel = label) { picking = true },
            contentAlignment = Alignment.Center,
        ) { Text(block.props.emoji ?: "💡", fontSize = 18.sp) }
        Spacer(Modifier.width(10.dp))
        field()
    }
    if (picking) {
        EmojiPickerDialog(
            title = stringResource(R.string.editor_callout_icon),
            onPick = { emoji ->
                picking = false
                if (emoji != null) actions.onUpdateProps(block.id, false) { it.copy(emoji = emoji) }
            },
            onDismiss = { picking = false },
            allowRemove = false,
        )
    }
}

@Composable
private fun TableBlock(block: Block, actions: BlockActions, modifier: Modifier) {
    val colors = ImadTheme.colors
    val cells = block.props.cells
    val columns = cells.firstOrNull()?.size ?: 0
    fun edit(transform: (List<List<String>>) -> List<List<String>>, typing: Boolean = false) =
        actions.onUpdateProps(block.id, typing) { it.copy(cells = transform(it.cells)) }

    Column(modifier) {
        Column(
            Modifier.horizontalScroll(rememberScrollState()).clip(RoundedCornerShape(10.dp)).border(1.dp, colors.border, RoundedCornerShape(10.dp)),
        ) {
            cells.forEachIndexed { r, row ->
                if (r > 0) HorizontalDivider(color = colors.border, modifier = Modifier.width((columns * 132).dp))
                Row(Modifier.height(IntrinsicSize.Min).background(if (r == 0 && block.props.headerRow) colors.surfaceAlt else Color.Transparent)) {
                    row.forEachIndexed { c, cell ->
                        if (c > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(colors.border))
                        val label = stringResource(R.string.editor_table_cell, r + 1, c + 1)
                        BasicTextField(
                            value = cell,
                            onValueChange = { value ->
                                edit({ grid -> grid.mapIndexed { ri, line -> if (ri == r) line.mapIndexed { ci, old -> if (ci == c) value else old } else line } }, typing = true)
                            },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.text, fontWeight = if (r == 0 && block.props.headerRow) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                            cursorBrush = SolidColor(colors.primary),
                            modifier = Modifier.width(131.dp).padding(horizontal = 10.dp, vertical = 10.dp).semantics { contentDescription = label }
                                .then(Modifier),
                        )
                    }
                }
            }
        }
        Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIconButton(Icons.Rounded.Add, stringResource(R.string.editor_table_add_row), { edit({ it + listOf(List(columns) { "" }) }) })
            Text(stringResource(R.string.editor_table_add_row), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            if (cells.size > 1) AppIconButton(Icons.Rounded.Remove, stringResource(R.string.editor_table_remove_row), { edit({ it.dropLast(1) }) })
            Spacer(Modifier.width(8.dp))
            AppIconButton(Icons.Rounded.Add, stringResource(R.string.editor_table_add_column), { edit({ grid -> grid.map { it + "" } }) })
            Text(stringResource(R.string.editor_table_add_column), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
            if (columns > 1) AppIconButton(Icons.Rounded.Remove, stringResource(R.string.editor_table_remove_column), { edit({ grid -> grid.map { it.dropLast(1) } }) })
        }
    }
}

@Composable
internal fun rememberAttachment(id: String?): AttachmentEntity? {
    val repository = LocalAppContainer.current.attachments
    val state by produceState<AttachmentEntity?>(null, id) { value = id?.let { repository.get(it) } }
    return state
}

@Composable
private fun PickerPlaceholder(label: String, type: BlockType, onClick: () -> Unit, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).background(ImadTheme.colors.surfaceAlt).clickable(role = Role.Button, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(type.icon(), null, tint = ImadTheme.colors.textSecondary)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ImadTheme.colors.textSecondary)
    }
}

@Composable
private fun ImageBlock(block: Block, actions: BlockActions, modifier: Modifier) {
    val repository = LocalAppContainer.current.attachments
    val attachment = rememberAttachment(block.props.attachmentId)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) actions.onAttach(block.id, uri) }
    var zoomed by remember { mutableStateOf(false) }
    if (block.props.attachmentId == null) {
        PickerPlaceholder(stringResource(R.string.editor_choose_image), block.type, {
            actions.onSelectBlock(block.id)
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }, modifier)
        return
    }
    Column(modifier) {
        val file = attachment?.let(repository::file)
        AsyncImage(
            model = file,
            contentDescription = block.props.caption ?: stringResource(R.string.block_image),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp, max = 420.dp).clip(RoundedCornerShape(12.dp)).background(ImadTheme.colors.surfaceAlt)
                .clickable(onClickLabel = stringResource(R.string.editor_image_zoom)) {
                    actions.onSelectBlock(block.id)
                    zoomed = true
                },
        )
        CaptionField(block, actions)
        if (zoomed && file != null) ImageViewer(file, block.props.caption) { zoomed = false }
    }
}

@Composable
private fun CaptionField(block: Block, actions: BlockActions) {
    InlinePropField(
        value = block.props.caption.orEmpty(),
        placeholder = stringResource(R.string.editor_caption_hint),
        style = MaterialTheme.typography.bodySmall.copy(color = ImadTheme.colors.textSecondary),
        onChange = { value -> actions.onUpdateProps(block.id, true) { it.copy(caption = value.ifEmpty { null }) } },
        modifier = Modifier.padding(top = 6.dp, start = 2.dp),
    )
}

/** Full-screen image with pinch zoom and pan. */
@Composable
fun ImageViewer(model: Any, description: String?, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = model,
                contentDescription = description,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 6f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer { scaleX = scale; scaleY = scale; translationX = offset.x; translationY = offset.y },
            )
            AppIconButton(Icons.Rounded.Close, stringResource(R.string.action_close), onClose, Modifier.align(Alignment.TopEnd).padding(12.dp), tint = Color.White)
        }
    }
}

fun openAttachment(context: Context, uri: Uri, mimeType: String): Boolean = try {
    context.startActivity(
        Intent(Intent.ACTION_VIEW).setDataAndType(uri, mimeType).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    true
} catch (_: ActivityNotFoundException) {
    false
}

@Composable
private fun FileBlock(block: Block, actions: BlockActions, modifier: Modifier) {
    val context = LocalContext.current
    val colors = ImadTheme.colors
    val repository = LocalAppContainer.current.attachments
    val attachment = rememberAttachment(block.props.attachmentId)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri != null) actions.onAttach(block.id, uri) }
    var noApp by remember { mutableStateOf(false) }
    val (mime, chooseLabel) = when (block.type) {
        BlockType.VIDEO -> "video/*" to R.string.editor_choose_video
        BlockType.AUDIO -> "audio/*" to R.string.editor_choose_audio
        else -> "*/*" to R.string.editor_choose_file
    }
    if (block.props.attachmentId == null) {
        PickerPlaceholder(stringResource(chooseLabel), block.type, {
            actions.onSelectBlock(block.id)
            picker.launch(arrayOf(mime))
        }, modifier)
        return
    }
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.action_open)) {
                    actions.onSelectBlock(block.id)
                    attachment?.let { noApp = !openAttachment(context, repository.shareUri(it), it.mimeType) }
                }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(colors.primarySoft), contentAlignment = Alignment.Center) {
                Icon(block.type.icon(), null, tint = colors.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    attachment?.fileName ?: stringResource(R.string.editor_file_missing),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = colors.text,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                if (attachment != null) {
                    Text(Formatter.formatShortFileSize(context, attachment.sizeBytes), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                }
            }
            Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        }
        if (noApp) Text(stringResource(R.string.editor_no_app), style = MaterialTheme.typography.bodySmall, color = colors.danger, modifier = Modifier.padding(top = 6.dp))
        CaptionField(block.copy(props = block.props.copy(caption = block.props.caption.takeIf { it != attachment?.fileName })), actions)
    }
}

@Composable
private fun BookmarkBlock(block: Block, actions: BlockActions, modifier: Modifier) {
    val context = LocalContext.current
    val colors = ImadTheme.colors
    val url = block.props.url
    var editing by remember(block.id) { mutableStateOf(url == null) }
    if (editing) {
        var draftUrl by remember { mutableStateOf(url.orEmpty()) }
        var draftTitle by remember { mutableStateOf(block.props.caption.orEmpty()) }
        fun save() {
            val clean = draftUrl.trim().let { if (it.isNotEmpty() && "://" !in it) "https://$it" else it }
            if (clean.isEmpty()) return
            actions.onUpdateProps(block.id, false) { it.copy(url = clean, caption = draftTitle.trim().ifEmpty { null }) }
            editing = false
        }
        Column(modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppTextField(draftUrl, { draftUrl = it }, stringResource(R.string.editor_bookmark_url_hint), Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyMedium)
            AppTextField(draftTitle, { draftTitle = it }, stringResource(R.string.editor_bookmark_title_hint), Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.bodyMedium, onDone = ::save)
            PrimaryButton(stringResource(R.string.editor_bookmark_add), ::save, enabled = draftUrl.isNotBlank())
        }
        return
    }
    val host = remember(url) { runCatching { Uri.parse(url).host }.getOrNull() ?: url.orEmpty() }
    Row(
        modifier.clip(RoundedCornerShape(12.dp)).border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.action_open)) {
                actions.onSelectBlock(block.id)
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(BlockType.BOOKMARK.icon(), null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(block.props.caption ?: host, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(url.orEmpty(), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = { editing = true }) { Text(stringResource(R.string.action_edit), color = colors.textSecondary) }
    }
}

@Composable
private fun TocBlock(block: Block, headings: List<Block>, actions: BlockActions, modifier: Modifier) {
    val colors = ImadTheme.colors
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(colors.surfaceAlt)
            .clickable(onClickLabel = stringResource(R.string.block_toc)) { actions.onSelectBlock(block.id) }.padding(vertical = 8.dp),
    ) {
        if (headings.isEmpty()) {
            Text(stringResource(R.string.editor_toc_empty), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
        }
        for (heading in headings) {
            val level = heading.type.ordinal - BlockType.HEADING_1.ordinal
            Text(
                heading.text.ifBlank { stringResource(R.string.untitled) },
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (level == 0) FontWeight.SemiBold else FontWeight.Normal),
                color = if (level == 0) colors.text else colors.textSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { actions.onJumpTo(heading.id) }
                    .padding(start = (14 + level * 16).dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
            )
        }
    }
}
