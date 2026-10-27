package com.lumalab.photo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Normalised crop rectangle (0..1 in both axes). */
data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float)

private enum class Handle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, LEFT, TOP, RIGHT, BOTTOM, MOVE }

private const val MIN_SIZE = 0.08f

/**
 * Crop frame drawn over the full (uncropped) image area that this composable covers.
 * [pixelAspect] is the crop's width/height in pixels (0 = free); [frameAspect] is the image's.
 */
@Composable
fun CropOverlay(
    rect: CropRect,
    pixelAspect: Float,
    frameAspect: Float,
    onChange: (CropRect) -> Unit,
    onDragStart: () -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latest = remember { arrayOf(rect) }
    latest[0] = rect
    val change by rememberUpdatedState(onChange)
    val start by rememberUpdatedState(onDragStart)
    val commit by rememberUpdatedState(onCommit)
    val aspect by rememberUpdatedState(pixelAspect)
    val frame by rememberUpdatedState(frameAspect)
    val handleHolder = remember { arrayOf(Handle.MOVE) }

    Canvas(
        modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { o ->
                    val r = latest[0]
                    val w = size.width.toFloat()
                    val h = size.height.toFloat()
                    val slop = 36.dp.toPx()
                    val l = r.left * w; val t = r.top * h; val rr = r.right * w; val b = r.bottom * h
                    fun near(x: Float, y: Float) = Offset(x, y).distanceTo(o) < slop
                    handleHolder[0] = when {
                        near(l, t) -> Handle.TOP_LEFT
                        near(rr, t) -> Handle.TOP_RIGHT
                        near(l, b) -> Handle.BOTTOM_LEFT
                        near(rr, b) -> Handle.BOTTOM_RIGHT
                        abs(o.x - l) < slop && o.y in t..b -> Handle.LEFT
                        abs(o.x - rr) < slop && o.y in t..b -> Handle.RIGHT
                        abs(o.y - t) < slop && o.x in l..rr -> Handle.TOP
                        abs(o.y - b) < slop && o.x in l..rr -> Handle.BOTTOM
                        else -> Handle.MOVE
                    }
                    start()
                },
                onDragEnd = { commit() },
                onDragCancel = { commit() },
                onDrag = { pointer, drag ->
                    pointer.consume()
                    val dx = drag.x / size.width
                    val dy = drag.y / size.height
                    val next = adjust(latest[0], handleHolder[0], dx, dy, if (aspect > 0f) aspect / frame else 0f)
                    latest[0] = next
                    change(next)
                },
            )
        },
    ) {
        val r = Rect(rect.left * size.width, rect.top * size.height, rect.right * size.width, rect.bottom * size.height)
        val shade = Color.Black.copy(alpha = 0.6f)
        drawRect(shade, Offset.Zero, Size(size.width, r.top))
        drawRect(shade, Offset(0f, r.bottom), Size(size.width, size.height - r.bottom))
        drawRect(shade, Offset(0f, r.top), Size(r.left, r.height))
        drawRect(shade, Offset(r.right, r.top), Size(size.width - r.right, r.height))
        val thin = Color.White.copy(alpha = 0.45f)
        for (k in 1..2) {
            drawLine(thin, Offset(r.left + r.width * k / 3f, r.top), Offset(r.left + r.width * k / 3f, r.bottom))
            drawLine(thin, Offset(r.left, r.top + r.height * k / 3f), Offset(r.right, r.top + r.height * k / 3f))
        }
        drawRect(Color.White, r.topLeft, r.size, style = Stroke(1.5.dp.toPx()))
        val len = 18.dp.toPx()
        val stroke = 4.dp.toPx()
        listOf(
            r.topLeft to Offset(1f, 1f), Offset(r.right, r.top) to Offset(-1f, 1f),
            Offset(r.left, r.bottom) to Offset(1f, -1f), r.bottomRight to Offset(-1f, -1f),
        ).forEach { (c, dir) ->
            drawLine(Color.White, c, Offset(c.x + len * dir.x, c.y), stroke)
            drawLine(Color.White, c, Offset(c.x, c.y + len * dir.y), stroke)
        }
    }
}

/** [normAspect] is width/height in normalised units (0 = free). */
private fun adjust(r: CropRect, handle: Handle, dx: Float, dy: Float, normAspect: Float): CropRect {
    if (handle == Handle.MOVE) {
        val w = r.right - r.left
        val h = r.bottom - r.top
        val l = (r.left + dx).coerceIn(0f, 1f - w)
        val t = (r.top + dy).coerceIn(0f, 1f - h)
        return CropRect(l, t, l + w, t + h)
    }
    var l = r.left; var t = r.top; var rr = r.right; var b = r.bottom
    when (handle) {
        Handle.TOP_LEFT -> { l += dx; t += dy }
        Handle.TOP_RIGHT -> { rr += dx; t += dy }
        Handle.BOTTOM_LEFT -> { l += dx; b += dy }
        Handle.BOTTOM_RIGHT -> { rr += dx; b += dy }
        Handle.LEFT -> l += dx
        Handle.RIGHT -> rr += dx
        Handle.TOP -> t += dy
        Handle.BOTTOM -> b += dy
        Handle.MOVE -> Unit
    }
    l = l.coerceIn(0f, r.right - MIN_SIZE)
    rr = rr.coerceIn(r.left + MIN_SIZE, 1f)
    t = t.coerceIn(0f, r.bottom - MIN_SIZE)
    b = b.coerceIn(r.top + MIN_SIZE, 1f)
    if (normAspect <= 0f) return CropRect(l, t, rr, b)

    // Keep the aspect ratio: width drives height, anchored at the opposite side or corner.
    var w = rr - l
    var h = w / normAspect
    val anchorTop = handle in setOf(Handle.BOTTOM_LEFT, Handle.BOTTOM_RIGHT, Handle.LEFT, Handle.RIGHT, Handle.BOTTOM)
    if (handle == Handle.TOP || handle == Handle.BOTTOM) {
        h = b - t
        w = h * normAspect
    }
    val maxH = if (anchorTop) 1f - r.top else r.bottom
    if (h > maxH) { h = maxH; w = h * normAspect }
    val anchorLeft = handle in setOf(Handle.TOP_RIGHT, Handle.BOTTOM_RIGHT, Handle.RIGHT, Handle.TOP, Handle.BOTTOM)
    val maxW = if (anchorLeft) 1f - r.left else r.right
    if (w > maxW) { w = maxW; h = w / normAspect }
    val newL = if (anchorLeft) r.left else r.right - w
    val newT = if (anchorTop) r.top else r.bottom - h
    return CropRect(newL, newT, newL + w, newT + h)
}
