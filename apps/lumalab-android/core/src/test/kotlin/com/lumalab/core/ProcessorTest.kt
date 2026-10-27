package com.lumalab.core

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProcessorTest {
    private fun gradientImage(w: Int = 64, h: Int = 48): ImageBuffer {
        val rnd = Random(7)
        return ImageBuffer(w, h, IntArray(w * h) { i ->
            val x = i % w
            val y = i / w
            ImageBuffer.argb(x * 255 / (w - 1), y * 255 / (h - 1), (128 + rnd.nextInt(-20, 20)).coerceIn(0, 255))
        })
    }

    private fun meanLuma(img: ImageBuffer): Double = img.pixels.map {
        0.2126 * ((it shr 16) and 0xFF) + 0.7152 * ((it shr 8) and 0xFF) + 0.0722 * (it and 0xFF)
    }.average()

    private fun meanSaturation(img: ImageBuffer): Double = img.pixels.map {
        val r = (it shr 16) and 0xFF
        val g = (it shr 8) and 0xFF
        val b = it and 0xFF
        (maxOf(r, g, b) - minOf(r, g, b)).toDouble()
    }.average()

    @Test
    fun defaultSettingsAreIdentity() {
        val src = gradientImage()
        val out = Processor.process(src, EditSettings())
        assertTrue(src.pixels.contentEquals(out.pixels))
    }

    @Test
    fun exposureBrightensAndDarkens() {
        val src = gradientImage()
        val base = meanLuma(src)
        assertTrue(meanLuma(Processor.process(src, EditSettings(exposure = 1f))) > base + 10)
        assertTrue(meanLuma(Processor.process(src, EditSettings(exposure = -1f))) < base - 10)
    }

    @Test
    fun toneSlidersMoveInExpectedDirection() {
        val src = gradientImage()
        val base = meanLuma(src)
        assertTrue(meanLuma(Processor.process(src, EditSettings(shadows = 100f))) > base)
        assertTrue(meanLuma(Processor.process(src, EditSettings(highlights = -100f))) < base)
        assertTrue(meanLuma(Processor.process(src, EditSettings(whites = 100f))) > base)
        assertTrue(meanLuma(Processor.process(src, EditSettings(blacks = -100f))) < base)
    }

    @Test
    fun monochromeProducesNeutralPixels() {
        val out = Processor.process(gradientImage(), EditSettings(monochrome = true))
        for (p in out.pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            assertTrue(maxOf(r, g, b) - minOf(r, g, b) <= 1)
        }
    }

    @Test
    fun saturationAndHslControlColourfulness() {
        val src = gradientImage()
        val base = meanSaturation(src)
        assertTrue(meanSaturation(Processor.process(src, EditSettings(saturation = 60f))) > base)
        assertTrue(meanSaturation(Processor.process(src, EditSettings(saturation = -100f))) < 2.0)
        val allBandsDesaturated = EditSettings(hsl = List(HslBand.entries.size) { HslAdjustment(saturation = -100f) })
        assertTrue(meanSaturation(Processor.process(src, allBandsDesaturated)) < 2.0)
    }

    @Test
    fun temperatureWarmsImage() {
        val src = ImageBuffer.solid(16, 16, ImageBuffer.argb(128, 128, 128))
        val warm = Processor.process(src, EditSettings(temperature = 80f)).pixels[0]
        assertTrue(((warm shr 16) and 0xFF) > (warm and 0xFF))
    }

    @Test
    fun spatialEffectsRunAndChangeImage() {
        val src = gradientImage()
        val settings = listOf(
            EditSettings(clarity = 60f), EditSettings(texture = 60f), EditSettings(dehaze = 60f),
            EditSettings(dehaze = -60f), EditSettings(sharpening = 100f), EditSettings(noiseReduction = 80f),
            EditSettings(colorNoiseReduction = 80f), EditSettings(vignette = -80f), EditSettings(vignette = 60f),
            EditSettings(grain = 80f),
        )
        for (s in settings) {
            val out = Processor.process(src, s)
            assertEquals(src.width, out.width)
            assertNotEquals(src.pixels.toList(), out.pixels.toList(), "No change for $s")
        }
    }

    @Test
    fun vignetteDarkensCornersNotCentre() {
        val src = ImageBuffer.solid(101, 101, ImageBuffer.argb(200, 200, 200))
        val out = Processor.process(src, EditSettings(vignette = -100f))
        val centre = out.pixels[50 * 101 + 50] and 0xFF
        val corner = out.pixels[0] and 0xFF
        assertTrue(centre >= 195)
        assertTrue(corner < 120)
    }

    @Test
    fun everyPresetRenders() {
        val src = gradientImage()
        for (preset in Presets.all) {
            val out = Processor.process(src, preset.settings)
            assertNotEquals(src.pixels.toList(), out.pixels.toList(), preset.id)
        }
        assertEquals(Presets.all.size, Presets.all.map { it.id }.toSet().size)
    }

    @Test
    fun checkpointCanAbortRender() {
        var calls = 0
        val result = runCatching {
            Processor.process(gradientImage(), EditSettings(clarity = 10f)) {
                calls++
                throw IllegalStateException("cancelled")
            }
        }
        assertTrue(result.isFailure)
        assertEquals(1, calls)
    }
}

class GeometryTest {
    private val img = ImageBuffer(3, 2, intArrayOf(1, 2, 3, 4, 5, 6))

    @Test
    fun quarterTurnRotatesClockwise() {
        val out = Geometry.orient(img, 1, false, false)
        assertEquals(2, out.width)
        assertEquals(3, out.height)
        // 1 2 3      4 1
        // 4 5 6  ->  5 2
        //            6 3
        assertEquals(listOf(4, 1, 5, 2, 6, 3), out.pixels.toList())
    }

    @Test
    fun fourTurnsAndDoubleFlipsAreIdentity() {
        var out = img
        repeat(4) { out = Geometry.orient(out, 1, false, false) }
        assertEquals(img.pixels.toList(), out.pixels.toList())
        val flipped = Geometry.orient(Geometry.orient(img, 0, true, false), 0, true, false)
        assertEquals(img.pixels.toList(), flipped.pixels.toList())
        assertEquals(listOf(3, 2, 1, 6, 5, 4), Geometry.orient(img, 0, true, false).pixels.toList())
    }

    @Test
    fun cropRectUsesNormalisedCoordinates() {
        val src = ImageBuffer(100, 50, IntArray(5000) { it })
        val out = Geometry.apply(src, CropSettings(left = 0.25f, top = 0.2f, right = 0.75f, bottom = 0.8f))
        assertEquals(50, out.width)
        assertEquals(30, out.height)
        assertEquals(10 * 100 + 25, out.pixels[0])
        val preview = Geometry.apply(src, CropSettings(left = 0.25f, right = 0.75f), includeCropRect = false)
        assertEquals(100, preview.width)
    }

    @Test
    fun straightenKeepsSizeAndScalesToFill() {
        val src = ImageBuffer.solid(80, 60, ImageBuffer.argb(10, 200, 30))
        val out = Geometry.straighten(src, 12f)
        assertEquals(80, out.width)
        assertEquals(60, out.height)
        assertTrue(out.pixels.all { it == src.pixels[0] }, "Corners must not show empty background")
        assertTrue(Geometry.straightenScale(80, 60, 12f) > 1f)
        assertEquals(1f, Geometry.straightenScale(80, 60, 0f), 1e-6f)
    }

    @Test
    fun centeredAspectRectMatchesRequestedRatio() {
        val r = Geometry.centeredAspectRect(400, 300, 1f)
        val w = (r[2] - r[0]) * 400
        val h = (r[3] - r[1]) * 300
        assertEquals(1f, w / h, 1e-4f)
        assertEquals(300f, h, 1e-3f)
    }
}

class SettingsTest {
    @Test
    fun jsonRoundTrip() {
        val s = Presets.byId("cine_teal_orange")!!.settings.copy(crop = CropSettings(left = 0.1f, angle = 3f, quarterTurns = 1))
        assertEquals(s, EditSettings.fromJson(s.toJson()))
        assertEquals(EditSettings(), EditSettings.fromJson("{}"))
        assertEquals(EditSettings(exposure = 1f), EditSettings.fromJson("""{"exposure":1.0,"futureField":true}"""))
    }

    @Test
    fun scalingInterpolatesTowardNeutral() {
        val s = EditSettings(exposure = 1f, contrast = 40f, crop = CropSettings(angle = 5f))
        assertEquals(s, s.scaled(1f))
        val half = s.scaled(0.5f)
        assertEquals(0.5f, half.exposure, 1e-6f)
        assertEquals(20f, half.contrast, 1e-6f)
        assertEquals(5f, half.crop.angle)
        assertTrue(s.scaled(0f).withoutGeometry().isDefault)
    }

    @Test
    fun applyLookKeepsGeometry() {
        val base = EditSettings(crop = CropSettings(quarterTurns = 1), exposure = 2f)
        val result = EditSettings.applyLook(base, EditSettings(contrast = 10f))
        assertEquals(1, result.crop.quarterTurns)
        assertEquals(0f, result.exposure)
        assertEquals(10f, result.contrast)
    }

    @Test
    fun toneCurveLutIsMonotoneAndIdentityIsSkipped() {
        assertNull(ToneCurveLut.build(ToneCurveSettings.IDENTITY))
        val lut = ToneCurveLut.build(listOf(CurvePoint(0f, 0f), CurvePoint(0.3f, 0.2f), CurvePoint(0.7f, 0.85f), CurvePoint(1f, 1f)))!!
        for (i in 1 until lut.size) assertTrue(lut[i] >= lut[i - 1] - 1e-6f)
        assertEquals(0.2f, ToneCurveLut.evaluate(listOf(CurvePoint(0f, 0f), CurvePoint(0.3f, 0.2f), CurvePoint(1f, 1f)), 0.3f), 1e-5f)
    }

    @Test
    fun historyUndoRedo() {
        val h = EditHistory(0)
        h.push(1); h.push(2)
        assertEquals(1, h.undo())
        assertEquals(0, h.undo())
        assertEquals(0, h.undo())
        assertEquals(1, h.redo())
        h.push(5)
        assertTrue(!h.canRedo)
        assertTrue(!h.push(5))
        assertEquals(1, h.undo())
    }

    @Test
    fun autoToneBrightensDarkImage() {
        val dark = ImageBuffer.solid(50, 50, ImageBuffer.argb(30, 30, 30))
        val auto = AutoTone.compute(dark)
        assertTrue(auto.exposure > 0.5f)
        val bright = ImageBuffer.solid(50, 50, ImageBuffer.argb(240, 240, 240))
        assertTrue(AutoTone.compute(bright).exposure < 0f)
    }

    @Test
    fun histogramCountsEveryPixel() {
        val img = ImageBuffer.solid(10, 10, ImageBuffer.argb(255, 0, 128))
        val hist = Histogram.of(img)
        assertEquals(100, hist.red[255])
        assertEquals(100, hist.green[0])
        assertEquals(100, hist.blue[128])
        assertEquals(1f, hist.clipping(100).second)
    }
}
