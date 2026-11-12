package app.ghars.feature.store

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.core.ui.CoinBadge
import app.ghars.core.ui.ConfirmDialog
import app.ghars.data.repo.EconomyRepository
import app.ghars.data.repo.SpeciesRepository
import app.ghars.domain.session.SessionRules
import app.ghars.feature.tree.SpeciesCatalog
import app.ghars.feature.tree.SpeciesInfo
import app.ghars.feature.tree.TreeView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A species as the store and the picker show it. */
data class SpeciesListing(val info: SpeciesInfo, val price: Int, val owned: Boolean)

data class StoreUiState(val balance: Int = 0, val items: List<SpeciesListing> = emptyList())

/** Balance alone, for the side menu. */
@HiltViewModel
class WalletViewModel @Inject constructor(economy: EconomyRepository) : ViewModel() {
    val balance = economy.balance.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}

@HiltViewModel
class StoreViewModel @Inject constructor(
    species: SpeciesRepository,
    private val economy: EconomyRepository,
) : ViewModel() {

    val ui = combine(species.observeAll(), economy.balance) { all, balance ->
        StoreUiState(
            balance = balance,
            // Free species are not for sale; only ones this version can draw are listed.
            items = all.filter { it.species.priceCoins > 0 }.mapNotNull { row ->
                SpeciesCatalog.find(row.species.id)?.let { SpeciesListing(it, row.species.priceCoins, row.owned) }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StoreUiState())

    fun buy(speciesId: String) {
        // The repository re-checks balance and ownership, so a stale screen cannot overspend.
        viewModelScope.launch { economy.purchase(speciesId) }
    }
}

/** S12: tree species for coins. */
@Composable
fun StoreScreen(viewModel: StoreViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val spacing = GharsTheme.spacing
    var confirmId by rememberSaveable { mutableStateOf<String?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(GharsTheme.sizes.pickerCell),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                val balanceText = pluralStringResource(R.plurals.coins, ui.balance, ui.balance)
                CoinBadge(text = ui.balance.toString(), description = stringResource(R.string.cd_balance, balanceText))
            }
        }
        items(ui.items, key = { it.info.id }) { item ->
            StoreCard(item = item, balance = ui.balance, onBuy = { confirmId = item.info.id })
        }
    }

    ui.items.firstOrNull { it.info.id == confirmId }?.let { item ->
        ConfirmDialog(
            title = stringResource(R.string.store_buy_title, stringResource(item.info.name)),
            body = stringResource(R.string.store_buy_body, pluralStringResource(R.plurals.coins, item.price, item.price)),
            primaryLabel = stringResource(R.string.store_buy_confirm),
            onPrimary = {
                viewModel.buy(item.info.id)
                confirmId = null
            },
            secondaryLabel = stringResource(R.string.store_buy_cancel),
            onSecondary = { confirmId = null },
            onDismiss = { confirmId = null },
        )
    }
}

@Composable
private fun StoreCard(item: SpeciesListing, balance: Int, onBuy: () -> Unit) {
    val colors = GharsTheme.colors
    val spacing = GharsTheme.spacing
    val affordable = !item.owned && balance >= item.price
    val priceText = pluralStringResource(R.plurals.coins, item.price, item.price)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GharsTheme.radius.mdShape)
            .background(colors.surface)
            .then(
                if (affordable) {
                    Modifier.clickable(
                        onClickLabel = stringResource(R.string.store_buy_confirm), role = Role.Button, onClick = onBuy,
                    )
                } else Modifier,
            )
            .padding(spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TreeView(
            speciesId = item.info.id,
            stage = SessionRules.STAGES,
            modifier = Modifier.size(GharsTheme.sizes.pickerTree),
        )
        Spacer(Modifier.height(spacing.space2))
        Text(text = stringResource(item.info.name), style = GharsTheme.type.label, color = colors.ink)
        Spacer(Modifier.height(spacing.space2))
        if (item.owned) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_check), contentDescription = null,
                    tint = colors.brand, modifier = Modifier.size(GharsTheme.sizes.iconSmall),
                )
                Spacer(Modifier.width(spacing.space1))
                Text(stringResource(R.string.store_owned), style = GharsTheme.type.caption, color = colors.inkMuted)
            }
        } else {
            CoinBadge(text = item.price.toString(), description = priceText)
            if (!affordable) {
                val missing = item.price - balance
                Spacer(Modifier.height(spacing.space1))
                Text(
                    text = stringResource(R.string.store_short, pluralStringResource(R.plurals.coins, missing, missing)),
                    style = GharsTheme.type.caption,
                    color = colors.inkMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
