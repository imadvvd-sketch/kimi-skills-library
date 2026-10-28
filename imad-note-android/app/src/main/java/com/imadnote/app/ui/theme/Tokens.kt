package com.imadnote.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** 4 dp grid. See docs/DESIGN_SYSTEM.md §5. */
object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    /** Breathing room below the last item of a scrolling list (Quick Capture lives in the nav bar). */
    val fabClearance = 24.dp
    /** Content is centred and capped at this width on tablets and in landscape. */
    val maxContentWidth = 720.dp
    val minTouchTarget = 48.dp
}

/** Corner radii. See docs/DESIGN_SYSTEM.md §6. */
object Radius {
    val xs = RoundedCornerShape(4.dp)
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(16.dp)
    val xl = RoundedCornerShape(28.dp)
    val full = CircleShape
}

/** Elevation levels. Only floating elements cast shadows. See §7. */
object Elevation {
    val flat = 0.dp
    val float = 6.dp
}

val BorderWidth = 1.dp

/** Motion tokens. See §10. Compose honours the system "remove animations" setting. */
object Motion {
    const val FAST = 120
    const val STANDARD = 220
    const val SCREEN = 280
    val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    val Standard = FastOutSlowInEasing
}

internal val AppShapes = Shapes(
    extraSmall = Radius.xs,
    small = Radius.sm,
    medium = Radius.md,
    large = Radius.lg,
    extraLarge = Radius.xl,
)
