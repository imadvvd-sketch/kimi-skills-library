package app.ghars.core.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign

/**
 * The ring shared by the duration slider and the session progress: a soft track with a brand arc
 * starting at 12 o'clock and running clockwise. Never mirrored in RTL, like a clock.
 */
fun DrawScope.drawRing(
    track: Color, arc: Color, strokePx: Float, insetPx: Float,
    trackSweep: Float, arcSweep: Float,
) {
    val diameter = size.minDimension - 2 * insetPx
    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
    val box = Size(diameter, diameter)
    val stroke = Stroke(strokePx, cap = StrokeCap.Round)
    drawArc(track, -90f, trackSweep, useCenter = false, topLeft = topLeft, size = box, style = stroke)
    if (arcSweep > 0f) {
        drawArc(arc, -90f, arcSweep, useCenter = false, topLeft = topLeft, size = box, style = stroke)
    }
}

/**
 * One line of text that shrinks to fit its width instead of clipping or wrapping.
 * Needed for the 72sp countdown at large system font sizes.
 *
 * @param widest a sample at least as wide as any value shown, so the size does not jump per digit.
 */
@Composable
fun FitText(text: String, widest: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val measurer = rememberTextMeasurer()
        val available = constraints.maxWidth
        val fitted = remember(widest, style, available) {
            val needed = measurer.measure(widest, style, softWrap = false, maxLines = 1).size.width
            if (needed <= available || needed == 0) style
            else {
                val scale = available.toFloat() / needed
                style.copy(fontSize = style.fontSize * scale, lineHeight = style.lineHeight * scale)
            }
        }
        Text(text = text, style = fitted, color = color, maxLines = 1, softWrap = false, textAlign = TextAlign.Center)
    }
}
