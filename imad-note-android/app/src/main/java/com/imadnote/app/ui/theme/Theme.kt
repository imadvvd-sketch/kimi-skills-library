package com.imadnote.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.imadnote.app.data.ThemeMode

// Brand tokens. Text-secondary, success and warning were darkened from the initial brief
// so text meets WCAG AA (>= 4.5:1) on light backgrounds.
private val Primary = Color(0xFF243B53)
private val Secondary = Color(0xFF527DAA)
private val BackgroundLight = Color(0xFFF7F8FA)
private val SurfaceLight = Color(0xFFFFFFFF)
private val TextPrimaryLight = Color(0xFF202A35)
private val TextSecondaryLight = Color(0xFF5F6B7A)
private val BorderLight = Color(0xFFE4E8ED)

private val BackgroundDark = Color(0xFF111820)
private val SurfaceDark = Color(0xFF1B2632)
private val TextPrimaryDark = Color(0xFFF1F5F9)
private val TextSecondaryDark = Color(0xFFA8B3C2)
private val PrimaryDark = Color(0xFF82A9D6)
private val BorderDark = Color(0xFF2D3946)

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F2),
    onPrimaryContainer = Color(0xFF14273A),
    secondary = Color(0xFF3F6A96),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2ECF8),
    onSecondaryContainer = Color(0xFF1C3550),
    tertiary = Color(0xFF2F7A64),
    onTertiary = Color.White,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = Color(0xFFFBFCFD),
    surfaceContainer = Color(0xFFF2F4F7),
    surfaceContainerHigh = Color(0xFFECEFF3),
    surfaceContainerHighest = Color(0xFFE6EAEF),
    outline = Color(0xFF8A96A3),
    outlineVariant = BorderLight,
    error = Color(0xFFB42318),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = BackgroundDark,
    primaryContainer = Color(0xFF2A4560),
    onPrimaryContainer = Color(0xFFDCE6F2),
    secondary = Color(0xFF9DBCE0),
    onSecondary = BackgroundDark,
    secondaryContainer = Color(0xFF253A52),
    onSecondaryContainer = Color(0xFFDCE6F2),
    tertiary = Color(0xFF6CC3A6),
    onTertiary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = Color(0xFF243140),
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerLowest = Color(0xFF0D131A),
    surfaceContainerLow = Color(0xFF161F29),
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = Color(0xFF223040),
    surfaceContainerHighest = Color(0xFF2A394A),
    outline = Color(0xFF6B7A8C),
    outlineVariant = BorderDark,
    error = Color(0xFFF2918A),
    onError = BackgroundDark,
)

@Immutable
data class ExtendedColors(
    val success: Color,
    val warning: Color,
    val priorityHigh: Color,
    val priorityMedium: Color,
    val priorityLow: Color,
    val noteColors: List<Color>,
    val projectColors: List<Color>,
)

/** Soft note tints; index 0 means "no colour" and falls back to the surface. */
private val NoteColorsLight = listOf(
    SurfaceLight, Color(0xFFFBF1DC), Color(0xFFE3F1EA), Color(0xFFE2ECF8),
    Color(0xFFECE6F6), Color(0xFFF9E8EA), Color(0xFFE8ECF1),
)
private val NoteColorsDark = listOf(
    SurfaceDark, Color(0xFF3A3222), Color(0xFF213A31), Color(0xFF1F3247),
    Color(0xFF2E2840), Color(0xFF40262B), Color(0xFF2A3440),
)
private val ProjectColors = listOf(
    Secondary, Color(0xFF3A8D75), Color(0xFFD99545), Color(0xFF8E6CC9),
    Color(0xFFC2566B), Color(0xFF4A9BB5), Color(0xFF7B8794),
)

private val LightExtended = ExtendedColors(
    success = Color(0xFF2F7A64),
    warning = Color(0xFF9A5B12),
    priorityHigh = Color(0xFFB42318),
    priorityMedium = Color(0xFF9A5B12),
    priorityLow = Color(0xFF3F6A96),
    noteColors = NoteColorsLight,
    projectColors = ProjectColors,
)
private val DarkExtended = ExtendedColors(
    success = Color(0xFF6CC3A6),
    warning = Color(0xFFD99545),
    priorityHigh = Color(0xFFF2918A),
    priorityMedium = Color(0xFFD99545),
    priorityLow = Color(0xFF9DBCE0),
    noteColors = NoteColorsDark,
    projectColors = ProjectColors,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

object Spacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
    /** Bottom padding for scrolling lists so the last item clears the quick-capture button. */
    val fabClearance = 96.dp
}

private val AppTypography = Typography().let { base ->
    base.copy(
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(lineHeight = 26.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 22.sp),
    )
}

@Composable
fun ImadNoteTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(LocalExtendedColors provides if (dark) DarkExtended else LightExtended) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            content = content,
        )
    }
}

object AppTheme {
    val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
