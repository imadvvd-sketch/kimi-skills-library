package com.lumalab.core

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Renders [EditSettings] onto an image.
 *
 * Order: geometry -> noise reduction -> dehaze -> white balance/exposure (linear light) ->
 * tone (gamma space) -> curves -> HSL/vibrance/saturation -> monochrome -> colour grading ->
 * texture/clarity -> sharpening -> vignette -> grain.
 *
 * Spatial radii are proportional to the image size so a small preview looks like the export.
 */
object Processor {
    /**
     * @param checkpoint called between stages; throw from it to abandon a stale render.
     * @param includeCropRect false while the crop tool is open, so the whole frame stays visible.
     */
    fun process(
        src: ImageBuffer,
        settings: EditSettings,
        includeCropRect: Boolean = true,
        checkpoint: () -> Unit = {},
    ): ImageBuffer {
        val geo = Geometry.apply(src, settings.crop, includeCropRect)
        checkpoint()
        val w = geo.width
        val h = geo.height
        val n = w * h
        val r = FloatArray(n)
        val g = FloatArray(n)
        val b = FloatArray(n)
        val px = geo.pixels
        parallelFor(h) { y ->
            for (i in y * w until (y + 1) * w) {
                val p = px[i]
                r[i] = ((p shr 16) and 0xFF) / 255f
                g[i] = ((p shr 8) and 0xFF) / 255f
                b[i] = (p and 0xFF) / 255f
            }
        }
        val planes = Planes(w, h, r, g, b)
        val scale = geo.maxDimension / REFERENCE_SIZE

        if (settings.noiseReduction > 0f || settings.colorNoiseReduction > 0f) {
            reduceNoise(planes, settings.noiseReduction / 100f, settings.colorNoiseReduction / 100f, scale)
            checkpoint()
        }
        if (settings.dehaze != 0f) {
            dehaze(planes, settings.dehaze / 100f, scale)
            checkpoint()
        }
        ColorPipeline(settings).run(planes)
        checkpoint()
        if (settings.texture != 0f || settings.clarity != 0f) {
            localContrast(planes, settings.texture / 100f, settings.clarity / 100f, scale)
            checkpoint()
        }
        if (settings.sharpening > 0f) {
            sharpen(planes, settings.sharpening / 100f, settings.sharpenRadius, scale)
            checkpoint()
        }
        if (settings.vignette != 0f) vignette(planes, settings)
        if (settings.grain > 0f) grain(planes, settings.grain / 100f, settings.grainSize, scale)

        val out = IntArray(n)
        parallelFor(h) { y ->
            for (i in y * w until (y + 1) * w) {
                out[i] = ImageBuffer.argb(ColorMath.to8(r[i]), ColorMath.to8(g[i]), ColorMath.to8(b[i]))
            }
        }
        return ImageBuffer(w, h, out)
    }

    /** Images are tuned at this long-edge size; radii scale relative to it. */
    private const val REFERENCE_SIZE = 2000f

    internal class Planes(val w: Int, val h: Int, val r: FloatArray, val g: FloatArray, val b: FloatArray) {
        fun luma(): FloatArray {
            val out = FloatArray(r.size)
            parallelFor(h) { y ->
                for (i in y * w until (y + 1) * w) out[i] = ColorMath.luma(r[i], g[i], b[i])
            }
            return out
        }
    }

    private fun radius(base: Float, scale: Float): Int = maxOf(1, (base * scale).roundToInt())

    private fun reduceNoise(p: Planes, luminance: Float, color: Float, scale: Float) {
        val n = p.r.size
        val y = FloatArray(n)
        val cb = FloatArray(n)
        val cr = FloatArray(n)
        for (i in 0 until n) {
            val l = ColorMath.luma(p.r[i], p.g[i], p.b[i])
            y[i] = l; cb[i] = p.b[i] - l; cr[i] = p.r[i] - l
        }
        val yOut = if (luminance > 0f) {
            val blurred = Blur.blur(y, p.w, p.h, radius(2f + 2f * luminance, scale), passes = 2)
            // Edge-aware blend: only smooth where the local difference looks like noise.
            val k = 0.002f + 0.02f * luminance
            FloatArray(n) { i ->
                val d = y[i] - blurred[i]
                val wgt = luminance * exp(-(d * d) / k)
                y[i] + (blurred[i] - y[i]) * wgt
            }
        } else y
        val (cbOut, crOut) = if (color > 0f) {
            val rad = radius(3f + 9f * color, scale)
            val bcb = Blur.blur(cb, p.w, p.h, rad, passes = 2)
            val bcr = Blur.blur(cr, p.w, p.h, rad, passes = 2)
            FloatArray(n) { cb[it] + (bcb[it] - cb[it]) * color } to FloatArray(n) { cr[it] + (bcr[it] - cr[it]) * color }
        } else cb to cr
        for (i in 0 until n) {
            val l = yOut[i]
            val rr = crOut[i] + l
            val bb = cbOut[i] + l
            p.r[i] = rr; p.b[i] = bb
            p.g[i] = (l - 0.2126f * rr - 0.0722f * bb) / 0.7152f
        }
    }

    /** Dark-channel style haze removal (positive) or haze addition (negative). */
    private fun dehaze(p: Planes, amount: Float, scale: Float) {
        val n = p.r.size
        val dark = FloatArray(n) { minOf(p.r[it], p.g[it], p.b[it]) }
        val darkBlur = Blur.blur(dark, p.w, p.h, radius(40f, scale), passes = 2)
        // Atmospheric light estimated from the brightest part of the dark channel.
        val sorted = darkBlur.copyOf().also { it.sort() }
        val airlight = sorted[(sorted.size * 0.995f).toInt().coerceAtMost(sorted.size - 1)].coerceIn(0.5f, 1f)
        parallelFor(p.h) { y ->
            for (i in y * p.w until (y + 1) * p.w) {
                if (amount > 0f) {
                    val t = (1f - 0.85f * amount * darkBlur[i] / airlight).coerceAtLeast(0.15f)
                    p.r[i] = (p.r[i] - airlight) / t + airlight
                    p.g[i] = (p.g[i] - airlight) / t + airlight
                    p.b[i] = (p.b[i] - airlight) / t + airlight
                } else {
                    val k = -amount * 0.6f
                    p.r[i] += (airlight - p.r[i]) * k
                    p.g[i] += (airlight - p.g[i]) * k
                    p.b[i] += (airlight - p.b[i]) * k
                }
            }
        }
    }

    private fun localContrast(p: Planes, texture: Float, clarity: Float, scale: Float) {
        val luma = p.luma()
        val delta = FloatArray(luma.size)
        if (clarity != 0f) {
            val blurred = Blur.blur(luma, p.w, p.h, radius(30f, scale), passes = 2)
            parallelFor(p.h) { y ->
                for (i in y * p.w until (y + 1) * p.w) {
                    val l = luma[i]
                    val mid = 1f - (2f * l - 1f).let { it * it } // protect deep shadows and highlights
                    delta[i] += clarity * 0.9f * (l - blurred[i]) * mid.coerceAtLeast(0f)
                }
            }
        }
        if (texture != 0f) {
            val blurred = Blur.blur(luma, p.w, p.h, radius(5f, scale), passes = 2)
            parallelFor(p.h) { y ->
                for (i in y * p.w until (y + 1) * p.w) delta[i] += texture * 1.2f * (luma[i] - blurred[i])
            }
        }
        addLuma(p, delta)
    }

    private fun sharpen(p: Planes, amount: Float, radiusSetting: Float, scale: Float) {
        val luma = p.luma()
        val rad = maxOf(1, (radiusSetting * maxOf(scale, 0.5f)).roundToInt())
        val blurred = Blur.blur(luma, p.w, p.h, rad, passes = 2)
        val delta = FloatArray(luma.size)
        parallelFor(p.h) { y ->
            for (i in y * p.w until (y + 1) * p.w) {
                val d = luma[i] - blurred[i]
                // Small threshold keeps flat areas (and their noise) quiet.
                val gated = if (abs(d) < 0.004f) d * (abs(d) / 0.004f) else d
                delta[i] = amount * 1.5f * gated
            }
        }
        addLuma(p, delta)
    }

    private fun addLuma(p: Planes, delta: FloatArray) {
        parallelFor(p.h) { y ->
            for (i in y * p.w until (y + 1) * p.w) {
                val d = delta[i]
                p.r[i] = ColorMath.clamp01(p.r[i] + d)
                p.g[i] = ColorMath.clamp01(p.g[i] + d)
                p.b[i] = ColorMath.clamp01(p.b[i] + d)
            }
        }
    }

    private fun vignette(p: Planes, s: EditSettings) {
        val amount = s.vignette / 100f
        val mid = 0.15f + 0.85f * (s.vignetteMidpoint / 100f)
        val feather = 0.05f + 0.95f * (s.vignetteFeather / 100f)
        val cx = p.w / 2f
        val cy = p.h / 2f
        parallelFor(p.h) { y ->
            val dy = (y - cy) / cy
            for (x in 0 until p.w) {
                val dx = (x - cx) / cx
                val d = sqrt(dx * dx + dy * dy) / 1.4142135f
                val f = ColorMath.smoothstep(mid * (1f - feather), mid * (1f - feather) + feather, d) * abs(amount)
                val i = y * p.w + x
                if (amount < 0f) {
                    val k = 1f - f
                    p.r[i] *= k; p.g[i] *= k; p.b[i] *= k
                } else {
                    p.r[i] += (1f - p.r[i]) * f
                    p.g[i] += (1f - p.g[i]) * f
                    p.b[i] += (1f - p.b[i]) * f
                }
            }
        }
    }

    private fun grain(p: Planes, amount: Float, size: Float, scale: Float) {
        val cell = maxOf(1f, (0.6f + size / 100f * 3.4f) * maxOf(scale, 0.35f))
        parallelFor(p.h) { y ->
            for (x in 0 until p.w) {
                val i = y * p.w + x
                val n = valueNoise(x / cell, y / cell) - 0.5f
                val l = ColorMath.luma(p.r[i], p.g[i], p.b[i])
                val weight = 0.4f + 0.6f * (1f - (2f * l - 1f).let { it * it })
                val d = n * amount * 0.22f * weight
                p.r[i] = ColorMath.clamp01(p.r[i] + d)
                p.g[i] = ColorMath.clamp01(p.g[i] + d)
                p.b[i] = ColorMath.clamp01(p.b[i] + d)
            }
        }
    }

    private fun hash(x: Int, y: Int): Float {
        var h = x * 374761393 + y * 668265263
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0xFFFFFF) / 16777215f
    }

    private fun valueNoise(x: Float, y: Float): Float {
        val x0 = kotlin.math.floor(x).toInt()
        val y0 = kotlin.math.floor(y).toInt()
        val fx = x - x0
        val fy = y - y0
        val sx = fx * fx * (3 - 2 * fx)
        val sy = fy * fy * (3 - 2 * fy)
        val a = hash(x0, y0)
        val b = hash(x0 + 1, y0)
        val c = hash(x0, y0 + 1)
        val d = hash(x0 + 1, y0 + 1)
        return (a + (b - a) * sx) * (1 - sy) + (c + (d - c) * sx) * sy
    }
}

/** Per-pixel colour and tone work, with lookup tables precomputed from the settings. */
internal class ColorPipeline(private val s: EditSettings) {
    private val exposureGain = 2f.pow(s.exposure)
    private val wb: FloatArray = whiteBalance(s.temperature / 100f, s.tint / 100f)
    private val toneLut: FloatArray? = buildToneLut()
    private val master = ToneCurveLut.build(s.toneCurve.master)
    private val red = ToneCurveLut.build(s.toneCurve.red)
    private val green = ToneCurveLut.build(s.toneCurve.green)
    private val blue = ToneCurveLut.build(s.toneCurve.blue)
    private val hslActive = s.hsl.any { !it.isNeutral }
    private val vibrance = s.vibrance / 100f
    private val saturation = s.saturation / 100f
    private val gradingActive = !s.grading.isNeutral
    private val shadowTint = ColorMath.hueColor(s.grading.shadowsHue)
    private val midTint = ColorMath.hueColor(s.grading.midtonesHue)
    private val highTint = ColorMath.hueColor(s.grading.highlightsHue)
    private val needsWbExposure = s.exposure != 0f || s.temperature != 0f || s.tint != 0f
    private val needsHslPass = hslActive || vibrance != 0f || saturation != 0f

    fun run(p: Processor.Planes) {
        parallelFor(p.h) { y ->
            val hsl = FloatArray(3)
            val rgb = FloatArray(3)
            for (i in y * p.w until (y + 1) * p.w) {
                var r = p.r[i]
                var g = p.g[i]
                var b = p.b[i]
                if (needsWbExposure) {
                    r = ColorMath.linearToSrgb(linear(r) * wb[0] * exposureGain)
                    g = ColorMath.linearToSrgb(linear(g) * wb[1] * exposureGain)
                    b = ColorMath.linearToSrgb(linear(b) * wb[2] * exposureGain)
                }
                if (toneLut != null) {
                    r = tone(r); g = tone(g); b = tone(b)
                } else {
                    r = ColorMath.clamp01(r); g = ColorMath.clamp01(g); b = ColorMath.clamp01(b)
                }
                if (master != null) {
                    r = ToneCurveLut.apply(master, r); g = ToneCurveLut.apply(master, g); b = ToneCurveLut.apply(master, b)
                }
                if (red != null) r = ToneCurveLut.apply(red, r)
                if (green != null) g = ToneCurveLut.apply(green, g)
                if (blue != null) b = ToneCurveLut.apply(blue, b)

                if (needsHslPass) {
                    ColorMath.rgbToHsl(r, g, b, hsl)
                    adjustHsl(hsl)
                    ColorMath.hslToRgb(hsl[0], hsl[1], hsl[2], rgb)
                    r = rgb[0]; g = rgb[1]; b = rgb[2]
                }
                if (s.monochrome) {
                    val l = ColorMath.luma(r, g, b)
                    r = l; g = l; b = l
                }
                if (gradingActive) {
                    val l = ColorMath.luma(r, g, b)
                    val gr = s.grading
                    val pivot = 0.5f - 0.25f * (gr.balance / 100f)
                    val width = 0.1f + 0.4f * (gr.blending / 100f)
                    val wh = ColorMath.smoothstep(pivot - width, pivot + width, l)
                    val ws = 1f - wh
                    val wm = (1f - abs(l - 0.5f) * 2f).coerceAtLeast(0f)
                    var dr = 0f; var dg = 0f; var db = 0f
                    fun add(tint: FloatArray, sat: Float, weight: Float) {
                        if (sat == 0f || weight == 0f) return
                        val k = sat / 100f * weight * 0.35f
                        val tl = ColorMath.luma(tint[0], tint[1], tint[2])
                        dr += (tint[0] - tl) * k; dg += (tint[1] - tl) * k; db += (tint[2] - tl) * k
                    }
                    add(shadowTint, gr.shadowsSat, ws)
                    add(midTint, gr.midtonesSat, wm)
                    add(highTint, gr.highlightsSat, wh)
                    r += dr; g += dg; b += db
                }
                p.r[i] = ColorMath.clamp01(r)
                p.g[i] = ColorMath.clamp01(g)
                p.b[i] = ColorMath.clamp01(b)
            }
        }
    }

    private fun linear(v: Float): Float = ColorMath.srgbToLinear(v)

    private fun tone(v: Float): Float {
        val lut = toneLut!!
        val pos = (v.coerceIn(0f, TONE_DOMAIN) / TONE_DOMAIN) * (TONE_LUT_SIZE - 1)
        val i = pos.toInt()
        if (i >= TONE_LUT_SIZE - 1) return lut[TONE_LUT_SIZE - 1]
        return lut[i] + (lut[i + 1] - lut[i]) * (pos - i)
    }

    private fun adjustHsl(hsl: FloatArray) {
        var h = hsl[0]
        var sat = hsl[1]
        var l = hsl[2]
        if (hslActive && sat > 0f) {
            var dh = 0f; var ds = 0f; var dl = 0f
            val bands = HslBand.entries
            for (k in bands.indices) {
                val adj = s.hslBand(bands[k])
                if (adj.isNeutral) continue
                val wgt = bandWeight(h, k)
                if (wgt <= 0f) continue
                dh += adj.hue * wgt
                ds += adj.saturation * wgt
                dl += adj.luminance * wgt
            }
            h += dh / 100f * 30f
            sat *= (1f + ds / 100f).coerceAtLeast(0f)
            // Luminance shifts only affect coloured pixels, scaled by how saturated they are.
            l += dl / 100f * 0.3f * hsl[1] * (if (dl > 0) 1f - l else l) * 2f
        }
        if (vibrance != 0f) {
            val boost = if (vibrance > 0f) vibrance * (1f - sat) * skinProtection(h) else vibrance
            sat *= (1f + boost)
        }
        if (saturation != 0f) sat *= (1f + saturation)
        hsl[0] = h
        hsl[1] = ColorMath.clamp01(sat)
        hsl[2] = ColorMath.clamp01(l)
    }

    /** Triangular weights between neighbouring band centres; weights for a hue sum to 1. */
    private fun bandWeight(hue: Float, band: Int): Float {
        val bands = HslBand.entries
        val n = bands.size
        val center = bands[band].centerHue
        val next = bands[(band + 1) % n].centerHue.let { if (it <= center) it + 360f else it }
        val prev = bands[(band - 1 + n) % n].centerHue.let { if (it >= center) it - 360f else it }
        var hh = hue
        if (hh < prev) hh += 360f
        if (hh > next) hh -= 360f
        return when {
            hh == center -> 1f
            hh in prev..center -> (hh - prev) / (center - prev)
            hh in center..next -> (next - hh) / (next - center)
            else -> 0f
        }
    }

    private fun skinProtection(h: Float): Float {
        val d = abs(((h - 25f + 540f) % 360f) - 180f)
        return if (d < 25f) 0.5f + 0.5f * (d / 25f) else 1f
    }

    private fun buildToneLut(): FloatArray? {
        if (s.contrast == 0f && s.highlights == 0f && s.shadows == 0f && s.whites == 0f && s.blacks == 0f) return null
        val c = s.contrast / 100f
        val hi = s.highlights / 100f
        val sh = s.shadows / 100f
        val wh = s.whites / 100f
        val bl = s.blacks / 100f
        return FloatArray(TONE_LUT_SIZE) { i ->
            var x = i / (TONE_LUT_SIZE - 1f) * TONE_DOMAIN
            // Highlight recovery compresses values pushed past white by exposure.
            if (hi < 0f && x > 0.7f) x = 0.7f + (x - 0.7f) / (1f + (-hi) * (x - 0.7f) * 2f)
            x += wh * 0.2f * ColorMath.smoothstep(0.5f, 1f, x)
            x += bl * 0.15f * (1f - ColorMath.smoothstep(0f, 0.5f, x))
            val xc = ColorMath.clamp01(x)
            x += sh * 0.3f * 6.75f * xc * (1f - xc) * (1f - xc)
            x += hi * 0.25f * 6.75f * xc * xc * (1f - xc)
            x = ColorMath.clamp01(x)
            if (c > 0f) {
                val sCurve = if (x < 0.5f) 2f * x * x else 1f - 2f * (1f - x) * (1f - x)
                x += (sCurve - x) * c
            } else if (c < 0f) {
                x += (0.5f + (x - 0.5f) * 0.5f - x) * (-c)
            }
            ColorMath.clamp01(x)
        }
    }

    private fun whiteBalance(temp: Float, tint: Float): FloatArray {
        val r = 1f + 0.25f * temp + 0.05f * tint
        val g = 1f - 0.18f * tint
        val b = 1f - 0.25f * temp + 0.05f * tint
        val norm = 0.2126f * r + 0.7152f * g + 0.0722f * b
        return floatArrayOf(r / norm, g / norm, b / norm)
    }

    private companion object {
        const val TONE_LUT_SIZE = 2048
        const val TONE_DOMAIN = 2f
    }
}
