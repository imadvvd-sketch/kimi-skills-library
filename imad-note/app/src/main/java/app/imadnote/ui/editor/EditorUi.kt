package app.imadnote.ui.editor

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.Toc
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ExpandCircleDown
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.HorizontalRule
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.imadnote.R
import app.imadnote.domain.editor.BlockType
import app.imadnote.ui.theme.ImadColors
import app.imadnote.ui.theme.ImadTheme

@StringRes
fun BlockType.labelRes(): Int = when (this) {
    BlockType.PARAGRAPH -> R.string.block_paragraph
    BlockType.HEADING_1 -> R.string.block_h1
    BlockType.HEADING_2 -> R.string.block_h2
    BlockType.HEADING_3 -> R.string.block_h3
    BlockType.BULLET -> R.string.block_bullet
    BlockType.NUMBERED -> R.string.block_numbered
    BlockType.TODO -> R.string.block_todo
    BlockType.TOGGLE -> R.string.block_toggle
    BlockType.QUOTE -> R.string.block_quote
    BlockType.CALLOUT -> R.string.block_callout
    BlockType.DIVIDER -> R.string.block_divider
    BlockType.CODE -> R.string.block_code
    BlockType.TABLE -> R.string.block_table
    BlockType.IMAGE -> R.string.block_image
    BlockType.VIDEO -> R.string.block_video
    BlockType.AUDIO -> R.string.block_audio
    BlockType.FILE -> R.string.block_file
    BlockType.BOOKMARK -> R.string.block_bookmark
    BlockType.EQUATION -> R.string.block_equation
    BlockType.TABLE_OF_CONTENTS -> R.string.block_toc
}

fun BlockType.icon(): ImageVector = when (this) {
    BlockType.PARAGRAPH -> Icons.AutoMirrored.Rounded.Notes
    BlockType.HEADING_1, BlockType.HEADING_2, BlockType.HEADING_3 -> Icons.Rounded.Title
    BlockType.BULLET -> Icons.AutoMirrored.Rounded.FormatListBulleted
    BlockType.NUMBERED -> Icons.Rounded.FormatListNumbered
    BlockType.TODO -> Icons.Rounded.CheckBox
    BlockType.TOGGLE -> Icons.Rounded.ExpandCircleDown
    BlockType.QUOTE -> Icons.Rounded.FormatQuote
    BlockType.CALLOUT -> Icons.Rounded.Lightbulb
    BlockType.DIVIDER -> Icons.Rounded.HorizontalRule
    BlockType.CODE -> Icons.Rounded.Code
    BlockType.TABLE -> Icons.Rounded.TableChart
    BlockType.IMAGE -> Icons.Rounded.Image
    BlockType.VIDEO -> Icons.Rounded.Videocam
    BlockType.AUDIO -> Icons.Rounded.Audiotrack
    BlockType.FILE -> Icons.AutoMirrored.Rounded.InsertDriveFile
    BlockType.BOOKMARK -> Icons.Rounded.Bookmark
    BlockType.EQUATION -> Icons.Rounded.Functions
    BlockType.TABLE_OF_CONTENTS -> Icons.AutoMirrored.Rounded.Toc
}

/** Inline colour keys are persisted in spans; the actual colour depends on the theme. */
enum class InlineColor(val key: String, @StringRes val label: Int) {
    GRAY("gray", R.string.color_gray),
    RED("red", R.string.color_red),
    ORANGE("orange", R.string.color_orange),
    GREEN("green", R.string.color_green),
    BLUE("blue", R.string.color_blue),
    PURPLE("purple", R.string.color_purple);

    fun text(colors: ImadColors): Color = when (this) {
        GRAY -> colors.textSecondary
        RED -> colors.danger
        ORANGE -> if (colors.isDark) colors.warning else Color(0xFFB7791F)
        GREEN -> colors.success
        BLUE -> colors.primary
        PURPLE -> if (colors.isDark) Color(0xFFC3A6F2) else Color(0xFF7B55C7)
    }

    fun highlight(colors: ImadColors): Color = text(colors).copy(alpha = if (colors.isDark) 0.30f else 0.20f)

    companion object {
        fun fromKey(key: String?): InlineColor? = entries.firstOrNull { it.key == key }
    }
}

private val pageEmojis = listOf(
    "📄", "📝", "📒", "📓", "📔", "📕", "📗", "📘", "📙", "📚", "🗒️", "🗂️", "📁", "📌", "📎", "🔖",
    "💡", "🎯", "🚀", "⭐", "🔥", "✅", "📅", "⏰", "📈", "📊", "💼", "🏠", "🧠", "💬", "🔬", "🎓",
    "✈️", "🍽️", "🏋️", "🎵", "🎬", "🎨", "💰", "🛒", "🔧", "🌱", "❤️", "☕", "🌙", "☀️", "🧭", "🔒",
)

@Composable
fun EmojiPickerDialog(title: String, onPick: (String?) -> Unit, onDismiss: () -> Unit, allowRemove: Boolean = true) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyVerticalGrid(GridCells.Adaptive(48.dp), Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                items(pageEmojis) { emoji ->
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { onPick(emoji) },
                        contentAlignment = Alignment.Center,
                    ) { Text(emoji, fontSize = 24.sp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), color = ImadTheme.colors.textSecondary) } },
        dismissButton = {
            if (allowRemove) TextButton(onClick = { onPick(null) }) { Text(stringResource(R.string.action_remove), color = ImadTheme.colors.danger) }
        },
        containerColor = ImadTheme.colors.surface,
        shape = MaterialTheme.shapes.large,
    )
}

@Composable
fun BlockTypeList(types: List<BlockType>, onPick: (BlockType) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        for (type in types) {
            app.imadnote.ui.components.ListRow(
                title = stringResource(type.labelRes()),
                leading = {
                    Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.Icon(type.icon(), null, tint = ImadTheme.colors.textSecondary, modifier = Modifier.size(20.dp))
                    }
                },
                onClick = { onPick(type) },
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}
