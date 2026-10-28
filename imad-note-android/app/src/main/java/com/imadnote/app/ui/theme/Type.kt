package com.imadnote.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.imadnote.app.R

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(res: Int) = FontFamily(
    listOf(400, 500, 600, 700).map { w ->
        Font(res, weight = FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    },
)

/** Brand typeface for Latin UI. See docs/DESIGN_SYSTEM.md §4. */
val Inter = variableFamily(R.font.inter)

/** Arabic UI typeface: a modern sans with matching Latin, used when the app runs in Arabic. */
val ReadexPro = variableFamily(R.font.readex_pro)

private fun textStyle(family: FontFamily, size: Int, line: Int, weight: FontWeight, scale: Float, tracking: Float = 0f) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = (size * scale).sp,
    lineHeight = (line * scale).sp,
    letterSpacing = if (tracking == 0f) TextUnit.Unspecified else tracking.sp,
)

/**
 * [scale] is the in-app text size preference (1.0 standard, 1.15 large), on top of the system font scale.
 * [arabic] switches to the Arabic typeface; Arabic glyphs also get slightly taller line heights.
 */
fun appTypography(scale: Float = 1f, arabic: Boolean = false): Typography {
    val family = if (arabic) ReadexPro else Inter
    val lineBoost = if (arabic) 1.08f else 1f
    fun style(size: Int, line: Int, weight: FontWeight, scale: Float, tracking: Float = 0f) =
        textStyle(family, size, (line * lineBoost).toInt(), weight, scale, if (arabic) 0f else tracking)
    val regular = FontWeight.Normal
    val medium = FontWeight.Medium
    val semi = FontWeight.SemiBold
    return Typography(
        displayLarge = style(40, 48, semi, scale),
        displayMedium = style(34, 42, semi, scale),
        displaySmall = style(30, 38, semi, scale),
        headlineLarge = style(28, 36, semi, scale),
        headlineMedium = style(26, 34, semi, scale),
        headlineSmall = style(22, 30, semi, scale),
        titleLarge = style(20, 28, semi, scale),
        titleMedium = style(16, 24, semi, scale),
        titleSmall = style(14, 20, semi, scale),
        bodyLarge = style(16, 26, regular, scale),
        bodyMedium = style(14, 22, regular, scale),
        bodySmall = style(12, 18, regular, scale),
        labelLarge = style(14, 20, medium, scale, 0.1f),
        labelMedium = style(12, 16, medium, scale, 0.2f),
        labelSmall = style(11, 16, medium, scale, 0.3f),
    )
}
