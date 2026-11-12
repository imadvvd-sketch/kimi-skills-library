package app.ghars.feature.tree

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.ghars.core.designsystem.GharsColors
import kotlin.math.cos
import kotlin.math.sin

/** Colours a tree may use. All come from the fixed illustration palette in the design tokens. */
enum class Tone { Leaf, Mint, Highlight, Clay, Sand, Night, Fruit, Withered }

fun GharsColors.tone(tone: Tone): Color = when (tone) {
    Tone.Leaf -> leaf
    Tone.Mint -> mint
    // The single light inner shade the visual identity allows.
    Tone.Highlight -> mint.copy(alpha = 0.35f)
    Tone.Clay -> clay
    Tone.Sand -> sand
    Tone.Night -> night
    Tone.Fruit -> coin
    Tone.Withered -> withered
}

/**
 * Soft geometric pieces a tree is built from. No outlines.
 * Coordinates are in tree units: x from -0.5 to 0.5, y from 0 (ground) up to about 0.9.
 */
sealed interface TreeShape {
    data class Disc(val x: Float, val y: Float, val r: Float, val tone: Tone) : TreeShape

    /** A leaf: starts at ([x], [y]) and points [length] along [angle] (degrees, 90 = up). */
    data class Lens(
        val x: Float, val y: Float, val length: Float, val width: Float, val angle: Float, val tone: Tone,
    ) : TreeShape

    /** A round-ended trunk or branch. [bend] pushes its middle sideways. */
    data class Stem(
        val x0: Float, val y0: Float, val x1: Float, val y1: Float,
        val width: Float, val tone: Tone, val bend: Float = 0f,
    ) : TreeShape

    /** The patch of soil every tree stands in. */
    data class Mound(val halfWidth: Float, val tone: Tone = Tone.Clay) : TreeShape
}

class SpeciesArt(
    /** Five growth stages: seed, sprout, sapling, shrub, tree. */
    val stages: List<List<TreeShape>>,
    /** Drooping bare branches. A different shape, so wilting never relies on colour alone. */
    val withered: List<TreeShape>,
) {
    init { require(stages.size == 5) }
}

private const val GROUND_MARGIN = 0.1f
private const val FIT = 0.9f

/** Where the trunk meets the ground, in pixels. Transforms pivot here. */
fun DrawScope.treeBase(): Offset =
    Offset(size.width / 2f, size.height - GROUND_MARGIN * size.minDimension)

fun DrawScope.drawTreeShapes(shapes: List<TreeShape>, colors: GharsColors) {
    val unit = size.minDimension * FIT
    val base = treeBase()
    fun point(x: Float, y: Float) = Offset(base.x + x * unit, base.y - y * unit)

    for (shape in shapes) when (shape) {
        is TreeShape.Disc -> drawCircle(colors.tone(shape.tone), shape.r * unit, point(shape.x, shape.y))

        is TreeShape.Mound -> drawOval(
            color = colors.tone(shape.tone),
            topLeft = Offset(base.x - shape.halfWidth * unit, base.y - 0.02f * unit),
            size = Size(shape.halfWidth * 2 * unit, 0.07f * unit),
        )

        is TreeShape.Stem -> {
            val from = point(shape.x0, shape.y0)
            val to = point(shape.x1, shape.y1)
            val path = Path().apply {
                moveTo(from.x, from.y)
                quadraticTo(
                    (from.x + to.x) / 2 + shape.bend * unit, (from.y + to.y) / 2,
                    to.x, to.y,
                )
            }
            drawPath(path, colors.tone(shape.tone), style = Stroke(shape.width * unit, cap = StrokeCap.Round))
        }

        is TreeShape.Lens -> {
            val rad = Math.toRadians(shape.angle.toDouble())
            val dir = Offset(cos(rad).toFloat(), -sin(rad).toFloat())
            val normal = Offset(-dir.y, dir.x)
            val from = point(shape.x, shape.y)
            val to = from + dir * (shape.length * unit)
            val mid = (from + to) / 2f
            val side = normal * (shape.width * unit)
            val path = Path().apply {
                moveTo(from.x, from.y)
                quadraticTo(mid.x + side.x, mid.y + side.y, to.x, to.y)
                quadraticTo(mid.x - side.x, mid.y - side.y, from.x, from.y)
                close()
            }
            drawPath(path, colors.tone(shape.tone))
        }
    }
}
