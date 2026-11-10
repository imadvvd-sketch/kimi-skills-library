package app.imadnote.ui.editor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import app.imadnote.domain.editor.Block
import app.imadnote.domain.editor.InlineStyle
import app.imadnote.domain.editor.Span
import app.imadnote.ui.theme.ImadColors
import app.imadnote.ui.theme.ImadTheme

/**
 * Soft keyboards do not report Backspace when there is nothing left to delete. Each block field
 * therefore starts with an invisible zero-width space: when it disappears, the user pressed
 * Backspace at the start of the block. The sentinel never reaches the document model.
 */
internal const val SENTINEL = "​"

fun Span.toSpanStyle(colors: ImadColors): SpanStyle = when (style) {
    InlineStyle.BOLD -> SpanStyle(fontWeight = FontWeight.Bold)
    InlineStyle.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    InlineStyle.UNDERLINE -> SpanStyle(textDecoration = TextDecoration.Underline)
    InlineStyle.STRIKE -> SpanStyle(textDecoration = TextDecoration.LineThrough)
    InlineStyle.CODE -> SpanStyle(fontFamily = FontFamily.Monospace, background = colors.surfaceAlt, color = colors.danger)
    InlineStyle.COLOR -> SpanStyle(color = InlineColor.fromKey(value)?.text(colors) ?: colors.text)
    InlineStyle.HIGHLIGHT -> SpanStyle(background = InlineColor.fromKey(value)?.highlight(colors) ?: colors.primarySoft)
    InlineStyle.LINK -> SpanStyle(color = colors.primary, textDecoration = TextDecoration.Underline)
}

/** Underline and strikethrough on the same range must be combined into one decoration. */
fun annotate(text: String, spans: List<Span>, colors: ImadColors, offset: Int = 0): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    for (span in spans) {
        val start = (span.start + offset).coerceIn(0, text.length)
        val end = (span.end + offset).coerceIn(0, text.length)
        if (end <= start) continue
        if (span.style == InlineStyle.UNDERLINE || span.style == InlineStyle.STRIKE) continue
        builder.addStyle(span.toSpanStyle(colors), start, end)
    }
    val decorated = spans.filter { it.style == InlineStyle.UNDERLINE || it.style == InlineStyle.STRIKE }
    if (decorated.isNotEmpty()) {
        val cuts = decorated.flatMap { listOf(it.start, it.end) }.distinct().sorted()
        for ((from, to) in cuts.zipWithNext()) {
            val active = decorated.filter { it.start <= from && it.end >= to }.map {
                if (it.style == InlineStyle.UNDERLINE) TextDecoration.Underline else TextDecoration.LineThrough
            }.distinct()
            val start = (from + offset).coerceIn(0, text.length)
            val end = (to + offset).coerceIn(0, text.length)
            if (active.isNotEmpty() && end > start) builder.addStyle(SpanStyle(textDecoration = TextDecoration.combine(active)), start, end)
        }
    }
    return builder.toAnnotatedString()
}

@Composable
fun BlockTextField(
    block: Block,
    textStyle: TextStyle,
    placeholder: String,
    focus: FocusRequest?,
    onFocusConsumed: (Long) -> Unit,
    onTextChange: (text: String, selStart: Int, selEnd: Int) -> Unit,
    onSelectionChange: (start: Int, end: Int) -> Unit,
    onBackspaceAtStart: () -> Unit,
    modifier: Modifier = Modifier,
    richText: Boolean = true,
) {
    val colors = ImadTheme.colors
    var value by remember(block.id) { mutableStateOf(TextFieldValue(SENTINEL + block.text, TextRange(block.text.length + 1))) }
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }

    // The model changed underneath the field (undo, shortcut, merge): resynchronise.
    if (value.text.length != block.text.length + 1 || !value.text.endsWith(block.text)) {
        val caret = value.selection.start.coerceIn(1, block.text.length + 1)
        value = TextFieldValue(SENTINEL + block.text, TextRange(caret))
    }

    LaunchedEffect(focus?.nonce) {
        if (focus != null && focus.blockId == block.id) {
            runCatching { requester.requestFocus() }
            value = value.copy(selection = TextRange((focus.offset + 1).coerceIn(1, value.text.length)))
            onFocusConsumed(focus.nonce)
        }
    }

    val transformation = remember(block.spans, colors, richText) {
        VisualTransformation { text ->
            TransformedText(if (richText) annotate(text.text, block.spans, colors, offset = 1) else text, OffsetMapping.Identity)
        }
    }

    Box(modifier) {
        if (block.text.isEmpty() && placeholder.isNotEmpty()) {
            Text(placeholder, style = textStyle, color = colors.textTertiary, maxLines = 1)
        }
        BasicTextField(
            value = value,
            onValueChange = { incoming ->
                if (!incoming.text.startsWith(SENTINEL)) {
                    if (value.selection.collapsed) {
                        // Backspace at the very start: nothing but the sentinel was deleted.
                        if (incoming.text.length == value.text.length - 1) onBackspaceAtStart()
                        return@BasicTextField
                    }
                    // A selection that covered the sentinel was replaced; put it back.
                    val text = incoming.text.replace(SENTINEL, "")
                    val caret = incoming.selection.start.coerceIn(0, text.length)
                    value = TextFieldValue(SENTINEL + text, TextRange(caret + 1))
                    onTextChange(text, caret, caret)
                    return@BasicTextField
                }
                var next = incoming
                if (next.text.indexOf(SENTINEL, 1) >= 0) {
                    // Pasted text carried a stray sentinel.
                    val clean = SENTINEL + next.text.replace(SENTINEL, "")
                    next = TextFieldValue(clean, TextRange(next.selection.start.coerceAtMost(clean.length)))
                }
                val selection = TextRange(next.selection.start.coerceAtLeast(1), next.selection.end.coerceAtLeast(1))
                if (selection != next.selection) next = next.copy(selection = selection)
                value = next
                val text = next.text.substring(1)
                if (text != block.text) onTextChange(text, selection.min - 1, selection.max - 1)
                else if (focused) onSelectionChange(selection.min - 1, selection.max - 1)
            },
            modifier = Modifier.fillMaxWidth().focusRequester(requester).testTag("block-${block.id}")
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onSelectionChange(value.selection.min - 1, value.selection.max - 1)
                },
            textStyle = textStyle.copy(color = textStyle.color.takeOrElse { colors.text }),
            cursorBrush = SolidColor(colors.primary),
            visualTransformation = transformation,
            keyboardOptions = KeyboardOptions(capitalization = if (richText) KeyboardCapitalization.Sentences else KeyboardCapitalization.None),
        )
    }
}
