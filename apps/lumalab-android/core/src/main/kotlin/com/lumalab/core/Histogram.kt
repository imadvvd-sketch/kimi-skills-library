package com.lumalab.core

/** 256-bin channel histograms of an image. */
class Histogram(val red: IntArray, val green: IntArray, val blue: IntArray, val luma: IntArray) {
    val max: Int get() = maxOf(red.max(), green.max(), blue.max())

    /** Fraction of pixels clipped to pure black / pure white in any channel. */
    fun clipping(total: Int): Pair<Float, Float> {
        val low = maxOf(red[0], green[0], blue[0]).toFloat() / total
        val high = maxOf(red[255], green[255], blue[255]).toFloat() / total
        return low to high
    }

    companion object {
        fun of(img: ImageBuffer, step: Int = 1): Histogram {
            val r = IntArray(256)
            val g = IntArray(256)
            val b = IntArray(256)
            val l = IntArray(256)
            val p = img.pixels
            var i = 0
            while (i < p.size) {
                val c = p[i]
                val rr = (c shr 16) and 0xFF
                val gg = (c shr 8) and 0xFF
                val bb = c and 0xFF
                r[rr]++; g[gg]++; b[bb]++
                l[(0.2126f * rr + 0.7152f * gg + 0.0722f * bb).toInt().coerceIn(0, 255)]++
                i += step
            }
            return Histogram(r, g, b, l)
        }
    }
}
