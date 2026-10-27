package com.lumalab.core

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

object Geometry {
    /** Applies orientation, straightening and (optionally) the crop rectangle. */
    fun apply(src: ImageBuffer, crop: CropSettings, includeCropRect: Boolean = true): ImageBuffer {
        var img = orient(src, crop.quarterTurns, crop.flipHorizontal, crop.flipVertical)
        if (crop.angle != 0f) img = straighten(img, crop.angle)
        if (includeCropRect && crop.hasCrop) img = cropRect(img, crop)
        return img
    }

    /** Size of the frame the crop rectangle is expressed in (after orientation). */
    fun orientedSize(width: Int, height: Int, quarterTurns: Int): Pair<Int, Int> =
        if (((quarterTurns % 4) + 4) % 4 % 2 == 1) height to width else width to height

    fun orient(src: ImageBuffer, quarterTurns: Int, flipH: Boolean, flipV: Boolean): ImageBuffer {
        val turns = ((quarterTurns % 4) + 4) % 4
        if (turns == 0 && !flipH && !flipV) return src
        val sw = src.width
        val sh = src.height
        val (dw, dh) = orientedSize(sw, sh, turns)
        val out = IntArray(dw * dh)
        val sp = src.pixels
        parallelFor(dh) { y ->
            for (x in 0 until dw) {
                val fx = if (flipH) dw - 1 - x else x
                val fy = if (flipV) dh - 1 - y else y
                // Map destination (fx, fy) back to the source for a clockwise rotation.
                val (sx, sy) = when (turns) {
                    1 -> fy to (sh - 1 - fx)
                    2 -> (sw - 1 - fx) to (sh - 1 - fy)
                    3 -> (sw - 1 - fy) to fx
                    else -> fx to fy
                }
                out[y * dw + x] = sp[sy * sw + sx]
            }
        }
        return ImageBuffer(dw, dh, out)
    }

    /** Scale needed so a frame rotated by [degrees] still fills the original rectangle. */
    fun straightenScale(width: Int, height: Int, degrees: Float): Float {
        val t = Math.toRadians(abs(degrees).toDouble())
        val c = cos(t).toFloat()
        val s = sin(t).toFloat()
        val w = width.toFloat()
        val h = height.toFloat()
        return maxOf(c + (h / w) * s, (w / h) * s + c)
    }

    fun straighten(src: ImageBuffer, degrees: Float): ImageBuffer {
        val w = src.width
        val h = src.height
        val scale = straightenScale(w, h, degrees)
        val rad = Math.toRadians(degrees.toDouble())
        val cosA = (cos(rad) / scale).toFloat()
        val sinA = (sin(rad) / scale).toFloat()
        val cx = (w - 1) / 2f
        val cy = (h - 1) / 2f
        val sp = src.pixels
        val out = IntArray(w * h)
        parallelFor(h) { y ->
            val dy = y - cy
            for (x in 0 until w) {
                val dx = x - cx
                // Inverse rotation: destination -> source.
                val sx = cosA * dx + sinA * dy + cx
                val sy = -sinA * dx + cosA * dy + cy
                out[y * w + x] = sampleBilinear(sp, w, h, sx, sy)
            }
        }
        return ImageBuffer(w, h, out)
    }

    fun cropRect(src: ImageBuffer, crop: CropSettings): ImageBuffer {
        val l = (crop.left.coerceIn(0f, 1f) * src.width).toInt()
        val t = (crop.top.coerceIn(0f, 1f) * src.height).toInt()
        val r = (crop.right.coerceIn(0f, 1f) * src.width).toInt().coerceAtLeast(l + 1).coerceAtMost(src.width)
        val b = (crop.bottom.coerceIn(0f, 1f) * src.height).toInt().coerceAtLeast(t + 1).coerceAtMost(src.height)
        val w = r - l
        val h = b - t
        if (w == src.width && h == src.height) return src
        val out = IntArray(w * h)
        for (y in 0 until h) System.arraycopy(src.pixels, (t + y) * src.width + l, out, y * w, w)
        return ImageBuffer(w, h, out)
    }

    private fun sampleBilinear(p: IntArray, w: Int, h: Int, x: Float, y: Float): Int {
        val xc = x.coerceIn(0f, (w - 1).toFloat())
        val yc = y.coerceIn(0f, (h - 1).toFloat())
        val x0 = floor(xc).toInt()
        val y0 = floor(yc).toInt()
        val x1 = minOf(x0 + 1, w - 1)
        val y1 = minOf(y0 + 1, h - 1)
        val fx = xc - x0
        val fy = yc - y0
        val a = p[y0 * w + x0]
        val b = p[y0 * w + x1]
        val c = p[y1 * w + x0]
        val d = p[y1 * w + x1]
        fun ch(shift: Int): Int {
            val top = ((a shr shift) and 0xFF) * (1 - fx) + ((b shr shift) and 0xFF) * fx
            val bot = ((c shr shift) and 0xFF) * (1 - fx) + ((d shr shift) and 0xFF) * fx
            return (top * (1 - fy) + bot * fy + 0.5f).toInt().coerceIn(0, 255)
        }
        return ImageBuffer.argb(ch(16), ch(8), ch(0))
    }

    /**
     * Largest crop of the given pixel aspect ratio, centred inside a frame of [frameW] x [frameH],
     * returned as normalised (left, top, right, bottom).
     */
    fun centeredAspectRect(frameW: Int, frameH: Int, aspect: Float): FloatArray {
        if (aspect <= 0f) return floatArrayOf(0f, 0f, 1f, 1f)
        val frameAspect = frameW.toFloat() / frameH
        return if (aspect > frameAspect) {
            val nh = frameAspect / aspect
            floatArrayOf(0f, (1f - nh) / 2f, 1f, (1f + nh) / 2f)
        } else {
            val nw = aspect / frameAspect
            floatArrayOf((1f - nw) / 2f, 0f, (1f + nw) / 2f, 1f)
        }
    }
}
