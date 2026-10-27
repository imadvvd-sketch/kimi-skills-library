package com.lumalab.core

import kotlin.math.abs
import kotlin.math.pow

internal object ColorMath {
    /** sRGB 8-bit value -> linear light. */
    val SRGB_TO_LINEAR = FloatArray(256) { i ->
        val c = i / 255f
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }

    private const val SRGB_LUT_SIZE = 4096
    private val SRGB_TO_LINEAR_FINE = FloatArray(SRGB_LUT_SIZE + 1) { i ->
        val c = i.toFloat() / SRGB_LUT_SIZE
        if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }

    /** Gamma-encoded value (clamped to 0..1) -> linear light. */
    fun srgbToLinear(v: Float): Float {
        val pos = clamp01(v) * SRGB_LUT_SIZE
        val i = pos.toInt()
        if (i >= SRGB_LUT_SIZE) return 1f
        return SRGB_TO_LINEAR_FINE[i] + (SRGB_TO_LINEAR_FINE[i + 1] - SRGB_TO_LINEAR_FINE[i]) * (pos - i)
    }

    private const val LINEAR_LUT_SIZE = 4096
    private val LINEAR_TO_SRGB = FloatArray(LINEAR_LUT_SIZE + 1) { i -> encodeExact(i.toFloat() / LINEAR_LUT_SIZE) }

    private fun encodeExact(l: Float): Float =
        if (l <= 0.0031308f) l * 12.92f else 1.055f * l.pow(1f / 2.4f) - 0.055f

    /** Linear light -> gamma-encoded value; values above 1 are encoded exactly so highlights can be recovered. */
    fun linearToSrgb(l: Float): Float = when {
        l <= 0f -> 0f
        l <= 1f -> {
            val pos = l * LINEAR_LUT_SIZE
            val i = pos.toInt()
            if (i >= LINEAR_LUT_SIZE) LINEAR_TO_SRGB[LINEAR_LUT_SIZE]
            else LINEAR_TO_SRGB[i] + (LINEAR_TO_SRGB[i + 1] - LINEAR_TO_SRGB[i]) * (pos - i)
        }
        else -> encodeExact(l)
    }

    fun luma(r: Float, g: Float, b: Float): Float = 0.2126f * r + 0.7152f * g + 0.0722f * b

    fun clamp01(v: Float): Float = if (v < 0f) 0f else if (v > 1f) 1f else v

    fun to8(v: Float): Int {
        val i = (v * 255f + 0.5f).toInt()
        return if (i < 0) 0 else if (i > 255) 255 else i
    }

    fun smoothstep(e0: Float, e1: Float, x: Float): Float {
        val t = clamp01((x - e0) / (e1 - e0))
        return t * t * (3f - 2f * t)
    }

    /** Writes hue (0..360), saturation and lightness into [out]. */
    fun rgbToHsl(r: Float, g: Float, b: Float, out: FloatArray) {
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val l = (max + min) / 2f
        val d = max - min
        if (d < 1e-6f) {
            out[0] = 0f; out[1] = 0f; out[2] = l
            return
        }
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        var h = when (max) {
            r -> (g - b) / d + (if (g < b) 6f else 0f)
            g -> (b - r) / d + 2f
            else -> (r - g) / d + 4f
        }
        h *= 60f
        out[0] = h; out[1] = s; out[2] = l
    }

    fun hslToRgb(h: Float, s: Float, l: Float, out: FloatArray) {
        if (s <= 0f) {
            out[0] = l; out[1] = l; out[2] = l
            return
        }
        val c = (1f - abs(2f * l - 1f)) * s
        val hp = ((h % 360f + 360f) % 360f) / 60f
        val x = c * (1f - abs(hp % 2f - 1f))
        val m = l - c / 2f
        val (r, g, b) = when (hp.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        out[0] = r + m; out[1] = g + m; out[2] = b + m
    }

    /** Fully saturated colour for a hue, used by colour grading and UI swatches. */
    fun hueColor(h: Float): FloatArray = FloatArray(3).also { hslToRgb(h, 1f, 0.5f, it) }
}
