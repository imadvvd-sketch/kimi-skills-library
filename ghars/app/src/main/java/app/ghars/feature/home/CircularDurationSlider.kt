package app.ghars.feature.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.core.ui.drawRing
import app.ghars.domain.session.DurationDial
import app.ghars.domain.session.SessionRules
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Ring slider for the session length, 10 to 120 minutes in steps of 5.
 * Drag or tap on the ring; [content] sits inside it and keeps its own touches.
 */
@Composable
fun CircularDurationSlider(
    minutes: Int,
    onMinutesChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = GharsTheme.colors
    val sizes = GharsTheme.sizes
    val current by rememberUpdatedState(minutes)
    val onChange by rememberUpdatedState(onMinutesChange)
    val label = stringResource(R.string.cd_duration)
    val value = pluralStringResource(R.plurals.duration_minutes, minutes, minutes)
    val steps = (SessionRules.MAX_MINUTES - SessionRules.MIN_MINUTES) / SessionRules.STEP_MINUTES - 1

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .semantics {
                contentDescription = label
                stateDescription = value
                progressBarRangeInfo = ProgressBarRangeInfo(
                    minutes.toFloat(),
                    SessionRules.MIN_MINUTES.toFloat()..SessionRules.MAX_MINUTES.toFloat(),
                    steps,
                )
                setProgress { target ->
                    onChange(SessionRules.snapDuration(Math.round(target)))
                    true
                }
            }
            .pointerInput(Unit) {
                val inset = sizes.ringThumb.toPx() / 2
                val reach = sizes.touchTarget.toPx() / 2
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = min(size.width, size.height) / 2f - inset
                    // Only touches on the ring count; the middle belongs to the tree.
                    if (abs((down.position - center).getDistance() - radius) > reach) return@awaitEachGesture
                    down.consume()
                    fun update(position: Offset, dragging: Boolean) {
                        val next = DurationDial.minutesAt(position.x - center.x, position.y - center.y, current, dragging)
                        if (next != current) onChange(next)
                    }
                    update(down.position, dragging = false)
                    drag(down.id) { change ->
                        change.consume()
                        update(change.position, dragging = true)
                    }
                }
            }
            .drawBehind {
                val inset = sizes.ringThumb.toPx() / 2
                val sweep = DurationDial.fraction(minutes) * DurationDial.SWEEP_DEGREES
                drawRing(
                    track = colors.brandSoft, arc = colors.brand,
                    strokePx = sizes.ringStroke.toPx(), insetPx = inset,
                    trackSweep = DurationDial.SWEEP_DEGREES, arcSweep = sweep,
                )
                val radius = size.minDimension / 2f - inset
                val angle = Math.toRadians((sweep - 90f).toDouble())
                val thumb = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
                drawCircle(colors.brand, sizes.ringThumb.toPx() / 2, thumb)
                drawCircle(colors.onBrand, sizes.ringThumb.toPx() / 5, thumb)
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(sizes.ringThumb + GharsTheme.spacing.space4),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}
