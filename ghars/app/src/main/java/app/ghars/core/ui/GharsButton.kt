package app.ghars.core.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import app.ghars.core.designsystem.GharsTheme

enum class GharsButtonStyle { Primary, Secondary, Danger }

/** Pill button. Primary is the one brand-coloured action on a screen; Danger is for giving up only. */
@Composable
fun GharsButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: GharsButtonStyle = GharsButtonStyle.Primary,
) {
    val colors = GharsTheme.colors
    val sizes = GharsTheme.sizes
    val shape = GharsTheme.radius.pillShape
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val (background, content) = when (style) {
        GharsButtonStyle.Primary -> colors.brand to colors.onBrand
        GharsButtonStyle.Secondary -> colors.surfaceSunk to colors.ink
        GharsButtonStyle.Danger -> Color.Transparent to colors.danger
    }
    val outline = when {
        focused -> Modifier.border(sizes.focusRing, if (style == GharsButtonStyle.Primary) colors.ink else colors.brand, shape)
        style == GharsButtonStyle.Danger -> Modifier.border(sizes.hairline, colors.danger, shape)
        else -> Modifier
    }
    Box(
        modifier = modifier
            .heightIn(min = sizes.touchTarget)
            .clip(shape)
            .background(background)
            .then(outline)
            .clickable(interaction, LocalIndication.current, role = Role.Button, onClick = onClick)
            .padding(horizontal = GharsTheme.spacing.space6, vertical = GharsTheme.spacing.space3),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = GharsTheme.type.label, color = content, textAlign = TextAlign.Center)
    }
}
