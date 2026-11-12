package app.ghars.core.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Colour roles from docs/design/tokens.json. TokensTest keeps these in sync with the file. */
@Immutable
data class GharsColors(
    val bg: Color,
    val surface: Color,
    val surfaceSunk: Color,
    val line: Color,
    val ink: Color,
    val inkMuted: Color,
    val brand: Color,
    val onBrand: Color,
    val brandSoft: Color,
    val coin: Color,
    val withered: Color,
    val danger: Color,
    // Fixed illustration colours: identical in both themes.
    val leaf: Color = Color(0xFF1E7A52),
    val mint: Color = Color(0xFF3DDC97),
    val clay: Color = Color(0xFFB8532F),
    val sand: Color = Color(0xFFF6F1E7),
    val night: Color = Color(0xFF0B1A15),
    val isFocus: Boolean,
)

/** Light theme: browsing (forest, stats, store). */
val GharsLightColors = GharsColors(
    bg = Color(0xFFF6F1E7),
    surface = Color(0xFFFFFFFF),
    surfaceSunk = Color(0xFFEDE5D6),
    line = Color(0xFFD9CFBE),
    ink = Color(0xFF10231C),
    inkMuted = Color(0xFF55645D),
    brand = Color(0xFF1E7A52),
    onBrand = Color(0xFFFFFFFF),
    brandSoft = Color(0xFFD6EBDF),
    coin = Color(0xFFE8A93A),
    withered = Color(0xFF7A6C5B),
    danger = Color(0xFFB23A33),
    isFocus = false,
)

/** Dark theme: the focus session only. */
val GharsFocusColors = GharsColors(
    bg = Color(0xFF0B1A15),
    surface = Color(0xFF13261F),
    surfaceSunk = Color(0xFF081310),
    line = Color(0xFF24402F),
    ink = Color(0xFFEAF2EC),
    inkMuted = Color(0xFF9DB0A6),
    brand = Color(0xFF3DDC97),
    onBrand = Color(0xFF0B1A15),
    brandSoft = Color(0xFF173828),
    coin = Color(0xFFF2B84B),
    withered = Color(0xFFB3A390),
    danger = Color(0xFFFF8A7A),
    isFocus = true,
)

/**
 * Colours a tag may have: the fixed illustration palette plus the neutral ink.
 * They mark a tag beside its name; charts never rely on them to tell tags apart.
 */
val GharsTagColors: List<Color> = with(GharsLightColors) { listOf(leaf, clay, mint, night, inkMuted) }
