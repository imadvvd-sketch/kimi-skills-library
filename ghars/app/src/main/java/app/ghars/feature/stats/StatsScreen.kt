package app.ghars.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.data.db.TagEntity
import app.ghars.data.repo.HistoryRepository
import app.ghars.data.repo.TagRepository
import app.ghars.domain.stats.StatsCalculator
import app.ghars.domain.stats.StatsSummary
import app.ghars.domain.stats.Streak
import app.ghars.feature.history.EmptyState
import app.ghars.feature.history.PeriodSelection
import app.ghars.feature.history.PeriodSwitcher
import app.ghars.feature.history.StatTile
import app.ghars.feature.history.TagDot
import app.ghars.feature.history.currentLocale
import app.ghars.feature.history.durationText
import app.ghars.feature.history.firstDayOfWeek
import app.ghars.feature.history.tagLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle as DateTextStyle
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class StatsUiState(
    val selection: PeriodSelection.State = PeriodSelection.State(),
    val summary: StatsSummary? = null,
    val streak: Streak = Streak(0, 0),
    val tags: Map<Long, TagEntity> = emptyMap(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(history: HistoryRepository, tags: TagRepository) : ViewModel() {
    val selection = PeriodSelection()

    private val summary = selection.state.flatMapLatest { state ->
        val zone = ZoneId.systemDefault()
        val period = state.period(zone)
        history.observeStartedBetween(period.startMs(zone), period.endMs(zone))
            .map { state to StatsCalculator.summarize(it, zone, firstDayOfWeek()) }
    }

    val ui = combine(summary, history.observeSuccessStarts(), tags.observeAll()) { (state, summary), starts, allTags ->
        val zone = ZoneId.systemDefault()
        StatsUiState(
            selection = state,
            summary = summary,
            // The streak is about now, whatever period is being looked at.
            streak = StatsCalculator.streak(starts, zone, LocalDate.now(zone)),
            tags = allTags.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUiState())
}

/** S10: where the focus time went. */
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val spacing = GharsTheme.spacing
    val locale = currentLocale()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        StatTile(
            label = stringResource(R.string.stats_streak),
            value = pluralStringResource(R.plurals.days, ui.streak.current, ui.streak.current),
            note = stringResource(R.string.stats_streak_best, pluralStringResource(R.plurals.days, ui.streak.best, ui.streak.best)),
            modifier = Modifier.fillMaxWidth(),
        )
        PeriodSwitcher(ui.selection, viewModel.selection::setKind, viewModel.selection::back, viewModel.selection::forward)

        val summary = ui.summary ?: return@Column
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing.space3)) {
            StatTile(stringResource(R.string.total_focus), durationText(summary.focusMinutes), Modifier.weight(1.4f))
            StatTile(stringResource(R.string.count_grown), summary.grown.toString(), Modifier.weight(1f))
            StatTile(stringResource(R.string.count_withered), summary.withered.toString(), Modifier.weight(1f))
        }
        if (summary.focusMinutes == 0) {
            EmptyState(stringResource(R.string.stats_empty))
            return@Column
        }

        Section(stringResource(R.string.stats_by_tag)) {
            val max = summary.byTag.maxOf { it.minutes }
            summary.byTag.forEach { total ->
                TagBar(tag = total.tagId?.let(ui.tags::get), minutes = total.minutes, fraction = total.minutes.toFloat() / max)
            }
        }

        Section(stringResource(R.string.stats_by_hour)) {
            val best = summary.bestHour
            val caption = best?.let { stringResource(R.string.stats_best_hour, "%02d:00".format(java.util.Locale.ROOT, it)) }
            // A day runs left to right on a chart in every language, like a clock face reads clockwise.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                BarChart(
                    values = summary.byHour,
                    labels = List(24) { if (it % 6 == 0) it.toString() else null },
                    description = caption.orEmpty(),
                )
            }
            if (caption != null) Caption(caption)
        }

        Section(stringResource(R.string.stats_by_day)) {
            val caption = summary.bestWeekday?.let {
                stringResource(R.string.stats_best_day, it.getDisplayName(DateTextStyle.FULL, locale))
            }
            BarChart(
                values = summary.byWeekday,
                labels = summary.weekdays.map { it.getDisplayName(DateTextStyle.SHORT, locale) },
                description = caption.orEmpty(),
            )
            if (caption != null) Caption(caption)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val spacing = GharsTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GharsTheme.radius.mdShape)
            .background(GharsTheme.colors.surface)
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text(
            text = title,
            style = GharsTheme.type.heading,
            color = GharsTheme.colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

@Composable
private fun Caption(text: String) =
    Text(text = text, style = GharsTheme.type.caption, color = GharsTheme.colors.inkMuted)

/**
 * One tag's share as a labelled bar. Every bar is the brand colour: length carries the amount,
 * the name carries the identity. The tag's own colour is only the dot beside its name.
 */
@Composable
private fun TagBar(tag: TagEntity?, minutes: Int, fraction: Float) {
    val colors = GharsTheme.colors
    val spacing = GharsTheme.spacing
    Column(modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TagDot(tag)
            Spacer(Modifier.width(spacing.space2))
            Text(text = tagLabel(tag), style = GharsTheme.type.label, color = colors.ink, modifier = Modifier.weight(1f))
            Text(text = durationText(minutes), style = GharsTheme.type.label, color = colors.inkMuted)
        }
        Spacer(Modifier.height(spacing.space1))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.space2)
                .clip(GharsTheme.radius.pillShape)
                .background(colors.surfaceSunk),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                    .clip(GharsTheme.radius.pillShape)
                    .background(colors.brand),
            )
        }
    }
}

/**
 * A single-series column chart: thin columns rising from a hairline baseline, rounded at the top.
 * One colour, no legend needed; the sentence under it states the peak for those who cannot see it.
 */
@Composable
private fun BarChart(values: List<Int>, labels: List<String?>, description: String) {
    val colors = GharsTheme.colors
    val sizes = GharsTheme.sizes
    val max = values.max().coerceAtLeast(1)
    Column(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description }) {
        Row(
            modifier = Modifier.fillMaxWidth().height(sizes.chart),
            horizontalArrangement = Arrangement.spacedBy(sizes.barGap),
            verticalAlignment = Alignment.Bottom,
        ) {
            values.forEach { value ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(if (value == 0) 0f else (value.toFloat() / max).coerceAtLeast(0.03f))
                        .clip(RoundedCornerShape(topStart = sizes.barRadius, topEnd = sizes.barRadius))
                        .background(colors.brand),
                )
            }
        }
        Box(Modifier.fillMaxWidth().height(sizes.hairline).background(colors.line))
        Spacer(Modifier.height(GharsTheme.spacing.space1))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(sizes.barGap)) {
            labels.forEach { label ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
                    if (label != null) {
                        Text(
                            text = label,
                            style = GharsTheme.type.caption,
                            color = colors.inkMuted,
                            maxLines = 1,
                            softWrap = false,
                            // Hour labels are wider than their column; let them spill evenly.
                            modifier = Modifier.wrapContentWidth(unbounded = true),
                        )
                    }
                }
            }
        }
    }
}
