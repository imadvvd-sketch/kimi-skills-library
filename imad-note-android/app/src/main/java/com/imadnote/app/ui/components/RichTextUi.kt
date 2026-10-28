package com.imadnote.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import com.imadnote.app.domain.RichText
import com.imadnote.app.domain.RichText.Kind

/** Colours and sizes used to render rich text; taken from the theme. */
data class RichTextStyle(
    val marker: Color,
    val secondary: Color,
    val accent: Color,
    val headingSize: TextUnit,
    val subheadingSize: TextUnit,
)

@Composable
fun rememberRichTextStyle(): RichTextStyle {
    val scheme = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    return remember(scheme, type) {
        RichTextStyle(
            marker = scheme.outline,
            secondary = scheme.onSurfaceVariant,
            accent = scheme.secondary,
            headingSize = type.titleLarge.fontSize,
            subheadingSize = type.titleMedium.fontSize,
        )
    }
}

private fun RichTextStyle.spanFor(kind: Kind): SpanStyle? = when (kind) {
    Kind.HEADING -> SpanStyle(fontSize = headingSize, fontWeight = FontWeight.SemiBold)
    Kind.SUBHEADING -> SpanStyle(fontSize = subheadingSize, fontWeight = FontWeight.SemiBold)
    Kind.QUOTE -> SpanStyle(color = secondary, fontStyle = FontStyle.Italic)
    Kind.BOLD -> SpanStyle(fontWeight = FontWeight.SemiBold)
    Kind.ITALIC -> SpanStyle(fontStyle = FontStyle.Italic)
    Kind.BULLET, Kind.NUMBER -> SpanStyle(color = accent, fontWeight = FontWeight.SemiBold)
    Kind.MARKER -> SpanStyle(color = marker.copy(alpha = 0.6f))
}

/**
 * Styles the editor text in place. Text length never changes ("- " is shown as "• "),
 * so the identity offset mapping keeps the caret exactly where the user expects.
 */
class RichTextTransformation(private val style: RichTextStyle) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val spans = RichText.parse(raw)
        val shown = StringBuilder(raw)
        spans.filter { it.kind == Kind.BULLET }.forEach { shown.setCharAt(it.start, '•') }
        val out = buildAnnotatedString {
            append(shown.toString())
            spans.forEach { s ->
                val spanStyle = style.spanFor(s.kind) ?: return@forEach
                if (s.end > s.start) addStyle(spanStyle, s.start, s.end)
            }
        }
        return TransformedText(out, OffsetMapping.Identity)
    }
}

/** Preview text for cards and lists: markers hidden, styles kept. */
fun richPreview(text: String, style: RichTextStyle): AnnotatedString = buildAnnotatedString {
    RichText.visibleRuns(text).forEach { run ->
        val styles = run.kinds.mapNotNull { k -> if (k == Kind.HEADING || k == Kind.SUBHEADING) SpanStyle(fontWeight = FontWeight.SemiBold) else style.spanFor(k) }
        if (styles.isEmpty()) {
            append(run.text)
        } else {
            val merged = styles.reduce { a, b -> a.merge(b) }
            withStyle(merged) { append(run.text) }
        }
    }
}
