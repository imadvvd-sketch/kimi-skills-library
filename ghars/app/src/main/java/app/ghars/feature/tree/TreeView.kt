package app.ghars.feature.tree

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.ghars.core.designsystem.GharsColors
import app.ghars.core.designsystem.GharsMotion
import app.ghars.core.designsystem.GharsTheme

/** True when the user turned animations off in system settings. */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember(resolver) {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

private const val WITHERED_KEY = 0

/**
 * Draws a tree and animates between growth stages.
 *
 * @param stage 1 (seed) to 5 (full tree).
 * @param sway gentle movement of the plant, for the running session.
 * @param description what a screen reader should say; null if a parent already describes it.
 */
@Composable
fun TreeView(
    speciesId: String,
    stage: Int,
    modifier: Modifier = Modifier,
    withered: Boolean = false,
    sway: Boolean = false,
    description: String? = null,
) {
    val art = SpeciesCatalog.get(speciesId).art
    val colors = GharsTheme.colors
    val reduceMotion = rememberReduceMotion()

    val key = if (withered) WITHERED_KEY else stage.coerceIn(1, art.stages.size)
    var from by remember { mutableIntStateOf(key) }
    var to by remember { mutableIntStateOf(key) }
    val transition = remember { Animatable(1f) }

    LaunchedEffect(key) {
        if (key == to) return@LaunchedEffect
        from = to
        to = key
        transition.snapTo(0f)
        transition.animateTo(
            1f,
            when {
                reduceMotion -> tween(GharsMotion.FADE_MS, easing = LinearEasing)
                key == WITHERED_KEY -> tween(GharsMotion.WITHER_MS, easing = FastOutSlowInEasing)
                else -> tween(GharsMotion.GROW_MS, easing = GharsMotion.GrowEasing)
            },
        )
    }

    val swayDegrees = if (sway && !reduceMotion && !withered) {
        val infinite = rememberInfiniteTransition(label = "sway")
        val value by infinite.animateFloat(
            initialValue = -1.2f, targetValue = 1.2f,
            animationSpec = infiniteRepeatable(tween(GharsMotion.SWAY_MS, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "sway",
        )
        value
    } else 0f

    fun shapesFor(k: Int) = if (k == WITHERED_KEY) art.withered else art.stages[k - 1]

    Canvas(
        modifier = if (description != null) modifier.semantics { contentDescription = description } else modifier,
    ) {
        val t = transition.value
        val settled = t >= 1f && !transition.isRunning
        if (settled) {
            drawLayer(shapesFor(to), colors, alpha = 1f, scale = 1f, rotation = swayDegrees)
        } else {
            val wilting = to == WITHERED_KEY
            // Growing pops up with a little overshoot. Wilting bows slowly, with no bounce.
            val moving = !reduceMotion
            drawLayer(
                shapesFor(from), colors,
                alpha = (1f - t).coerceIn(0f, 1f),
                scale = 1f,
                rotation = if (wilting && moving) 5f * t else swayDegrees,
            )
            drawLayer(
                shapesFor(to), colors,
                alpha = t.coerceIn(0f, 1f),
                scale = if (!wilting && moving) 0.82f + 0.18f * t else 1f,
                rotation = if (wilting) 0f else swayDegrees,
            )
        }
    }
}

private fun DrawScope.drawLayer(
    shapes: List<TreeShape>, colors: GharsColors, alpha: Float, scale: Float, rotation: Float,
) {
    if (alpha <= 0f) return
    val base = treeBase()
    drawIntoCanvas { canvas ->
        // One layer per tree so overlapping pieces fade together instead of showing through each other.
        if (alpha < 1f) canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { this.alpha = alpha })
        withTransform({
            scale(scale, scale, base)
            rotate(rotation, base)
        }) { drawTreeShapes(shapes, colors) }
        if (alpha < 1f) canvas.restore()
    }
}
