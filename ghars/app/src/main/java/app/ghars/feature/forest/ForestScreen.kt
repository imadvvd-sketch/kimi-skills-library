package app.ghars.feature.forest

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.data.repo.HistoryRepository
import app.ghars.domain.session.SessionRules
import app.ghars.domain.stats.SessionRecord
import app.ghars.feature.history.EmptyState
import app.ghars.feature.history.PeriodSelection
import app.ghars.feature.history.PeriodSwitcher
import app.ghars.feature.history.StatTile
import app.ghars.feature.history.durationText
import app.ghars.feature.tree.SpeciesCatalog
import app.ghars.feature.tree.TreeView
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class ForestUiState(
    val selection: PeriodSelection.State = PeriodSelection.State(),
    val trees: List<SessionRecord> = emptyList(),
) {
    val focusMinutes: Int get() = trees.filter { it.success }.sumOf { it.plannedMinutes }
    val grown: Int get() = trees.count { it.success }
    val withered: Int get() = trees.size - grown
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ForestViewModel @Inject constructor(history: HistoryRepository) : ViewModel() {
    val selection = PeriodSelection()

    val ui = selection.state.flatMapLatest { state ->
        val zone = ZoneId.systemDefault()
        val period = state.period(zone)
        history.observeStartedBetween(period.startMs(zone), period.endMs(zone)).map { ForestUiState(state, it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ForestUiState())
}

/** S08: every tree of the period, the withered ones included. */
@Composable
fun ForestScreen(viewModel: ForestViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val spacing = GharsTheme.spacing
    LazyVerticalGrid(
        columns = GridCells.Adaptive(GharsTheme.sizes.forestCell),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.space4),
        horizontalArrangement = Arrangement.spacedBy(spacing.space2),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.space3)) {
                PeriodSwitcher(ui.selection, viewModel.selection::setKind, viewModel.selection::back, viewModel.selection::forward)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
                    StatTile(stringResource(R.string.total_focus), durationText(ui.focusMinutes), Modifier.weight(1.4f))
                    StatTile(stringResource(R.string.count_grown), ui.grown.toString(), Modifier.weight(1f))
                    StatTile(stringResource(R.string.count_withered), ui.withered.toString(), Modifier.weight(1f))
                }
            }
        }
        if (ui.trees.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                // The very first visit gets the welcoming line; later empty periods a plain one.
                val first = ui.selection.stepsBack == 0
                EmptyState(stringResource(if (first) R.string.forest_empty else R.string.forest_period_empty))
            }
        }
        items(ui.trees, key = { it.id }) { tree ->
            val name = stringResource(SpeciesCatalog.get(tree.speciesId).name)
            val minutes = pluralStringResource(R.plurals.duration_minutes, tree.plannedMinutes, tree.plannedMinutes)
            TreeView(
                speciesId = tree.speciesId,
                stage = SessionRules.STAGES,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                withered = !tree.success,
                description = stringResource(
                    R.string.cd_forest_tree,
                    if (tree.success) name else stringResource(R.string.cd_tree_withered),
                    minutes,
                ),
            )
        }
    }
}
