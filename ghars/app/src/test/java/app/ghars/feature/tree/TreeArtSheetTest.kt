package app.ghars.feature.tree

import app.ghars.core.designsystem.GharsColors
import app.ghars.core.designsystem.GharsFocusColors
import app.ghars.core.designsystem.GharsLightColors
import androidx.compose.ui.graphics.toArgb
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Checks every species has drawable art, and writes build/reports/tree-art.svg:
 * a contact sheet of all stages on both themes, for reviewing artwork without a device.
 * The SVG mirrors the geometry in TreeShapes.kt.
 */
class TreeArtSheetTest {
    private val ids = listOf("olive", "lemon", "cactus")
    private val cell = 200f

    private fun GharsColors.css(tone: Tone): String {
        val c = tone(tone)
        return "fill=\"#%06X\" fill-opacity=\"%.2f\"".format(c.toArgb() and 0xFFFFFF, c.alpha)
    }

    private fun svg(shapes: List<TreeShape>, colors: GharsColors, ox: Float, oy: Float): String {
        val unit = cell * 0.9f
        val bx = ox + cell / 2
        val by = oy + cell - 0.1f * cell
        fun px(x: Float) = bx + x * unit
        fun py(y: Float) = by - y * unit
        return shapes.joinToString("\n") { s ->
            when (s) {
                is TreeShape.Disc -> "<circle cx='${px(s.x)}' cy='${py(s.y)}' r='${s.r * unit}' ${colors.css(s.tone)}/>"
                is TreeShape.Mound -> "<ellipse cx='$bx' cy='${by + 0.015f * unit}' rx='${s.halfWidth * unit}' ry='${0.035f * unit}' ${colors.css(s.tone)}/>"
                is TreeShape.Stem -> {
                    val cx = (px(s.x0) + px(s.x1)) / 2 + s.bend * unit
                    val cy = (py(s.y0) + py(s.y1)) / 2
                    "<path d='M${px(s.x0)},${py(s.y0)} Q$cx,$cy ${px(s.x1)},${py(s.y1)}' fill='none' stroke-linecap='round' " +
                        "stroke-width='${s.width * unit}' ${colors.css(s.tone).replace("fill", "stroke")}/>"
                }
                is TreeShape.Lens -> {
                    val rad = Math.toRadians(s.angle.toDouble())
                    val dx = cos(rad).toFloat(); val dy = -sin(rad).toFloat()
                    val fx = px(s.x); val fy = py(s.y)
                    val tx = fx + dx * s.length * unit; val ty = fy + dy * s.length * unit
                    val mx = (fx + tx) / 2; val my = (fy + ty) / 2
                    val sx = -dy * s.width * unit; val sy = dx * s.width * unit
                    "<path d='M$fx,$fy Q${mx + sx},${my + sy} $tx,$ty Q${mx - sx},${my - sy} $fx,$fy Z' ${colors.css(s.tone)}/>"
                }
            }
        }
    }

    @Test
    fun `every species has five stages and a distinct withered form`() {
        val out = StringBuilder()
        var row = 0
        for (colors in listOf(GharsFocusColors, GharsLightColors)) for (id in ids) {
            val art = SpeciesCatalog.get(id).art
            assertTrue(id, art.stages.all { it.isNotEmpty() } && art.withered.isNotEmpty())
            // Wilting must be recognisable by shape: no leaves, fruit or crown left.
            assertTrue("$id withered", art.withered.none { it is TreeShape.Disc })
            val bg = "#%06X".format(colors.bg.toArgb() and 0xFFFFFF)
            out.append("<rect x='0' y='${row * cell}' width='${6 * cell}' height='$cell' fill='$bg'/>\n")
            (art.stages + listOf(art.withered)).forEachIndexed { col, shapes ->
                out.append(svg(shapes, colors, col * cell, row * cell)).append('\n')
            }
            row++
        }
        File("build/reports").mkdirs()
        File("build/reports/tree-art.svg").writeText(
            "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 ${6 * cell} ${row * cell}'>\n$out</svg>\n",
        )
    }
}
