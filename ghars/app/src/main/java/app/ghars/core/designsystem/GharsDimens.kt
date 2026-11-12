package app.ghars.core.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp grid from docs/design/tokens.json. */
@Immutable
data class GharsSpacing(
    val space1: Dp = 4.dp,
    val space2: Dp = 8.dp,
    val space3: Dp = 12.dp,
    val space4: Dp = 16.dp,
    val space6: Dp = 24.dp,
    val space8: Dp = 32.dp,
    val space12: Dp = 48.dp,
)

@Immutable
data class GharsRadius(
    val sm: Dp = 10.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 28.dp,
    val pill: Dp = 999.dp,
) {
    val smShape: Shape get() = RoundedCornerShape(sm)
    val mdShape: Shape get() = RoundedCornerShape(md)
    val lgShape: Shape get() = RoundedCornerShape(lg)
    val pillShape: Shape get() = RoundedCornerShape(percent = 50)
}

@Immutable
data class GharsShadow(val offsetY: Dp, val blur: Dp, val color: Color)

@Immutable
data class GharsShadows(
    /** Raised cards, light theme only. */
    val soft: GharsShadow = GharsShadow(8.dp, 24.dp, Color(0x1410231C)),
    /** Tree glow at the moment a session completes, nothing else. */
    val glow: GharsShadow = GharsShadow(0.dp, 32.dp, Color(0x593DDC97)),
)

/** Fixed sizes from the Icons and Accessibility sections of docs/design/README.md. */
@Immutable
data class GharsSizes(
    val icon: Dp = 24.dp,
    val touchTarget: Dp = 48.dp,
    val hairline: Dp = 1.dp,
    val focusRing: Dp = 2.dp,
)
