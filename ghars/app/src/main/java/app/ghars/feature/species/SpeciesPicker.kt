package app.ghars.feature.species

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.core.ui.CoinBadge
import app.ghars.data.prefs.SettingsStore
import app.ghars.data.repo.SpeciesRepository
import app.ghars.domain.session.SessionRules
import app.ghars.feature.store.SpeciesListing
import app.ghars.feature.tree.SpeciesCatalog
import app.ghars.feature.tree.TreeView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SpeciesPickerUiState(val items: List<SpeciesListing> = emptyList(), val selectedId: String = "")

@HiltViewModel
class SpeciesPickerViewModel @Inject constructor(
    repository: SpeciesRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val ui = combine(repository.observeAll(), settings.speciesId) { all, selected ->
        SpeciesPickerUiState(
            // Owned first, then what can still be bought. Only species this version can draw.
            items = all.mapNotNull { row ->
                SpeciesCatalog.find(row.species.id)?.let { SpeciesListing(it, row.species.priceCoins, row.owned) }
            }.sortedByDescending { it.owned },
            selectedId = selected,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpeciesPickerUiState())

    fun select(id: String) {
        viewModelScope.launch { settings.setSpeciesId(id) }
    }
}

/** S03: choose what to plant. Locked species lead to the store. */
@Composable
fun SpeciesPickerScreen(
    onDone: () -> Unit,
    onOpenStore: () -> Unit,
    viewModel: SpeciesPickerViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val spacing = GharsTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(GharsTheme.sizes.pickerCell),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        items(ui.items, key = { it.info.id }) { item ->
            if (item.owned) {
                OwnedCard(
                    item = item,
                    selected = item.info.id == ui.selectedId,
                    onClick = {
                        viewModel.select(item.info.id)
                        onDone()
                    },
                )
            } else {
                LockedCard(item = item, onClick = onOpenStore)
            }
        }
    }
}

@Composable
private fun OwnedCard(item: SpeciesListing, selected: Boolean, onClick: () -> Unit) {
    val colors = GharsTheme.colors
    val shape = GharsTheme.radius.mdShape
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.brandSoft else colors.surface)
            .then(if (selected) Modifier.border(GharsTheme.sizes.focusRing, colors.brand, shape) else Modifier)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(GharsTheme.spacing.space4),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TreeView(
                speciesId = item.info.id,
                stage = SessionRules.STAGES,
                modifier = Modifier.size(GharsTheme.sizes.pickerTree),
            )
            Spacer(Modifier.height(GharsTheme.spacing.space2))
            Text(
                text = stringResource(item.info.name),
                style = GharsTheme.type.label,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
        }
        if (selected) {
            // Selection is shown by the border and this mark, not by colour alone.
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colors.brand,
                modifier = Modifier.align(Alignment.TopEnd).size(GharsTheme.sizes.icon),
            )
        }
    }
}

@Composable
private fun LockedCard(item: SpeciesListing, onClick: () -> Unit) {
    val colors = GharsTheme.colors
    val priceText = pluralStringResource(R.plurals.coins, item.price, item.price)
    val description = stringResource(R.string.cd_species_locked, stringResource(item.info.name), priceText)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GharsTheme.radius.mdShape)
            .background(colors.surfaceSunk)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                onClick { onClick(); true }
            }
            .padding(GharsTheme.spacing.space4),
    ) {
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            TreeView(
                speciesId = item.info.id,
                stage = SessionRules.STAGES,
                // Faded, and marked with a lock: never by fading alone.
                modifier = Modifier.size(GharsTheme.sizes.pickerTree).alpha(0.45f),
            )
            Spacer(Modifier.height(GharsTheme.spacing.space2))
            Text(
                text = stringResource(item.info.name),
                style = GharsTheme.type.label,
                color = colors.inkMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(GharsTheme.spacing.space1))
            CoinBadge(text = item.price.toString(), description = priceText)
        }
        Icon(
            painter = painterResource(R.drawable.ic_lock),
            contentDescription = null,
            tint = colors.inkMuted,
            modifier = Modifier.align(Alignment.TopEnd).size(GharsTheme.sizes.icon),
        )
    }
}
