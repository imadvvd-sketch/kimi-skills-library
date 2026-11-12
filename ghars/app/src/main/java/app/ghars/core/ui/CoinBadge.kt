package app.ghars.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme

/**
 * The only place the gold colour appears in the interface: coins and rewards.
 *
 * @param text what is shown, for example "120" or "+25".
 * @param description the full sentence for screen readers, for example "Your balance: 120 coins".
 */
@Composable
fun CoinBadge(text: String, description: String, modifier: Modifier = Modifier) {
    val colors = GharsTheme.colors
    // Dark text on gold in both themes; the dark theme's ink is light and would not be readable here.
    val content = if (colors.isFocus) colors.night else colors.ink
    Row(
        modifier = modifier
            .clip(GharsTheme.radius.pillShape)
            .background(colors.coin)
            .padding(horizontal = GharsTheme.spacing.space3, vertical = GharsTheme.spacing.space1)
            .clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_coins),
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(GharsTheme.sizes.iconSmall),
        )
        Spacer(Modifier.width(GharsTheme.spacing.space1))
        Text(text = text, style = GharsTheme.type.label, color = content)
    }
}
