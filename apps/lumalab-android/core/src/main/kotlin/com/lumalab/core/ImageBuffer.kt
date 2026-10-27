package com.lumalab.core

/** Packed ARGB_8888 pixels, row-major, matching android.graphics.Bitmap#getPixels. */
class ImageBuffer(val width: Int, val height: Int, val pixels: IntArray = IntArray(width * height)) {
    init {
        require(width > 0 && height > 0) { "Image must not be empty" }
        require(pixels.size == width * height) { "Pixel count does not match ${width}x$height" }
    }

    val maxDimension: Int get() = maxOf(width, height)

    fun copy(): ImageBuffer = ImageBuffer(width, height, pixels.copyOf())

    /** Nearest-neighbour downscale so that the longest edge is at most [maxDim]; used for thumbnails. */
    fun downscaled(maxDim: Int): ImageBuffer {
        if (maxDimension <= maxDim) return this
        val scale = maxDim.toFloat() / maxDimension
        val w = maxOf(1, (width * scale).toInt())
        val h = maxOf(1, (height * scale).toInt())
        val out = IntArray(w * h)
        // Box-average each destination pixel to avoid aliasing.
        val fx = width.toFloat() / w
        val fy = height.toFloat() / h
        for (y in 0 until h) {
            val y0 = (y * fy).toInt()
            val y1 = minOf(height, maxOf(y0 + 1, ((y + 1) * fy).toInt()))
            for (x in 0 until w) {
                val x0 = (x * fx).toInt()
                val x1 = minOf(width, maxOf(x0 + 1, ((x + 1) * fx).toInt()))
                var r = 0; var g = 0; var b = 0; var n = 0
                for (sy in y0 until y1) {
                    val row = sy * width
                    for (sx in x0 until x1) {
                        val p = pixels[row + sx]
                        r += (p shr 16) and 0xFF; g += (p shr 8) and 0xFF; b += p and 0xFF; n++
                    }
                }
                out[y * w + x] = argb(r / n, g / n, b / n)
            }
        }
        return ImageBuffer(w, h, out)
    }

    companion object {
        fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

        fun solid(width: Int, height: Int, color: Int) = ImageBuffer(width, height, IntArray(width * height) { color })
    }
}
