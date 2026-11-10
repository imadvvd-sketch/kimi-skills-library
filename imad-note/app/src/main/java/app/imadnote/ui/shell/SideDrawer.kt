package app.imadnote.ui.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.ui.theme.ImadTheme
import kotlin.math.roundToInt

/**
 * Slide-in sidebar for phones: a panel over a dimmed scrim. It closes on a scrim tap, the back
 * button, or a swipe towards the screen edge, and mirrors itself in right-to-left layouts.
 * The panel's content is only composed while it is visible.
 */
@Composable
fun SideDrawer(open: Boolean, onClose: () -> Unit, drawer: @Composable () -> Unit, content: @Composable () -> Unit) {
    val colors = ImadTheme.colors
    val width = 304.dp
    val widthPx = with(LocalDensity.current) { width.toPx() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    var drag by remember { mutableFloatStateOf(0f) }
    val progress by animateFloatAsState(if (open) 1f else 0f, tween(240), label = "drawer")
    val shown = (progress - drag / widthPx).coerceIn(0f, 1f)

    Box(Modifier.fillMaxSize()) {
        content()
        if (progress > 0f) {
            val closeLabel = stringResource(R.string.action_close)
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = shown }.background(colors.scrim)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
                    .semantics { contentDescription = closeLabel },
            )
            Box(
                Modifier.width(width).fillMaxHeight()
                    .offset { IntOffset(((shown - 1f) * widthPx).roundToInt(), 0) }
                    .shadow(16.dp)
                    .background(colors.surface)
                    .pointerInput(rtl) {
                        detectHorizontalDragGestures(
                            onDragEnd = { if (drag > widthPx / 3) onClose(); drag = 0f },
                            onDragCancel = { drag = 0f },
                        ) { _, amount ->
                            // Dragging towards the start edge closes the drawer.
                            drag = (drag + if (rtl) amount else -amount).coerceIn(0f, widthPx)
                        }
                    },
            ) { if (open || progress > 0f) drawer() }
        }
    }
    BackHandler(enabled = open, onBack = onClose)
}
