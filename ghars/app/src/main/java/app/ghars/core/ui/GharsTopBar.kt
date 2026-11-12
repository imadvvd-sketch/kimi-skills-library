package app.ghars.core.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.ghars.core.designsystem.GharsTheme

/** Screen header: one leading action (menu or back) and the screen title. */
@Composable
fun GharsTopBar(
    title: String,
    @DrawableRes navIcon: Int,
    navIconDescription: String,
    onNavClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = GharsTheme.sizes.touchTarget + GharsTheme.spacing.space2)
            .padding(horizontal = GharsTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onNavClick) {
            Icon(
                painter = painterResource(navIcon),
                contentDescription = navIconDescription,
                tint = GharsTheme.colors.ink,
            )
        }
        Spacer(Modifier.width(GharsTheme.spacing.space1))
        Text(
            text = title,
            style = GharsTheme.type.heading,
            color = GharsTheme.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
    }
}
