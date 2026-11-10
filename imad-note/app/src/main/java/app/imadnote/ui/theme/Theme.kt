package app.imadnote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.imadnote.data.ThemeMode

/** Semantic colour roles. Screens use these instead of raw hex values. */
@Immutable
data class ImadColors(
    val navy: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val text: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val LightImadColors = ImadColors(
    navy = Color(0xFF243B53),
    primary = Color(0xFF527DAA),
    onPrimary = Color.White,
    primarySoft = Color(0xFFE6EEF7),
    background = Color(0xFFF7F8FA),
    surface = Color(0xFFFFFFFF),
    surfaceAlt = Color(0xFFEEF2F6),
    text = Color(0xFF172B4D),
    textSecondary = Color(0xFF62748A),
    textTertiary = Color(0xFF94A3B5),
    border = Color(0xFFE5EAF0),
    success = Color(0xFF2E9D73),
    warning = Color(0xFFE5A43B),
    danger = Color(0xFFD9534F),
    scrim = Color(0x66172B4D),
    isDark = false,
)

// The dark palette keeps the same hues but raises lightness of accents so text on
// dark surfaces stays above WCAG AA contrast (checked in ThemeContrastTest).
val DarkImadColors = ImadColors(
    navy = Color(0xFFC5D5E8),
    primary = Color(0xFF8FB3DC),
    onPrimary = Color(0xFF0F1B2A),
    primarySoft = Color(0xFF233448),
    background = Color(0xFF0F151D),
    surface = Color(0xFF171F2A),
    surfaceAlt = Color(0xFF202B38),
    text = Color(0xFFE8EDF3),
    textSecondary = Color(0xFFA3B2C4),
    textTertiary = Color(0xFF7C8B9E),
    border = Color(0xFF2B3746),
    success = Color(0xFF58C79C),
    warning = Color(0xFFF0BC62),
    danger = Color(0xFFF08A86),
    scrim = Color(0x99000000),
    isDark = true,
)

@Immutable
data class Spacing(
    val xxs: androidx.compose.ui.unit.Dp = 2.dp,
    val xs: androidx.compose.ui.unit.Dp = 4.dp,
    val sm: androidx.compose.ui.unit.Dp = 8.dp,
    val md: androidx.compose.ui.unit.Dp = 12.dp,
    val lg: androidx.compose.ui.unit.Dp = 16.dp,
    val xl: androidx.compose.ui.unit.Dp = 24.dp,
    val xxl: androidx.compose.ui.unit.Dp = 32.dp,
    val xxxl: androidx.compose.ui.unit.Dp = 48.dp,
)

private val LocalImadColors = staticCompositionLocalOf { LightImadColors }
private val LocalSpacing = staticCompositionLocalOf { Spacing() }

object ImadTheme {
    val colors: ImadColors @Composable @ReadOnlyComposable get() = LocalImadColors.current
    val spacing: Spacing @Composable @ReadOnlyComposable get() = LocalSpacing.current
}

// Letter spacing stays at zero: tracking breaks the joining of Arabic letters.
private fun style(size: Int, line: Int, weight: FontWeight) =
    TextStyle(fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, letterSpacing = 0.sp)

val ImadTypography = Typography(
    displaySmall = style(30, 38, FontWeight.Bold),
    headlineLarge = style(28, 36, FontWeight.Bold),
    headlineMedium = style(24, 32, FontWeight.Bold),
    headlineSmall = style(20, 28, FontWeight.SemiBold),
    titleLarge = style(18, 26, FontWeight.SemiBold),
    titleMedium = style(16, 24, FontWeight.SemiBold),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 26, FontWeight.Normal),
    bodyMedium = style(14, 22, FontWeight.Normal),
    bodySmall = style(12, 18, FontWeight.Normal),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(11, 14, FontWeight.Medium),
)

val ImadShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private fun ImadColors.toMaterial() = (if (isDark) darkColorScheme() else lightColorScheme()).copy(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primarySoft,
    onPrimaryContainer = if (isDark) text else navy,
    secondary = navy,
    onSecondary = if (isDark) background else Color.White,
    secondaryContainer = surfaceAlt,
    onSecondaryContainer = text,
    tertiary = success,
    background = background,
    onBackground = text,
    surface = surface,
    onSurface = text,
    surfaceVariant = surfaceAlt,
    onSurfaceVariant = textSecondary,
    surfaceContainerLowest = surface,
    surfaceContainerLow = surface,
    surfaceContainer = surface,
    surfaceContainerHigh = surfaceAlt,
    surfaceContainerHighest = surfaceAlt,
    surfaceTint = Color.Transparent,
    outline = border,
    outlineVariant = border,
    error = danger,
    onError = if (isDark) background else Color.White,
    scrim = scrim,
)

@Composable
fun ImadNoteTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkImadColors else LightImadColors
    CompositionLocalProvider(LocalImadColors provides colors, LocalSpacing provides Spacing()) {
        MaterialTheme(colorScheme = colors.toMaterial(), typography = ImadTypography, shapes = ImadShapes, content = content)
    }
}
