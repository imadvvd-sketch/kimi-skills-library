package com.lumalab.core

/** Separable box blur on single-channel float planes; repeated passes approximate a Gaussian. */
internal object Blur {
    fun blur(src: FloatArray, width: Int, height: Int, radius: Int, passes: Int = 3): FloatArray {
        if (radius < 1) return src.copyOf()
        var a = src.copyOf()
        val tmp = FloatArray(src.size)
        repeat(passes) {
            horizontal(a, tmp, width, height, radius)
            vertical(tmp, a, width, height, radius)
        }
        return a
    }

    private fun horizontal(src: FloatArray, dst: FloatArray, w: Int, h: Int, r: Int) {
        val norm = 1f / (2 * r + 1)
        parallelFor(h) { y ->
            val row = y * w
            var sum = 0f
            for (i in -r..r) sum += src[row + i.coerceIn(0, w - 1)]
            for (x in 0 until w) {
                dst[row + x] = sum * norm
                val add = (x + r + 1).coerceAtMost(w - 1)
                val sub = (x - r).coerceAtLeast(0)
                sum += src[row + add] - src[row + sub]
            }
        }
    }

    private fun vertical(src: FloatArray, dst: FloatArray, w: Int, h: Int, r: Int) {
        val norm = 1f / (2 * r + 1)
        parallelFor(w) { x ->
            var sum = 0f
            for (i in -r..r) sum += src[i.coerceIn(0, h - 1) * w + x]
            for (y in 0 until h) {
                dst[y * w + x] = sum * norm
                val add = (y + r + 1).coerceAtMost(h - 1)
                val sub = (y - r).coerceAtLeast(0)
                sum += src[add * w + x] - src[sub * w + x]
            }
        }
    }
}
