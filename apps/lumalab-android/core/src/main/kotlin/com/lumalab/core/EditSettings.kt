package com.lumalab.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Non-destructive description of every adjustment applied to a photo.
 *
 * Slider values use the ranges shown in the editor (mostly -100..100) so that the
 * stored document is readable and stable across rendering changes.
 */
@Serializable
data class EditSettings(
    // Light
    val exposure: Float = 0f,
    val contrast: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val whites: Float = 0f,
    val blacks: Float = 0f,
    val toneCurve: ToneCurveSettings = ToneCurveSettings(),
    // Color
    val temperature: Float = 0f,
    val tint: Float = 0f,
    val vibrance: Float = 0f,
    val saturation: Float = 0f,
    val monochrome: Boolean = false,
    val hsl: List<HslAdjustment> = HslAdjustment.neutralBands(),
    val grading: ColorGrading = ColorGrading(),
    // Effects
    val texture: Float = 0f,
    val clarity: Float = 0f,
    val dehaze: Float = 0f,
    val vignette: Float = 0f,
    val vignetteMidpoint: Float = 50f,
    val vignetteFeather: Float = 50f,
    val grain: Float = 0f,
    val grainSize: Float = 25f,
    // Detail
    val sharpening: Float = 0f,
    val sharpenRadius: Float = 1f,
    val noiseReduction: Float = 0f,
    val colorNoiseReduction: Float = 0f,
    // Geometry
    val crop: CropSettings = CropSettings(),
) {
    val isDefault: Boolean get() = this == DEFAULT

    /** Everything except geometry, which presets and pasted settings never touch. */
    fun withoutGeometry(): EditSettings = copy(crop = CropSettings())

    fun hslBand(band: HslBand): HslAdjustment = hsl.getOrElse(band.ordinal) { HslAdjustment() }

    fun withHslBand(band: HslBand, value: HslAdjustment): EditSettings {
        val bands = hsl.toMutableList()
        while (bands.size < HslBand.entries.size) bands.add(HslAdjustment())
        bands[band.ordinal] = value
        return copy(hsl = bands)
    }

    /** Interpolates every look adjustment between the neutral state and this one (0 = neutral, 1 = full). */
    fun scaled(amount: Float): EditSettings = lerp(DEFAULT.copy(crop = crop), this, amount)

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        val DEFAULT = EditSettings()

        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = false }

        fun fromJson(text: String): EditSettings = json.decodeFromString(serializer(), text)

        /** Applies [look] on top of [base], keeping the base's geometry. */
        fun applyLook(base: EditSettings, look: EditSettings): EditSettings = look.copy(crop = base.crop)

        fun lerp(a: EditSettings, b: EditSettings, t: Float): EditSettings {
            fun f(x: Float, y: Float) = x + (y - x) * t
            return EditSettings(
                exposure = f(a.exposure, b.exposure),
                contrast = f(a.contrast, b.contrast),
                highlights = f(a.highlights, b.highlights),
                shadows = f(a.shadows, b.shadows),
                whites = f(a.whites, b.whites),
                blacks = f(a.blacks, b.blacks),
                toneCurve = ToneCurveSettings.lerp(a.toneCurve, b.toneCurve, t),
                temperature = f(a.temperature, b.temperature),
                tint = f(a.tint, b.tint),
                vibrance = f(a.vibrance, b.vibrance),
                saturation = f(a.saturation, b.saturation),
                monochrome = if (t >= 0.5f) b.monochrome else a.monochrome,
                hsl = HslBand.entries.map { band ->
                    val x = a.hslBand(band)
                    val y = b.hslBand(band)
                    HslAdjustment(f(x.hue, y.hue), f(x.saturation, y.saturation), f(x.luminance, y.luminance))
                },
                grading = ColorGrading(
                    shadowsHue = if (t >= 0.5f || a.grading.shadowsSat == 0f) b.grading.shadowsHue else a.grading.shadowsHue,
                    shadowsSat = f(a.grading.shadowsSat, b.grading.shadowsSat),
                    midtonesHue = if (t >= 0.5f || a.grading.midtonesSat == 0f) b.grading.midtonesHue else a.grading.midtonesHue,
                    midtonesSat = f(a.grading.midtonesSat, b.grading.midtonesSat),
                    highlightsHue = if (t >= 0.5f || a.grading.highlightsSat == 0f) b.grading.highlightsHue else a.grading.highlightsHue,
                    highlightsSat = f(a.grading.highlightsSat, b.grading.highlightsSat),
                    blending = f(a.grading.blending, b.grading.blending),
                    balance = f(a.grading.balance, b.grading.balance),
                ),
                texture = f(a.texture, b.texture),
                clarity = f(a.clarity, b.clarity),
                dehaze = f(a.dehaze, b.dehaze),
                vignette = f(a.vignette, b.vignette),
                vignetteMidpoint = f(a.vignetteMidpoint, b.vignetteMidpoint),
                vignetteFeather = f(a.vignetteFeather, b.vignetteFeather),
                grain = f(a.grain, b.grain),
                grainSize = f(a.grainSize, b.grainSize),
                sharpening = f(a.sharpening, b.sharpening),
                sharpenRadius = f(a.sharpenRadius, b.sharpenRadius),
                noiseReduction = f(a.noiseReduction, b.noiseReduction),
                colorNoiseReduction = f(a.colorNoiseReduction, b.colorNoiseReduction),
                crop = b.crop,
            )
        }
    }
}

enum class HslBand(val centerHue: Float) {
    RED(0f), ORANGE(30f), YELLOW(60f), GREEN(120f), AQUA(180f), BLUE(240f), PURPLE(270f), MAGENTA(300f)
}

@Serializable
data class HslAdjustment(
    val hue: Float = 0f,
    val saturation: Float = 0f,
    val luminance: Float = 0f,
) {
    val isNeutral: Boolean get() = hue == 0f && saturation == 0f && luminance == 0f

    companion object {
        fun neutralBands(): List<HslAdjustment> = List(HslBand.entries.size) { HslAdjustment() }
    }
}

@Serializable
data class CurvePoint(val x: Float, val y: Float)

enum class CurveChannel { MASTER, RED, GREEN, BLUE }

@Serializable
data class ToneCurveSettings(
    val master: List<CurvePoint> = IDENTITY,
    val red: List<CurvePoint> = IDENTITY,
    val green: List<CurvePoint> = IDENTITY,
    val blue: List<CurvePoint> = IDENTITY,
) {
    val isIdentity: Boolean get() = listOf(master, red, green, blue).all(::isIdentityCurve)

    fun points(channel: CurveChannel): List<CurvePoint> = when (channel) {
        CurveChannel.MASTER -> master
        CurveChannel.RED -> red
        CurveChannel.GREEN -> green
        CurveChannel.BLUE -> blue
    }

    fun withPoints(channel: CurveChannel, points: List<CurvePoint>): ToneCurveSettings {
        val sorted = points.sortedBy { it.x }
        return when (channel) {
            CurveChannel.MASTER -> copy(master = sorted)
            CurveChannel.RED -> copy(red = sorted)
            CurveChannel.GREEN -> copy(green = sorted)
            CurveChannel.BLUE -> copy(blue = sorted)
        }
    }

    companion object {
        val IDENTITY = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))

        fun isIdentityCurve(points: List<CurvePoint>): Boolean = points.all { kotlin.math.abs(it.x - it.y) < 1e-4f }

        /** Moves each point of [b] toward the diagonal; [a] is assumed to be identity-like for presets. */
        fun lerp(a: ToneCurveSettings, b: ToneCurveSettings, t: Float): ToneCurveSettings {
            if (t >= 1f) return b
            if (t <= 0f) return a
            fun blend(pa: List<CurvePoint>, pb: List<CurvePoint>): List<CurvePoint> {
                if (isIdentityCurve(pb)) return pa
                if (!isIdentityCurve(pa)) return if (t < 0.5f) pa else pb
                return pb.map { CurvePoint(it.x, it.x + (it.y - it.x) * t) }
            }
            return ToneCurveSettings(blend(a.master, b.master), blend(a.red, b.red), blend(a.green, b.green), blend(a.blue, b.blue))
        }
    }
}

@Serializable
data class ColorGrading(
    val shadowsHue: Float = 220f,
    val shadowsSat: Float = 0f,
    val midtonesHue: Float = 40f,
    val midtonesSat: Float = 0f,
    val highlightsHue: Float = 45f,
    val highlightsSat: Float = 0f,
    val blending: Float = 50f,
    val balance: Float = 0f,
) {
    val isNeutral: Boolean get() = shadowsSat == 0f && midtonesSat == 0f && highlightsSat == 0f
}

/**
 * Geometry. Orientation changes are applied first, then straightening (with automatic
 * scale-to-fill), then the crop rectangle, expressed as fractions of the straightened frame.
 */
@Serializable
data class CropSettings(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f,
    val angle: Float = 0f,
    val quarterTurns: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    /** Width / height of the crop in pixels; 0 means unconstrained. */
    val aspectRatio: Float = 0f,
) {
    val hasCrop: Boolean get() = left > 0f || top > 0f || right < 1f || bottom < 1f
    val isIdentity: Boolean get() = !hasCrop && angle == 0f && quarterTurns % 4 == 0 && !flipHorizontal && !flipVertical

    fun withoutRect(): CropSettings = copy(left = 0f, top = 0f, right = 1f, bottom = 1f)
}
