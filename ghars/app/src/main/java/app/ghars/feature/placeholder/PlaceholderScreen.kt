package app.ghars.feature.placeholder

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import app.ghars.core.designsystem.GharsTheme

/** Empty shell for screens that are built in later phases. */
@Composable
fun PlaceholderScreen(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(GharsTheme.spacing.space4),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = GharsTheme.type.body,
            color = GharsTheme.colors.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}
