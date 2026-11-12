package app.ghars.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme

/** S02 shell. The timer, tree preview and Plant button arrive in phase 1. */
@Composable
fun HomeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = GharsTheme.spacing.space4, vertical = GharsTheme.spacing.space12),
        verticalArrangement = Arrangement.spacedBy(GharsTheme.spacing.space2, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = GharsTheme.type.display,
            color = GharsTheme.colors.brand,
        )
        Text(
            text = stringResource(R.string.tagline),
            style = GharsTheme.type.body,
            color = GharsTheme.colors.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}
