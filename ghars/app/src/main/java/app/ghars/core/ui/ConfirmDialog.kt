package app.ghars.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Dialog
import app.ghars.core.designsystem.GharsTheme

/** A calm two-choice dialog. The safe choice comes first and is the prominent one. */
@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    stayLabel: String,
    confirmLabel: String,
    onStay: () -> Unit,
    onConfirm: () -> Unit,
) {
    val spacing = GharsTheme.spacing
    Dialog(onDismissRequest = onStay) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GharsTheme.radius.lgShape)
                .background(GharsTheme.colors.surface)
                .padding(spacing.space6),
        ) {
            Text(
                text = title,
                style = GharsTheme.type.heading,
                color = GharsTheme.colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(spacing.space2))
            Text(text = body, style = GharsTheme.type.body, color = GharsTheme.colors.inkMuted)
            Spacer(Modifier.height(spacing.space6))
            GharsButton(text = stayLabel, onClick = onStay, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(spacing.space2))
            GharsButton(
                text = confirmLabel, onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(), style = GharsButtonStyle.Danger,
            )
        }
    }
}
