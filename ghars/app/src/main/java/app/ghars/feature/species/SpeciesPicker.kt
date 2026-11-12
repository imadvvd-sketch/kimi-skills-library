package app.ghars.feature.species

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.data.prefs.SettingsStore
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

data class SpeciesPickerUiState(val owned: List<SpeciesInfo> = emptyList(), val selectedId: String = "")

@HiltViewModel
class SpeciesPickerViewModel @Inject constructor(
    repository: SpeciesRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val ui = combine(repository.observeOwned(), settings.speciesId) { owned, selected ->
        // Only species this version of the app can draw.
        SpeciesPickerUiState(owned.mapNotNull { SpeciesCatalog.find(it.id) }, selected)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpeciesPickerUiState())

    fun select(id: String) {
        viewModelScope.launch { settings.setSpeciesId(id) }
    }
}

/** S03: the species the user owns. Locked ones arrive with the store in phase 2. */
@Composable
fun SpeciesPickerScreen(onDone: () -> Unit, viewModel: SpeciesPickerViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val spacing = GharsTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(GharsTheme.sizes.pickerCell),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(spacing.space3),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        items(ui.owned, key = { it.id }) { species ->
            SpeciesCard(
                species = species,
                selected = species.id == ui.selectedId,
                onClick = {
                    viewModel.select(species.id)
                    onDone()
                },
            )
        }
    }
}

@Composable
private fun SpeciesCard(species: SpeciesInfo, selected: Boolean, onClick: () -> Unit) {
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
                speciesId = species.id,
                stage = SessionRules.STAGES,
                modifier = Modifier.size(GharsTheme.sizes.pickerTree),
            )
            Spacer(Modifier.height(GharsTheme.spacing.space2))
            Text(
                text = stringResource(species.name),
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
