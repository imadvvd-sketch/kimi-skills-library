package com.imadnote.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import com.imadnote.app.data.ThemeMode

val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

@Composable
fun ImadNoteTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val arabic = LocalConfiguration.current.locales[0].language == "ar"
    val typography = remember(textScale, arabic) { appTypography(textScale, arabic) }
    CompositionLocalProvider(LocalExtendedColors provides if (dark) DarkExtended else LightExtended) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = typography,
            shapes = AppShapes,
            content = content,
        )
    }
}

object AppTheme {
    val extended: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}
