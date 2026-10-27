package com.lumalab.photo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.lumalab.core.CurveChannel
import com.lumalab.core.CurvePoint
import com.lumalab.core.ToneCurveLut

/**
 * Interactive tone curve. Tap adds a point, drag moves the nearest point,
 * double-tap removes an interior point. Endpoints can only move vertically.
 */
@Composable
fun ToneCurveEditor(
    points: List<CurvePoint>,
    channel: CurveChannel,
    onChange: (List<CurvePoint>) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Holds the newest points, including edits made earlier in the same gesture before recomposition.
    val latest = remember { arrayOf(points) }
    latest[0] = points
    val onChangeState by rememberUpdatedState(onChange)
    fun change(list: List<CurvePoint>) {
        latest[0] = list
        onChangeState(list)
    }
    val commit by rememberUpdatedState(onCommit)
    var dragIndex by remember { mutableIntStateOf(-1) }
    val lineColor = when (channel) {
        CurveChannel.MASTER -> Color.White
        CurveChannel.RED -> Color(0xFFFF5A5A)
        CurveChannel.GREEN -> Color(0xFF5AE05A)
        CurveChannel.BLUE -> Color(0xFF5A9BFF)
    }

    fun toCurve(o: Offset, w: Float, h: Float) = CurvePoint((o.x / w).coerceIn(0f, 1f), (1f - o.y / h).coerceIn(0f, 1f))

    fun nearest(o: Offset, w: Float, h: Float, slop: Float): Int {
        var best = -1
        var bestD = slop
        latest[0].forEachIndexed { i, p ->
            val d = Offset(p.x * w, (1f - p.y) * h).distanceTo(o)
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }

    fun insert(p: CurvePoint): Int {
        val list = latest[0].toMutableList()
        val idx = list.indexOfFirst { it.x > p.x }.let { if (it < 0) list.size else it }
        if (list.any { kotlin.math.abs(it.x - p.x) < 0.02f }) return -1
        list.add(idx, p)
        change(list)
        return idx
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .aspectRatio(1.6f)
            .background(Color(0xFF181818))
            .pointerInput(channel) {
                detectTapGestures(
                    onTap = { o ->
                        val slop = 24.dp.toPx()
                        if (nearest(o, size.width.toFloat(), size.height.toFloat(), slop) < 0) {
                            val p = toCurve(o, size.width.toFloat(), size.height.toFloat())
                            val y = ToneCurveLut.evaluate(latest[0].sortedBy { it.x }, p.x)
                            if (insert(CurvePoint(p.x, y)) >= 0) commit()
                        }
                    },
                    onDoubleTap = { o ->
                        val i = nearest(o, size.width.toFloat(), size.height.toFloat(), 32.dp.toPx())
                        if (i > 0 && i < latest[0].size - 1) {
                            change(latest[0].filterIndexed { index, _ -> index != i })
                            commit()
                        }
                    },
                )
            }
            .pointerInput(channel) {
                detectDragGestures(
                    onDragStart = { o ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        dragIndex = nearest(o, w, h, 32.dp.toPx())
                        if (dragIndex < 0) dragIndex = insert(toCurve(o, w, h))
                    },
                    onDragEnd = { dragIndex = -1; commit() },
                    onDragCancel = { dragIndex = -1; commit() },
                    onDrag = { pointer, _ ->
                        val i = dragIndex
                        if (i < 0 || i >= latest[0].size) return@detectDragGestures
                        pointer.consume()
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val p = toCurve(pointer.position, w, h)
                        val list = latest[0].toMutableList()
                        val x = when (i) {
                            0 -> list[0].x
                            list.size - 1 -> list[i].x
                            else -> p.x.coerceIn(list[i - 1].x + 0.02f, list[i + 1].x - 0.02f)
                        }
                        list[i] = CurvePoint(x, p.y)
                        change(list)
                    },
                )
            },
    ) {
        val w = size.width
        val h = size.height
        val grid = Color(0xFF333333)
        for (k in 1..3) {
            drawLine(grid, Offset(w * k / 4f, 0f), Offset(w * k / 4f, h))
            drawLine(grid, Offset(0f, h * k / 4f), Offset(w, h * k / 4f))
        }
        drawLine(Color(0xFF444444), Offset(0f, h), Offset(w, 0f))
        val sorted = points.sortedBy { it.x }.distinctBy { it.x }
        val path = Path()
        val steps = 96
        for (s in 0..steps) {
            val x = s / steps.toFloat()
            val y = if (sorted.size >= 2) ToneCurveLut.evaluate(sorted, x).coerceIn(0f, 1f) else x
            if (s == 0) path.moveTo(x * w, (1f - y) * h) else path.lineTo(x * w, (1f - y) * h)
        }
        drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx()))
        sorted.forEach { p ->
            drawCircle(Color.Black, radius = 7.dp.toPx(), center = Offset(p.x * w, (1f - p.y) * h))
            drawCircle(lineColor, radius = 5.dp.toPx(), center = Offset(p.x * w, (1f - p.y) * h))
        }
    }
}
