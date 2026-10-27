package com.lumalab.photo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Accent = Color(0xFF4EA8FF)
val Surface0 = Color(0xFF121212)
val Surface1 = Color(0xFF1C1C1E)
val Surface2 = Color(0xFF2A2A2D)
val TextMuted = Color(0xFF9A9AA0)
val Gold = Color(0xFFFFC44E)

private val colors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.Black,
    secondary = Gold,
    background = Surface0,
    onBackground = Color(0xFFEDEDED),
    surface = Surface1,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMuted,
    surfaceContainer = Surface1,
    surfaceContainerHigh = Surface2,
)

@Composable
fun LumaLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, content = content)
}
