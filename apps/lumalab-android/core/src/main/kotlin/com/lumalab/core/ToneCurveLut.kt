package com.lumalab.core

/**
 * Monotone cubic (Fritsch–Carlson) interpolation of tone-curve control points,
 * sampled into a lookup table over 0..1.
 */
object ToneCurveLut {
    const val SIZE = 1024

    fun build(points: List<CurvePoint>): FloatArray? {
        if (ToneCurveSettings.isIdentityCurve(points)) return null
        val pts = points.sortedBy { it.x }.distinctBy { it.x }
        val lut = FloatArray(SIZE)
        if (pts.size == 1) {
            lut.fill(pts[0].y.coerceIn(0f, 1f))
            return lut
        }
        for (i in 0 until SIZE) {
            lut[i] = evaluate(pts, i / (SIZE - 1f)).coerceIn(0f, 1f)
        }
        return lut
    }

    /** Evaluates the curve at [x]; points must be sorted by x with unique x values. */
    fun evaluate(pts: List<CurvePoint>, x: Float): Float {
        val n = pts.size
        if (x <= pts[0].x) return pts[0].y
        if (x >= pts[n - 1].x) return pts[n - 1].y
        val dx = FloatArray(n - 1) { pts[it + 1].x - pts[it].x }
        val slope = FloatArray(n - 1) { (pts[it + 1].y - pts[it].y) / dx[it] }
        val tangent = FloatArray(n)
        tangent[0] = slope[0]
        tangent[n - 1] = slope[n - 2]
        for (i in 1 until n - 1) {
            tangent[i] = if (slope[i - 1] * slope[i] <= 0f) 0f else (slope[i - 1] + slope[i]) / 2f
        }
        for (i in 0 until n - 1) {
            if (slope[i] == 0f) {
                tangent[i] = 0f; tangent[i + 1] = 0f
            } else {
                val a = tangent[i] / slope[i]
                val b = tangent[i + 1] / slope[i]
                val h = a * a + b * b
                if (h > 9f) {
                    val t = 3f / kotlin.math.sqrt(h)
                    tangent[i] = t * a * slope[i]
                    tangent[i + 1] = t * b * slope[i]
                }
            }
        }
        var k = 0
        while (k < n - 2 && x > pts[k + 1].x) k++
        val h = dx[k]
        val t = (x - pts[k].x) / h
        val t2 = t * t
        val t3 = t2 * t
        return (2 * t3 - 3 * t2 + 1) * pts[k].y + (t3 - 2 * t2 + t) * h * tangent[k] +
            (-2 * t3 + 3 * t2) * pts[k + 1].y + (t3 - t2) * h * tangent[k + 1]
    }

    fun apply(lut: FloatArray, v: Float): Float {
        val pos = ColorMath.clamp01(v) * (SIZE - 1)
        val i = pos.toInt()
        if (i >= SIZE - 1) return lut[SIZE - 1]
        return lut[i] + (lut[i + 1] - lut[i]) * (pos - i)
    }
}
