package app.ghars.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.data.db.SessionEntity
import app.ghars.data.db.TagEntity
import app.ghars.data.repo.HistoryRepository
import app.ghars.data.repo.TagRepository
import app.ghars.domain.session.SessionRules
import app.ghars.feature.history.EmptyState
import app.ghars.feature.history.TagChip
import app.ghars.feature.history.currentLocale
import app.ghars.feature.history.dateFormatter
import app.ghars.feature.tree.SpeciesCatalog
import app.ghars.feature.tree.TreeView
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class TimelineDay(val date: LocalDate, val sessions: List<SessionEntity>)

data class TimelineUiState(
    val loaded: Boolean = false,
    val days: List<TimelineDay> = emptyList(),
    val tags: Map<Long, TagEntity> = emptyMap(),
)

@HiltViewModel
class TimelineViewModel @Inject constructor(history: HistoryRepository, tags: TagRepository) : ViewModel() {
    val ui = combine(history.observeRecent(LIMIT), tags.observeAll()) { sessions, allTags ->
        val zone = ZoneId.systemDefault()
        TimelineUiState(
            loaded = true,
            days = sessions.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
                .map { TimelineDay(it.key, it.value) },
            tags = allTags.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimelineUiState())

    private companion object {
        /** Plenty for a list; older sessions remain in the forest and the statistics. */
        const val LIMIT = 300
    }
}

/** S09: sessions as a dated list, newest first. */
@Composable
fun TimelineScreen(viewModel: TimelineViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val spacing = GharsTheme.spacing
    val locale = currentLocale()
    if (ui.loaded && ui.days.isEmpty()) {
        EmptyState(stringResource(R.string.timeline_empty), Modifier.fillMaxSize())
        return
    }
    val dayFormat = dateFormatter("EEEE d MMMM", locale)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        ui.days.forEach { day ->
            item(key = "day-${day.date}") {
                Text(
                    text = dayFormat.format(day.date),
                    style = GharsTheme.type.label,
                    color = GharsTheme.colors.inkMuted,
                    modifier = Modifier.padding(top = spacing.space2).semantics { heading() },
                )
            }
            items(day.sessions, key = { it.id }) { session ->
                SessionRow(session, ui.tags[session.tagId])
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionEntity, tag: TagEntity?) {
    val colors = GharsTheme.colors
    val spacing = GharsTheme.spacing
    val zone = ZoneId.systemDefault()
    val time = dateFormatter("HH:mm", currentLocale())
    val success = session.status == "success"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GharsTheme.radius.mdShape)
            .background(colors.surface)
            .padding(spacing.space3)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The small tree shows the outcome by its shape; the text beside it says it in words.
        TreeView(
            speciesId = session.speciesId,
            stage = SessionRules.STAGES,
            modifier = Modifier.size(GharsTheme.sizes.touchTarget),
            withered = !success,
        )
        Spacer(Modifier.width(spacing.space3))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(SpeciesCatalog.get(session.speciesId).name) + " · " +
                    pluralStringResource(R.plurals.duration_minutes, session.plannedMinutes, session.plannedMinutes),
                style = GharsTheme.type.label,
                color = colors.ink,
            )
            Text(
                text = stringResource(
                    R.string.timeline_range,
                    time.format(Instant.ofEpochMilli(session.startedAt).atZone(zone)),
                    time.format(Instant.ofEpochMilli(session.endedAt).atZone(zone)),
                ) + " · " + stringResource(
                    when (session.status) {
                        "success" -> R.string.status_success
                        "given_up" -> R.string.status_given_up
                        else -> R.string.status_withered
                    },
                ),
                style = GharsTheme.type.caption,
                color = if (success) colors.inkMuted else colors.withered,
            )
        }
        if (tag != null) {
            Spacer(Modifier.width(spacing.space2))
            TagChip(tag)
        }
    }
}
