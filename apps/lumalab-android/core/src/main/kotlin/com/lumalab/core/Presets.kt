package com.lumalab.core

enum class PresetCategory { RECOMMENDED, PORTRAIT, LANDSCAPE, CINEMATIC, VINTAGE, BLACK_AND_WHITE }

data class Preset(
    val id: String,
    val name: String,
    val category: PresetCategory,
    val settings: EditSettings,
)

/** Built-in looks. Presets never change geometry. */
object Presets {
    private fun curve(vararg xy: Float): List<CurvePoint> = xy.toList().chunked(2).map { CurvePoint(it[0], it[1]) }

    private val fadedCurve = curve(0f, 0.08f, 0.25f, 0.27f, 0.75f, 0.76f, 1f, 0.94f)
    private val sCurve = curve(0f, 0f, 0.25f, 0.2f, 0.75f, 0.82f, 1f, 1f)
    private val matteCurve = curve(0f, 0.12f, 0.3f, 0.3f, 0.7f, 0.72f, 1f, 0.96f)

    val all: List<Preset> = listOf(
        Preset(
            "auto_pop", "Pop", PresetCategory.RECOMMENDED,
            EditSettings(contrast = 15f, highlights = -20f, shadows = 20f, clarity = 15f, vibrance = 25f, dehaze = 5f),
        ),
        Preset(
            "bright_airy", "Bright & Airy", PresetCategory.RECOMMENDED,
            EditSettings(exposure = 0.4f, contrast = -10f, highlights = -30f, shadows = 35f, whites = 15f, temperature = 6f, vibrance = 10f, saturation = -8f),
        ),
        Preset(
            "moody", "Moody", PresetCategory.RECOMMENDED,
            EditSettings(
                exposure = -0.3f, contrast = 20f, highlights = -40f, shadows = -10f, blacks = -15f, saturation = -20f, clarity = 10f,
                vignette = -25f, toneCurve = ToneCurveSettings(master = fadedCurve),
                grading = ColorGrading(shadowsHue = 210f, shadowsSat = 25f, highlightsHue = 40f, highlightsSat = 15f),
            ),
        ),
        Preset(
            "portrait_soft", "Soft Skin", PresetCategory.PORTRAIT,
            EditSettings(
                exposure = 0.15f, contrast = -5f, highlights = -20f, shadows = 15f, texture = -20f, clarity = -10f, temperature = 5f,
                hsl = HslAdjustment.neutralBands().toMutableList().also {
                    it[HslBand.ORANGE.ordinal] = HslAdjustment(saturation = -10f, luminance = 15f)
                    it[HslBand.RED.ordinal] = HslAdjustment(saturation = -5f, luminance = 5f)
                },
            ),
        ),
        Preset(
            "portrait_warm", "Golden Hour", PresetCategory.PORTRAIT,
            EditSettings(
                exposure = 0.1f, contrast = 10f, highlights = -25f, shadows = 20f, temperature = 22f, tint = 5f, vibrance = 15f,
                grading = ColorGrading(highlightsHue = 40f, highlightsSat = 20f, shadowsHue = 25f, shadowsSat = 10f),
            ),
        ),
        Preset(
            "portrait_clean", "Clean Studio", PresetCategory.PORTRAIT,
            EditSettings(contrast = 12f, highlights = -10f, whites = 10f, blacks = -5f, saturation = -5f, sharpening = 40f),
        ),
        Preset(
            "landscape_vivid", "Vivid Land", PresetCategory.LANDSCAPE,
            EditSettings(
                contrast = 20f, highlights = -45f, shadows = 35f, whites = 10f, blacks = -10f, clarity = 25f, dehaze = 15f, vibrance = 35f,
                hsl = HslAdjustment.neutralBands().toMutableList().also {
                    it[HslBand.BLUE.ordinal] = HslAdjustment(saturation = 15f, luminance = -15f)
                    it[HslBand.GREEN.ordinal] = HslAdjustment(hue = 10f, saturation = 10f)
                },
            ),
        ),
        Preset(
            "landscape_haze", "Clear Sky", PresetCategory.LANDSCAPE,
            EditSettings(dehaze = 35f, contrast = 10f, highlights = -30f, vibrance = 15f, temperature = -5f, sharpening = 30f),
        ),
        Preset(
            "landscape_autumn", "Autumn", PresetCategory.LANDSCAPE,
            EditSettings(
                temperature = 15f, contrast = 15f, vibrance = 20f,
                hsl = HslAdjustment.neutralBands().toMutableList().also {
                    it[HslBand.GREEN.ordinal] = HslAdjustment(hue = -60f, saturation = -10f)
                    it[HslBand.YELLOW.ordinal] = HslAdjustment(hue = -25f, saturation = 20f)
                    it[HslBand.ORANGE.ordinal] = HslAdjustment(saturation = 25f)
                },
            ),
        ),
        Preset(
            "cine_teal_orange", "Teal & Orange", PresetCategory.CINEMATIC,
            EditSettings(
                contrast = 15f, highlights = -25f, shadows = 10f, saturation = -10f,
                grading = ColorGrading(shadowsHue = 195f, shadowsSat = 40f, highlightsHue = 35f, highlightsSat = 30f, balance = 10f),
                hsl = HslAdjustment.neutralBands().toMutableList().also {
                    it[HslBand.BLUE.ordinal] = HslAdjustment(hue = -20f)
                    it[HslBand.ORANGE.ordinal] = HslAdjustment(saturation = 10f)
                },
                toneCurve = ToneCurveSettings(master = sCurve), vignette = -15f,
            ),
        ),
        Preset(
            "cine_noir", "Night City", PresetCategory.CINEMATIC,
            EditSettings(
                exposure = -0.2f, contrast = 25f, highlights = -30f, blacks = -20f, temperature = -15f, saturation = -15f, clarity = 15f,
                grading = ColorGrading(shadowsHue = 230f, shadowsSat = 30f, highlightsHue = 50f, highlightsSat = 10f), vignette = -30f,
            ),
        ),
        Preset(
            "cine_matte", "Matte Film", PresetCategory.CINEMATIC,
            EditSettings(contrast = -10f, saturation = -15f, toneCurve = ToneCurveSettings(master = matteCurve), grain = 15f, grainSize = 30f),
        ),
        Preset(
            "vintage_70s", "Seventies", PresetCategory.VINTAGE,
            EditSettings(
                temperature = 25f, tint = 8f, contrast = -10f, saturation = -10f, toneCurve = ToneCurveSettings(
                    master = fadedCurve, blue = curve(0f, 0.1f, 1f, 0.9f),
                ),
                grain = 30f, grainSize = 40f, vignette = -20f,
            ),
        ),
        Preset(
            "vintage_polaroid", "Instant", PresetCategory.VINTAGE,
            EditSettings(
                exposure = 0.2f, contrast = -15f, highlights = -20f, temperature = 10f, tint = -6f, vibrance = -10f,
                toneCurve = ToneCurveSettings(master = matteCurve, green = curve(0f, 0.04f, 1f, 0.98f)),
                grading = ColorGrading(shadowsHue = 170f, shadowsSat = 20f, highlightsHue = 55f, highlightsSat = 20f), vignette = -10f,
            ),
        ),
        Preset(
            "vintage_sepia", "Sepia", PresetCategory.VINTAGE,
            EditSettings(
                monochrome = true, contrast = 5f, grain = 20f,
                grading = ColorGrading(shadowsHue = 30f, shadowsSat = 35f, highlightsHue = 45f, highlightsSat = 30f, midtonesHue = 35f, midtonesSat = 20f),
            ),
        ),
        Preset(
            "bw_classic", "Classic B&W", PresetCategory.BLACK_AND_WHITE,
            EditSettings(monochrome = true, contrast = 20f, clarity = 10f),
        ),
        Preset(
            "bw_high_contrast", "High Contrast", PresetCategory.BLACK_AND_WHITE,
            EditSettings(monochrome = true, contrast = 50f, highlights = -20f, whites = 25f, blacks = -30f, clarity = 30f, toneCurve = ToneCurveSettings(master = sCurve)),
        ),
        Preset(
            "bw_soft", "Soft Matte B&W", PresetCategory.BLACK_AND_WHITE,
            EditSettings(monochrome = true, contrast = -15f, shadows = 30f, toneCurve = ToneCurveSettings(master = matteCurve), grain = 25f),
        ),
        Preset(
            "bw_infrared", "Infrared", PresetCategory.BLACK_AND_WHITE,
            EditSettings(
                monochrome = true, contrast = 25f, clarity = 20f,
                hsl = HslAdjustment.neutralBands().toMutableList().also {
                    it[HslBand.GREEN.ordinal] = HslAdjustment(luminance = 80f)
                    it[HslBand.YELLOW.ordinal] = HslAdjustment(luminance = 60f)
                    it[HslBand.BLUE.ordinal] = HslAdjustment(luminance = -70f)
                },
            ),
        ),
    )

    fun byId(id: String): Preset? = all.firstOrNull { it.id == id }
}
