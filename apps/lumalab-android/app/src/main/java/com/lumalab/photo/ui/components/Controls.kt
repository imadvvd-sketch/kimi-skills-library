package com.lumalab.photo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.lumalab.core.Histogram
import com.lumalab.photo.ui.theme.Accent
import com.lumalab.photo.ui.theme.TextMuted
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Labelled adjustment slider. Double-tap the label row to reset to [default].
 * The slider itself is always laid out left-to-right so gradient tracks read correctly in RTL.
 */
@Composable
fun AdjustSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onCommit: () -> Unit,
    default: Float = 0f,
    decimals: Int = 0,
    signed: Boolean = range.start < 0f,
    track: Brush? = null,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .pointerInput(default) { detectTapGestures(onDoubleTap = { onChange(default); onCommit() }) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = if (value != default) Color.White else TextMuted)
            Spacer(Modifier.weight(1f))
            Text(formatValue(value, decimals, signed), style = MaterialTheme.typography.bodyMedium, color = TextMuted)
        }
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(contentAlignment = Alignment.Center) {
                if (track != null) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(track),
                    )
                }
                Slider(
                    value = value.coerceIn(range),
                    onValueChange = onChange,
                    onValueChangeFinished = onCommit,
                    valueRange = range,
                    modifier = Modifier.height(32.dp),
                    colors = if (track != null) {
                        SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.Transparent,
                            inactiveTrackColor = Color.Transparent,
                        )
                    } else {
                        SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Accent,
                            inactiveTrackColor = Color(0xFF3A3A3E),
                        )
                    },
                )
            }
        }
    }
}

private fun formatValue(v: Float, decimals: Int, signed: Boolean): String {
    val text = String.format(Locale.US, "%.${decimals}f", v)
    val isZero = abs(v) < (if (decimals == 0) 0.5f else 0.005f)
    return if (signed && v > 0f && !isZero) "+$text" else if (isZero) String.format(Locale.US, "%.${decimals}f", 0f) else text
}

object Tracks {
    val temperature = Brush.horizontalGradient(listOf(Color(0xFF3D7BFF), Color(0xFFDDDDDD), Color(0xFFFFC23D)))
    val tint = Brush.horizontalGradient(listOf(Color(0xFF3DCC5A), Color(0xFFDDDDDD), Color(0xFFE04FD8)))
    val hue = Brush.horizontalGradient(
        listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red),
    )
    val luminance = Brush.horizontalGradient(listOf(Color.Black, Color.White))

    fun saturation(color: Color) = Brush.horizontalGradient(listOf(Color(0xFF808080), color))

    fun hueShift(center: Float): Brush = Brush.horizontalGradient(
        listOf(hueColor(center - 30f), hueColor(center), hueColor(center + 30f)),
    )

    fun hueColor(h: Float): Color = Color.hsl(((h % 360f) + 360f) % 360f, 1f, 0.5f)
}

/** RGB histogram drawn with additive-looking translucent channels. */
@Composable
fun HistogramView(histogram: Histogram, modifier: Modifier = Modifier) {
    Canvas(modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))) {
        val max = sqrt(histogram.max.toFloat()).coerceAtLeast(1f)
        fun channel(bins: IntArray, color: Color) {
            val path = Path().apply {
                moveTo(0f, size.height)
                for (i in 0 until 256) {
                    val x = i / 255f * size.width
                    val y = size.height - sqrt(bins[i].toFloat()) / max * size.height * 0.95f
                    lineTo(x, y)
                }
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, color)
        }
        channel(histogram.red, Color(0x99FF4040))
        channel(histogram.green, Color(0x9940FF40))
        channel(histogram.blue, Color(0x994080FF))
    }
}

/** Colour wheel-free grading control: a hue slider and saturation slider for one tonal range. */
@Composable
fun GradingRange(
    label: String,
    hue: Float,
    sat: Float,
    onChange: (Float, Float) -> Unit,
    onCommit: () -> Unit,
    hueLabel: String,
    satLabel: String,
    defaultHue: Float,
) {
    Column {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .padding(end = 8.dp)
                    .size(12.dp)
                    .background(Tracks.hueColor(hue).copy(alpha = (0.25f + sat / 100f).coerceAtMost(1f)), RoundedCornerShape(6.dp)),
            )
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
        AdjustSlider(hueLabel, hue, 0f..360f, { onChange(it, sat) }, onCommit, default = defaultHue, signed = false, track = Tracks.hue)
        AdjustSlider(satLabel, sat, 0f..100f, { onChange(hue, it) }, onCommit, signed = false, track = Tracks.saturation(Tracks.hueColor(hue)))
    }
}

internal fun Offset.distanceTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt(dx * dx + dy * dy)
}
