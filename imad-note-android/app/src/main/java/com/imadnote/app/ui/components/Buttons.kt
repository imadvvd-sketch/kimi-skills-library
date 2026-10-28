package com.imadnote.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.imadnote.app.ui.theme.Radius
import com.imadnote.app.ui.theme.Spacing

private val ButtonPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm)

/** Filled primary action. One per screen region. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = Radius.md,
        contentPadding = ButtonPadding,
        modifier = modifier.heightIn(min = Spacing.minTouchTarget),
    ) { ButtonContent(text, icon) }
}

/** Tonal secondary action. */
@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = Radius.md,
        contentPadding = ButtonPadding,
        modifier = modifier.heightIn(min = Spacing.minTouchTarget),
    ) { ButtonContent(text, icon) }
}

/** Low-emphasis action; [destructive] tints it with the danger colour. */
@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    destructive: Boolean = false,
) {
    TextButton(
        onClick = onClick,
        shape = Radius.md,
        modifier = modifier.heightIn(min = Spacing.minTouchTarget),
    ) {
        val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(Spacing.sm))
        }
        Text(text, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(Spacing.sm))
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
}
