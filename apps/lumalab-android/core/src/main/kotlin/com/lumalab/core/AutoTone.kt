package com.lumalab.core

import kotlin.math.ln

/** Derives a balanced starting point from image statistics, like an "Auto" button. */
object AutoTone {
    fun compute(img: ImageBuffer, base: EditSettings = EditSettings.DEFAULT): EditSettings {
        val sample = img.downscaled(400)
        val hist = Histogram.of(sample)
        val total = sample.pixels.size
        val low = percentile(hist.luma, total, 0.005f)
        val high = percentile(hist.luma, total, 0.995f)
        var linearSum = 0.0
        var rs = 0.0; var gs = 0.0; var bs = 0.0
        for (p in sample.pixels) {
            val r = ColorMath.SRGB_TO_LINEAR[(p shr 16) and 0xFF]
            val g = ColorMath.SRGB_TO_LINEAR[(p shr 8) and 0xFF]
            val b = ColorMath.SRGB_TO_LINEAR[p and 0xFF]
            linearSum += ColorMath.luma(r, g, b)
            rs += r; gs += g; bs += b
        }
        val meanLinear = (linearSum / total).coerceAtLeast(1e-4)
        // Aim for a mid-grey average (18% reflectance).
        val exposure = (ln(0.18 / meanLinear) / ln(2.0)).toFloat().coerceIn(-2f, 2f) * 0.8f
        val spread = (high - low).coerceAtLeast(1)
        val contrast = ((180f - spread) / 180f * 40f).coerceIn(-20f, 35f)
        val blacks = (-low / 255f * 120f).coerceIn(-40f, 10f)
        val whites = ((255 - high) / 255f * 120f).coerceIn(-10f, 40f)
        val highlights = if (high >= 250) -35f else -10f
        val shadows = if (low < 20) 30f else 10f
        // Grey-world white balance estimate.
        val avg = (rs + gs + bs) / 3
        val temperature = (((bs - rs) / avg) * 60).toFloat().coerceIn(-30f, 30f)
        val tint = (((gs - avg) / avg) * 80).toFloat().coerceIn(-20f, 20f)
        return base.copy(
            exposure = exposure,
            contrast = contrast,
            highlights = highlights,
            shadows = shadows,
            whites = whites,
            blacks = blacks,
            temperature = temperature,
            tint = tint,
            vibrance = maxOf(base.vibrance, 15f),
        )
    }

    private fun percentile(bins: IntArray, total: Int, fraction: Float): Int {
        val target = (total * fraction).toLong()
        var acc = 0L
        for (i in bins.indices) {
            acc += bins[i]
            if (acc >= target) return i
        }
        return 255
    }
}
