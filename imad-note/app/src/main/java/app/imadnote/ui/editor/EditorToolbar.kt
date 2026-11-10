package app.imadnote.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatAlignRight
import androidx.compose.material.icons.automirrored.rounded.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BorderColor
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatStrikethrough
import androidx.compose.material.icons.rounded.FormatUnderlined
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.domain.editor.BlockType
import app.imadnote.domain.editor.InlineStyle
import app.imadnote.domain.editor.TextAlignment
import app.imadnote.ui.theme.ImadTheme

@Composable
private fun ToolButton(icon: ImageVector, label: String, onClick: () -> Unit, active: Boolean = false, enabled: Boolean = true) {
    val colors = ImadTheme.colors
    Box(
        Modifier.size(44.dp).padding(3.dp).clip(RoundedCornerShape(10.dp))
            .background(if (active) colors.primarySoft else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label; selected = active },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = if (active) colors.primary else colors.text.copy(alpha = if (enabled) 0.85f else 0.3f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ToolDivider() = Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(24.dp).background(ImadTheme.colors.border))

@Composable
private fun ColorMenu(expanded: Boolean, highlight: Boolean, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    val colors = ImadTheme.colors
    DropdownMenu(expanded, onDismiss, containerColor = colors.surface, shape = MaterialTheme.shapes.medium) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.editor_color_default)) },
            leadingIcon = { Box(Modifier.size(18.dp).clip(CircleShape).border(1.5.dp, colors.textTertiary, CircleShape)) },
            onClick = { onPick(null) },
        )
        for (color in InlineColor.entries) {
            DropdownMenuItem(
                text = { Text(stringResource(color.label)) },
                leadingIcon = { Box(Modifier.size(18.dp).clip(CircleShape).background(if (highlight) color.highlight(colors) else color.text(colors))) },
                onClick = { onPick(color.key) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorToolbar(state: EditorUiState, viewModel: EditorViewModel, modifier: Modifier = Modifier) {
    val colors = ImadTheme.colors
    val block = state.focusedBlock
    val inlineEnabled = block != null && block.type.hasText && block.type != BlockType.CODE && block.type != BlockType.EQUATION
    var sheet by remember { mutableStateOf<SheetMode?>(null) }
    var colorMenu by remember { mutableStateOf(false) }
    var highlightMenu by remember { mutableStateOf(false) }

    Surface(modifier.fillMaxWidth(), color = colors.surface, shadowElevation = 6.dp) {
        Column {
            HorizontalDivider(color = colors.border)
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolButton(Icons.Rounded.Add, stringResource(R.string.editor_insert_block), { sheet = SheetMode.INSERT })
                ToolButton(Icons.Rounded.SwapHoriz, stringResource(R.string.editor_turn_into), { sheet = SheetMode.CONVERT }, enabled = block != null)
                ToolDivider()
                ToolButton(Icons.Rounded.FormatBold, stringResource(R.string.editor_bold), { viewModel.toggleStyle(InlineStyle.BOLD) }, state.isStyleActive(InlineStyle.BOLD), inlineEnabled)
                ToolButton(Icons.Rounded.FormatItalic, stringResource(R.string.editor_italic), { viewModel.toggleStyle(InlineStyle.ITALIC) }, state.isStyleActive(InlineStyle.ITALIC), inlineEnabled)
                ToolButton(Icons.Rounded.FormatUnderlined, stringResource(R.string.editor_underline), { viewModel.toggleStyle(InlineStyle.UNDERLINE) }, state.isStyleActive(InlineStyle.UNDERLINE), inlineEnabled)
                ToolButton(Icons.Rounded.FormatStrikethrough, stringResource(R.string.editor_strikethrough), { viewModel.toggleStyle(InlineStyle.STRIKE) }, state.isStyleActive(InlineStyle.STRIKE), inlineEnabled)
                ToolButton(Icons.Rounded.Code, stringResource(R.string.editor_inline_code), { viewModel.toggleStyle(InlineStyle.CODE) }, state.isStyleActive(InlineStyle.CODE), inlineEnabled)
                Box {
                    ToolButton(Icons.Rounded.FormatColorText, stringResource(R.string.editor_text_color), { colorMenu = true }, state.isStyleActive(InlineStyle.COLOR), inlineEnabled)
                    ColorMenu(colorMenu, highlight = false, onPick = { viewModel.setColor(InlineStyle.COLOR, it); colorMenu = false }, onDismiss = { colorMenu = false })
                }
                Box {
                    ToolButton(Icons.Rounded.BorderColor, stringResource(R.string.editor_highlight), { highlightMenu = true }, state.isStyleActive(InlineStyle.HIGHLIGHT), inlineEnabled)
                    ColorMenu(highlightMenu, highlight = true, onPick = { viewModel.setColor(InlineStyle.HIGHLIGHT, it); highlightMenu = false }, onDismiss = { highlightMenu = false })
                }
                ToolDivider()
                val alignIcon = when (block?.props?.align) {
                    TextAlignment.CENTER -> Icons.Rounded.FormatAlignCenter
                    TextAlignment.END -> Icons.AutoMirrored.Rounded.FormatAlignRight
                    else -> Icons.AutoMirrored.Rounded.FormatAlignLeft
                }
                ToolButton(alignIcon, stringResource(R.string.editor_align), viewModel::cycleAlignment, enabled = block?.type?.hasText == true)
                ToolButton(Icons.AutoMirrored.Rounded.FormatIndentDecrease, stringResource(R.string.editor_outdent), { viewModel.indentFocused(-1) }, enabled = (block?.indent ?: 0) > 0)
                ToolButton(Icons.AutoMirrored.Rounded.FormatIndentIncrease, stringResource(R.string.editor_indent), { viewModel.indentFocused(1) }, enabled = block != null)
                ToolDivider()
                ToolButton(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.editor_move_up), { viewModel.moveFocused(true) }, enabled = block != null)
                ToolButton(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.editor_move_down), { viewModel.moveFocused(false) }, enabled = block != null)
                ToolButton(Icons.Rounded.ContentCopy, stringResource(R.string.editor_duplicate_block), viewModel::duplicateFocused, enabled = block != null)
                ToolButton(Icons.Rounded.DeleteOutline, stringResource(R.string.editor_delete_block), viewModel::deleteFocused, enabled = block != null)
            }
        }
    }

    sheet?.let { mode ->
        ModalBottomSheet(onDismissRequest = { sheet = null }, containerColor = colors.surface) {
            Text(
                stringResource(if (mode == SheetMode.INSERT) R.string.editor_insert_block else R.string.editor_turn_into),
                style = MaterialTheme.typography.titleMedium, color = colors.text, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            BlockTypeList(
                types = BlockType.entries,
                onPick = { type ->
                    if (mode == SheetMode.INSERT) viewModel.insertAfterFocused(type) else viewModel.convertFocused(type)
                    sheet = null
                },
                modifier = Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            )
        }
    }
}

private enum class SheetMode { INSERT, CONVERT }

/** Inline menu shown while a block's text is a slash command such as "/head". */
@Composable
fun SlashMenu(query: String, onPick: (BlockType) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val matches = remember(query) {
        BlockType.entries.filter { type ->
            query.isEmpty() || type.key.contains(query, ignoreCase = true) ||
                context.getString(type.labelRes()).contains(query, ignoreCase = true) || type.name.replace('_', ' ').contains(query, ignoreCase = true)
        }
    }
    Surface(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium, color = ImadTheme.colors.surface, shadowElevation = 8.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ImadTheme.colors.border),
    ) {
        if (matches.isEmpty()) {
            Text(stringResource(R.string.editor_no_blocks_match), style = MaterialTheme.typography.bodyMedium, color = ImadTheme.colors.textSecondary, modifier = Modifier.padding(16.dp))
        } else {
            BlockTypeList(matches, onPick, Modifier.heightIn(max = 232.dp).verticalScroll(rememberScrollState()).padding(vertical = 4.dp))
        }
    }
    Spacer(Modifier.height(0.dp))
}
