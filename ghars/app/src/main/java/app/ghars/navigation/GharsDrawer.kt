package app.ghars.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ghars.core.ui.CoinBadge
import app.ghars.feature.store.WalletViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme

/** S20: side menu. Opens from the start edge, so from the right in Arabic. */
@Composable
fun GharsDrawer(
    current: GharsDestination,
    onSelect: (GharsDestination) -> Unit,
    wallet: WalletViewModel = hiltViewModel(),
) {
    val spacing = GharsTheme.spacing
    val balance by wallet.balance.collectAsStateWithLifecycle()
    ModalDrawerSheet(
        drawerContainerColor = GharsTheme.colors.surface,
        drawerContentColor = GharsTheme.colors.ink,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.space3),
        ) {
            Spacer(Modifier.height(spacing.space6))
            Text(
                text = stringResource(R.string.app_name),
                style = GharsTheme.type.title,
                color = GharsTheme.colors.brand,
                modifier = Modifier
                    .padding(horizontal = spacing.space3)
                    .semantics { heading() },
            )
            Text(
                text = stringResource(R.string.tagline),
                style = GharsTheme.type.label,
                color = GharsTheme.colors.inkMuted,
                modifier = Modifier.padding(horizontal = spacing.space3),
            )
            Spacer(Modifier.height(spacing.space3))
            CoinBadge(
                text = balance.toString(),
                description = stringResource(R.string.cd_balance, pluralStringResource(R.plurals.coins, balance, balance)),
                modifier = Modifier.padding(horizontal = spacing.space3),
            )
            Spacer(Modifier.height(spacing.space6))
            GharsDestination.entries.forEach { destination ->
                DrawerItem(
                    destination = destination,
                    selected = destination == current,
                    onClick = { onSelect(destination) },
                )
                Spacer(Modifier.height(spacing.space1))
            }
            Spacer(Modifier.height(spacing.space6))
        }
    }
}

@Composable
private fun DrawerItem(
    destination: GharsDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = GharsTheme.colors
    // Text stays ink: brand on brand-soft is only 4.2:1. The icon may be brand (3:1 is enough).
    val iconTint = if (selected) colors.brand else colors.ink
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GharsTheme.sizes.touchTarget)
            .clip(GharsTheme.radius.mdShape)
            .background(if (selected) colors.brandSoft else colors.surface)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .padding(horizontal = GharsTheme.spacing.space3, vertical = GharsTheme.spacing.space2),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(destination.icon),
            contentDescription = null, // the label next to it says the same thing
            tint = iconTint,
            modifier = Modifier.size(GharsTheme.sizes.icon),
        )
        Spacer(Modifier.width(GharsTheme.spacing.space3))
        Text(
            text = stringResource(destination.title),
            style = GharsTheme.type.label,
            color = colors.ink,
        )
    }
}
