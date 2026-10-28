package com.imadnote.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

// Brand palette from the brief. Text-secondary, success and warning have darker *text* variants
// because the originals fail WCAG AA on light backgrounds (see docs/DESIGN_SYSTEM.md §3).
internal object Brand {
    val Primary = Color(0xFF243B53)
    val Secondary = Color(0xFF527DAA)
    val BackgroundLight = Color(0xFFF7F8FA)
    val SurfaceLight = Color(0xFFFFFFFF)
    val TextPrimaryLight = Color(0xFF202A35)
    val TextSecondaryLight = Color(0xFF5F6B7A)
    val Success = Color(0xFF3A8D75)
    val Warning = Color(0xFFD99545)
    val BorderLight = Color(0xFFE4E8ED)

    val BackgroundDark = Color(0xFF111820)
    val SurfaceDark = Color(0xFF1B2632)
    val TextPrimaryDark = Color(0xFFF1F5F9)
    val TextSecondaryDark = Color(0xFFA8B3C2)
    val PrimaryDark = Color(0xFF82A9D6)
    val BorderDark = Color(0xFF2D3946)
}

internal val LightColors = lightColorScheme(
    primary = Brand.Primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE6F2),
    onPrimaryContainer = Color(0xFF14273A),
    secondary = Color(0xFF3F6A96),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2ECF8),
    onSecondaryContainer = Color(0xFF1C3550),
    tertiary = Color(0xFF2F7A64),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDDF0E8),
    onTertiaryContainer = Color(0xFF123D31),
    background = Brand.BackgroundLight,
    onBackground = Brand.TextPrimaryLight,
    surface = Brand.SurfaceLight,
    onSurface = Brand.TextPrimaryLight,
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = Brand.TextSecondaryLight,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Brand.SurfaceLight,
    surfaceContainerLow = Color(0xFFFBFCFD),
    surfaceContainer = Color(0xFFF1F4F7),
    surfaceContainerHigh = Color(0xFFEBEEF2),
    surfaceContainerHighest = Color(0xFFE4E8ED),
    inverseSurface = Color(0xFF26313D),
    inverseOnSurface = Color(0xFFF1F5F9),
    inversePrimary = Brand.PrimaryDark,
    outline = Color(0xFF8A96A3),
    outlineVariant = Brand.BorderLight,
    error = Color(0xFFB42318),
    onError = Color.White,
    errorContainer = Color(0xFFFBE4E1),
    onErrorContainer = Color(0xFF5C1109),
    scrim = Color.Black,
)

internal val DarkColors = darkColorScheme(
    primary = Brand.PrimaryDark,
    onPrimary = Brand.BackgroundDark,
    primaryContainer = Color(0xFF2A4560),
    onPrimaryContainer = Color(0xFFDCE6F2),
    secondary = Color(0xFF9DBCE0),
    onSecondary = Brand.BackgroundDark,
    secondaryContainer = Color(0xFF253A52),
    onSecondaryContainer = Color(0xFFDCE6F2),
    tertiary = Color(0xFF6CC3A6),
    onTertiary = Brand.BackgroundDark,
    tertiaryContainer = Color(0xFF1D3B33),
    onTertiaryContainer = Color(0xFFBFE8D9),
    background = Brand.BackgroundDark,
    onBackground = Brand.TextPrimaryDark,
    surface = Brand.SurfaceDark,
    onSurface = Brand.TextPrimaryDark,
    surfaceVariant = Color(0xFF243140),
    onSurfaceVariant = Brand.TextSecondaryDark,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = Color(0xFF0D131A),
    surfaceContainerLow = Color(0xFF161F29),
    surfaceContainer = Brand.SurfaceDark,
    surfaceContainerHigh = Color(0xFF223040),
    surfaceContainerHighest = Color(0xFF2A394A),
    inverseSurface = Color(0xFFE4E8ED),
    inverseOnSurface = Color(0xFF1B2632),
    inversePrimary = Brand.Primary,
    outline = Color(0xFF6B7A8C),
    outlineVariant = Brand.BorderDark,
    error = Color(0xFFF2918A),
    onError = Brand.BackgroundDark,
    errorContainer = Color(0xFF4A1D1A),
    onErrorContainer = Color(0xFFFAD4D0),
    scrim = Color.Black,
)

@Immutable
data class ExtendedColors(
    val success: Color,
    val warning: Color,
    /** The warm brand accent (folded corner, "sun"); for fills and artwork, never for text. */
    val accent: Color,
    val priorityHigh: Color,
    val priorityMedium: Color,
    val priorityLow: Color,
    val noteColors: List<Color>,
    val projectColors: List<Color>,
)

/** Note tints; index 0 means "no colour" and falls back to the surface. */
private val NoteColorsLight = listOf(
    Brand.SurfaceLight, Color(0xFFFBF1DC), Color(0xFFE3F1EA), Color(0xFFE2ECF8),
    Color(0xFFECE6F6), Color(0xFFF9E8EA), Color(0xFFE8ECF1),
)
private val NoteColorsDark = listOf(
    Brand.SurfaceDark, Color(0xFF3A3222), Color(0xFF213A31), Color(0xFF1F3247),
    Color(0xFF2E2840), Color(0xFF40262B), Color(0xFF2A3440),
)
private val ProjectColors = listOf(
    Brand.Secondary, Brand.Success, Brand.Warning, Color(0xFF8E6CC9),
    Color(0xFFC2566B), Color(0xFF4A9BB5), Color(0xFF7B8794),
)

internal val LightExtended = ExtendedColors(
    success = Color(0xFF2F7A64),
    warning = Color(0xFF9A5B12),
    accent = Brand.Warning,
    priorityHigh = Color(0xFFB42318),
    priorityMedium = Color(0xFF9A5B12),
    priorityLow = Color(0xFF3F6A96),
    noteColors = NoteColorsLight,
    projectColors = ProjectColors,
)

internal val DarkExtended = ExtendedColors(
    success = Color(0xFF6CC3A6),
    warning = Brand.Warning,
    accent = Brand.Warning,
    priorityHigh = Color(0xFFF2918A),
    priorityMedium = Brand.Warning,
    priorityLow = Color(0xFF9DBCE0),
    noteColors = NoteColorsDark,
    projectColors = ProjectColors,
)
