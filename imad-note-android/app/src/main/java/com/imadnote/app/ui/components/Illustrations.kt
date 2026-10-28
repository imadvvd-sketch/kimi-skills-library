package com.imadnote.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.imadnote.app.ui.theme.AppTheme

/** Empty/error state artwork, drawn in code so it follows the theme. See DESIGN_SYSTEM.md §9. */
enum class Illustration { NOTES, TASKS, CALENDAR, PROJECTS, SEARCH, ARCHIVE, ALL_CLEAR, ERROR, PLANT }

private class Palette(val line: Color, val soft: Color, val paper: Color, val accent: Color, val alert: Color)

@Composable
fun IllustrationArt(kind: Illustration, modifier: Modifier = Modifier, size: Dp = 120.dp) {
    val scheme = MaterialTheme.colorScheme
    val palette = Palette(
        line = scheme.onSurfaceVariant.copy(alpha = 0.75f),
        soft = scheme.secondaryContainer,
        paper = scheme.surface,
        accent = AppTheme.extended.accent,
        alert = scheme.error,
    )
    // Decorative: the title next to it carries the meaning, so no content description.
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 120f
        val p = palette
        when (kind) {
            Illustration.NOTES -> notes(p, u)
            Illustration.TASKS -> tasks(p, u)
            Illustration.CALENDAR -> calendar(p, u)
            Illustration.PROJECTS -> projects(p, u)
            Illustration.SEARCH -> search(p, u)
            Illustration.ARCHIVE -> archive(p, u)
            Illustration.ALL_CLEAR -> allClear(p, u)
            Illustration.ERROR -> error(p, u)
            Illustration.PLANT -> plant(p, u)
        }
    }
}

private fun DrawScope.stroke(u: Float) = Stroke(width = 2.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)

/** The brand sheet: rounded rectangle with a folded top-end corner showing the p.accent. */
private fun DrawScope.sheet(p: Palette, u: Float, x: Float, y: Float, w: Float, h: Float, fill: Color, fold: Boolean) {
    val r = 8f * u
    val f = if (fold) 14f * u else 0f
    val path = Path().apply {
        moveTo(x + r, y)
        lineTo(x + w - f - (if (fold) 0f else r), y)
        if (fold) lineTo(x + w, y + f) else quadraticBezierTo(x + w, y, x + w, y + r)
        lineTo(x + w, y + h - r)
        quadraticBezierTo(x + w, y + h, x + w - r, y + h)
        lineTo(x + r, y + h)
        quadraticBezierTo(x, y + h, x, y + h - r)
        lineTo(x, y + r)
        quadraticBezierTo(x, y, x + r, y)
        close()
    }
    drawPath(path, fill)
    drawPath(path, p.line, style = stroke(u))
    if (fold) {
        val corner = Path().apply {
            moveTo(x + w - f, y)
            lineTo(x + w, y + f)
            lineTo(x + w - f, y + f)
            close()
        }
        drawPath(corner, p.accent)
        drawPath(corner, p.line, style = stroke(u))
    }
}

private fun DrawScope.textLine(p: Palette, u: Float, x: Float, y: Float, w: Float, color: Color = p.line.copy(alpha = 0.45f)) {
    drawLine(color, Offset(x, y), Offset(x + w, y), strokeWidth = 4f * u, cap = StrokeCap.Round)
}

private fun DrawScope.ground(p: Palette, u: Float) {
    drawOval(p.soft.copy(alpha = 0.6f), topLeft = Offset(18f * u, 100f * u), size = Size(84f * u, 10f * u))
}

private fun DrawScope.notes(p: Palette, u: Float) {
    ground(p, u)
    sheet(p, u, 44f * u, 14f * u, 54f * u, 70f * u, p.soft, fold = false)
    sheet(p, u, 24f * u, 26f * u, 56f * u, 72f * u, p.paper, fold = true)
    textLine(p, u, 34f * u, 46f * u, 26f * u)
    textLine(p, u, 34f * u, 58f * u, 34f * u)
    textLine(p, u, 34f * u, 70f * u, 20f * u)
}

private fun DrawScope.tasks(p: Palette, u: Float) {
    ground(p, u)
    sheet(p, u, 28f * u, 14f * u, 64f * u, 84f * u, p.paper, fold = true)
    listOf(36f, 56f, 76f).forEachIndexed { i, y ->
        val cy = y * u
        val cx = 42f * u
        if (i == 0) {
            drawCircle(p.soft, 6f * u, Offset(cx, cy))
            drawCircle(p.line, 6f * u, Offset(cx, cy), style = stroke(u))
            val check = Path().apply {
                moveTo(cx - 3f * u, cy)
                lineTo(cx - 0.5f * u, cy + 2.5f * u)
                lineTo(cx + 3.5f * u, cy - 2.5f * u)
            }
            drawPath(check, p.line, style = stroke(u))
        } else {
            drawCircle(p.line, 6f * u, Offset(cx, cy), style = stroke(u))
        }
        textLine(p, u, 54f * u, cy, if (i == 1) 26f * u else 18f * u)
    }
}

private fun DrawScope.calendar(p: Palette, u: Float) {
    ground(p, u)
    val x = 22f * u
    val y = 20f * u
    val w = 76f * u
    val h = 76f * u
    drawRoundRect(p.paper, Offset(x, y), Size(w, h), CornerRadius(10f * u))
    drawRoundRect(p.soft, Offset(x, y), Size(w, 18f * u), CornerRadius(10f * u))
    drawRect(p.soft, Offset(x, y + 9f * u), Size(w, 9f * u))
    drawRoundRect(p.line, Offset(x, y), Size(w, h), CornerRadius(10f * u), style = stroke(u))
    drawLine(p.line, Offset(40f * u, 14f * u), Offset(40f * u, 26f * u), 2.5f * u, StrokeCap.Round)
    drawLine(p.line, Offset(80f * u, 14f * u), Offset(80f * u, 26f * u), 2.5f * u, StrokeCap.Round)
    for (row in 0 until 3) for (col in 0 until 4) {
        val c = Offset((34f + col * 17f) * u, (52f + row * 15f) * u)
        if (row == 1 && col == 2) drawCircle(p.accent, 5.5f * u, c) else drawCircle(p.line.copy(alpha = 0.35f), 2.5f * u, c)
    }
}

private fun DrawScope.projects(p: Palette, u: Float) {
    ground(p, u)
    sheet(p, u, 36f * u, 12f * u, 58f * u, 62f * u, p.soft, fold = false)
    sheet(p, u, 24f * u, 24f * u, 62f * u, 72f * u, p.paper, fold = true)
    textLine(p, u, 34f * u, 44f * u, 30f * u)
    textLine(p, u, 34f * u, 56f * u, 22f * u)
    drawRoundRect(p.soft, Offset(34f * u, 76f * u), Size(42f * u, 6f * u), CornerRadius(3f * u))
    drawRoundRect(p.line, Offset(34f * u, 76f * u), Size(26f * u, 6f * u), CornerRadius(3f * u))
}

private fun DrawScope.search(p: Palette, u: Float) {
    ground(p, u)
    sheet(p, u, 22f * u, 16f * u, 58f * u, 76f * u, p.paper, fold = true)
    textLine(p, u, 32f * u, 38f * u, 28f * u)
    textLine(p, u, 32f * u, 50f * u, 20f * u)
    val c = Offset(72f * u, 70f * u)
    drawCircle(p.soft.copy(alpha = 0.9f), 16f * u, c)
    drawCircle(p.line, 16f * u, c, style = stroke(u))
    drawLine(p.line, Offset(84f * u, 82f * u), Offset(96f * u, 94f * u), 5f * u, StrokeCap.Round)
}

private fun DrawScope.archive(p: Palette, u: Float) {
    ground(p, u)
    drawRoundRect(p.paper, Offset(26f * u, 44f * u), Size(68f * u, 52f * u), CornerRadius(8f * u))
    drawRoundRect(p.line, Offset(26f * u, 44f * u), Size(68f * u, 52f * u), CornerRadius(8f * u), style = stroke(u))
    drawRoundRect(p.soft, Offset(20f * u, 30f * u), Size(80f * u, 16f * u), CornerRadius(6f * u))
    drawRoundRect(p.line, Offset(20f * u, 30f * u), Size(80f * u, 16f * u), CornerRadius(6f * u), style = stroke(u))
    drawLine(p.line, Offset(50f * u, 60f * u), Offset(70f * u, 60f * u), 4f * u, StrokeCap.Round)
    sheet(p, u, 46f * u, 14f * u, 28f * u, 22f * u, p.paper, fold = true)
}

private fun DrawScope.allClear(p: Palette, u: Float) {
    // A calm sun resting on the horizon.
    val center = Offset(60f * u, 74f * u)
    drawArc(p.accent.copy(alpha = 0.9f), 180f, 180f, true, Offset(center.x - 26f * u, center.y - 26f * u), Size(52f * u, 52f * u))
    listOf(-60f, -90f, -120f, -30f, -150f).forEach { deg ->
        val rad = Math.toRadians(deg.toDouble())
        val inner = Offset(center.x + (34f * u * kotlin.math.cos(rad)).toFloat(), center.y + (34f * u * kotlin.math.sin(rad)).toFloat())
        val outer = Offset(center.x + (44f * u * kotlin.math.cos(rad)).toFloat(), center.y + (44f * u * kotlin.math.sin(rad)).toFloat())
        drawLine(p.line.copy(alpha = 0.55f), inner, outer, 2.5f * u, StrokeCap.Round)
    }
    drawLine(p.line, Offset(16f * u, 74f * u), Offset(104f * u, 74f * u), 2.5f * u, StrokeCap.Round)
    drawLine(p.line.copy(alpha = 0.4f), Offset(32f * u, 86f * u), Offset(88f * u, 86f * u), 2.5f * u, StrokeCap.Round)
    drawLine(p.line.copy(alpha = 0.25f), Offset(46f * u, 96f * u), Offset(74f * u, 96f * u), 2.5f * u, StrokeCap.Round)
}

private fun DrawScope.error(p: Palette, u: Float) {
    ground(p, u)
    sheet(p, u, 30f * u, 16f * u, 60f * u, 78f * u, p.paper, fold = false)
    drawLine(p.alert, Offset(60f * u, 36f * u), Offset(60f * u, 62f * u), 6f * u, StrokeCap.Round)
    drawCircle(p.alert, 3.5f * u, Offset(60f * u, 76f * u))
}

/** A young plant: "start something new" — used when there are no notes yet. */
private fun DrawScope.plant(p: Palette, u: Float) {
    drawCircle(p.soft.copy(alpha = 0.55f), 50f * u, Offset(60f * u, 58f * u))
    val leaf = { cx: Float, cy: Float, angle: Float, len: Float ->
        rotate(angle, Offset(cx, cy)) {
            val path = Path().apply {
                moveTo(cx, cy)
                quadraticBezierTo(cx + len * 0.45f, cy - len * 0.32f, cx + len, cy)
                quadraticBezierTo(cx + len * 0.45f, cy + len * 0.32f, cx, cy)
                close()
            }
            drawPath(path, p.paper)
            drawPath(path, p.line, style = stroke(u))
            drawLine(p.line.copy(alpha = 0.5f), Offset(cx + len * 0.15f, cy), Offset(cx + len * 0.8f, cy), 1.5f * u, StrokeCap.Round)
        }
    }
    drawLine(p.line, Offset(60f * u, 78f * u), Offset(60f * u, 30f * u), 2.5f * u, StrokeCap.Round)
    leaf(60f * u, 64f * u, -150f, 26f * u)
    leaf(60f * u, 56f * u, -30f, 26f * u)
    leaf(60f * u, 44f * u, -140f, 20f * u)
    leaf(60f * u, 38f * u, -45f, 18f * u)
    drawCircle(p.accent, 3.5f * u, Offset(60f * u, 28f * u))
    // Pot
    val pot = Path().apply {
        moveTo(44f * u, 78f * u)
        lineTo(76f * u, 78f * u)
        lineTo(72f * u, 100f * u)
        lineTo(48f * u, 100f * u)
        close()
    }
    drawPath(pot, p.soft)
    drawPath(pot, p.line, style = stroke(u))
    drawLine(p.line, Offset(42f * u, 78f * u), Offset(78f * u, 78f * u), 3f * u, StrokeCap.Round)
}
