package app.ghars.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape

private val LocalColors = staticCompositionLocalOf { GharsLightColors }
private val LocalTypography = staticCompositionLocalOf { GharsTypography() }
private val LocalSpacing = staticCompositionLocalOf { GharsSpacing() }
private val LocalRadius = staticCompositionLocalOf { GharsRadius() }
private val LocalShadows = staticCompositionLocalOf { GharsShadows() }
private val LocalSizes = staticCompositionLocalOf { GharsSizes() }

/**
 * The only source of colours, type and dimensions for screens.
 *
 * @param focusMode true only on the focus-session screen, which is always dark.
 *   Everything else is light and does not follow the system dark setting.
 */
@Composable
fun GharsTheme(focusMode: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (focusMode) GharsFocusColors else GharsLightColors
    val typography = GharsTypography()
    val radius = GharsRadius()
    CompositionLocalProvider(
        LocalColors provides colors,
        LocalTypography provides typography,
        LocalRadius provides radius,
    ) {
        // Material components are fed from Ghars tokens so no default Material look leaks through.
        MaterialTheme(
            colorScheme = colors.toMaterial(),
            typography = typography.toMaterial(),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(radius.sm),
                small = RoundedCornerShape(radius.sm),
                medium = RoundedCornerShape(radius.md),
                large = RoundedCornerShape(radius.lg),
                extraLarge = RoundedCornerShape(radius.lg),
            ),
            content = content,
        )
    }
}

object GharsTheme {
    val colors: GharsColors @Composable @ReadOnlyComposable get() = LocalColors.current
    val type: GharsTypography @Composable @ReadOnlyComposable get() = LocalTypography.current
    val spacing: GharsSpacing @Composable @ReadOnlyComposable get() = LocalSpacing.current
    val radius: GharsRadius @Composable @ReadOnlyComposable get() = LocalRadius.current
    val shadows: GharsShadows @Composable @ReadOnlyComposable get() = LocalShadows.current
    val sizes: GharsSizes @Composable @ReadOnlyComposable get() = LocalSizes.current
}

private fun GharsColors.toMaterial(): ColorScheme {
    val base = if (isFocus) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = brand,
        onPrimary = onBrand,
        primaryContainer = brandSoft,
        onPrimaryContainer = ink,
        secondary = brand,
        onSecondary = onBrand,
        secondaryContainer = brandSoft,
        onSecondaryContainer = ink,
        tertiary = coin,
        onTertiary = if (isFocus) night else ink,
        background = bg,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceSunk,
        onSurfaceVariant = inkMuted,
        surfaceContainerLowest = surface,
        surfaceContainerLow = surface,
        surfaceContainer = surface,
        surfaceContainerHigh = surface,
        surfaceContainerHighest = surfaceSunk,
        outline = inkMuted,
        outlineVariant = line,
        error = danger,
        onError = onBrand,
        scrim = night,
    )
}

private fun GharsTypography.toMaterial() = Typography(
    displayLarge = timer, displayMedium = display, displaySmall = display,
    headlineLarge = title, headlineMedium = title, headlineSmall = title,
    titleLarge = heading, titleMedium = heading, titleSmall = label,
    bodyLarge = body, bodyMedium = body, bodySmall = caption,
    labelLarge = label, labelMedium = label, labelSmall = caption,
)
