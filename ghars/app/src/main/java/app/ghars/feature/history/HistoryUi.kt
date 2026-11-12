package app.ghars.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.data.db.TagEntity
import app.ghars.domain.stats.Period
import app.ghars.domain.stats.PeriodKind
import app.ghars.domain.stats.Periods
import app.ghars.domain.stats.StatsCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DecimalStyle
import java.time.temporal.WeekFields
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Which period a history screen is looking at. Shared by the forest and the statistics. */
class PeriodSelection {
    data class State(val kind: PeriodKind = PeriodKind.Week, val stepsBack: Int = 0) {
        fun period(zone: ZoneId = ZoneId.systemDefault(), locale: Locale = Locale.getDefault()): Period =
            Periods.of(kind, LocalDate.now(zone), stepsBack, firstDayOfWeek(locale))
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun setKind(kind: PeriodKind) = _state.update { State(kind, 0) }
    fun back() = _state.update { it.copy(stepsBack = it.stepsBack + 1) }
    fun forward() = _state.update { it.copy(stepsBack = (it.stepsBack - 1).coerceAtLeast(0)) }
}

fun firstDayOfWeek(locale: Locale = Locale.getDefault()): DayOfWeek = WeekFields.of(locale).firstDayOfWeek

@Composable
@ReadOnlyComposable
fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** Localised month and day names, but always Western digits, as the visual identity asks. */
fun dateFormatter(pattern: String, locale: Locale): DateTimeFormatter =
    DateTimeFormatter.ofPattern(pattern, locale).withDecimalStyle(DecimalStyle.STANDARD)

fun periodLabel(period: Period, locale: Locale): String = when (period.kind) {
    PeriodKind.Day -> dateFormatter("EEEE d MMMM", locale).format(period.start)
    PeriodKind.Week -> {
        val day = dateFormatter("d MMM", locale)
        "${day.format(period.start)} – ${day.format(period.lastDay)}"
    }
    PeriodKind.Month -> dateFormatter("LLLL yyyy", locale).format(period.start)
    PeriodKind.Year -> dateFormatter("yyyy", locale).format(period.start)
}

/** "1h 25m", "2h" or "45m". */
@Composable
fun durationText(totalMinutes: Int): String {
    val (hours, minutes) = StatsCalculator.hoursAndMinutes(totalMinutes)
    return when {
        hours == 0 -> stringResource(R.string.duration_m, minutes)
        minutes == 0 -> stringResource(R.string.duration_h, hours)
        else -> stringResource(R.string.duration_hm, hours, minutes)
    }
}

/** Built-in tags are named by the app in the user's language; custom ones carry their own name. */
@Composable
fun tagLabel(tag: TagEntity?): String = when {
    tag == null -> stringResource(R.string.tag_none)
    !tag.isBuiltin -> tag.name
    else -> stringResource(
        when (tag.name) {
            "study" -> R.string.tag_study
            "work" -> R.string.tag_work
            "reading" -> R.string.tag_reading
            "sport" -> R.string.tag_sport
            else -> R.string.tag_none
        },
    )
}

@Composable
fun TagDot(tag: TagEntity?, modifier: Modifier = Modifier) {
    val colors = GharsTheme.colors
    Box(
        modifier
            .size(GharsTheme.spacing.space3)
            .clip(CircleShape)
            .then(
                // No tag: an empty ring, so "none" is a shape and not just a colour.
                if (tag == null) Modifier.border(GharsTheme.sizes.focusRing, colors.inkMuted, CircleShape)
                else Modifier.background(Color(tag.color)),
            ),
    )
}

/** A tag's dot and name on a pill. */
@Composable
fun TagChip(tag: TagEntity?, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(GharsTheme.radius.pillShape)
            .background(GharsTheme.colors.surfaceSunk)
            .padding(horizontal = GharsTheme.spacing.space3, vertical = GharsTheme.spacing.space1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagDot(tag)
        Spacer(Modifier.width(GharsTheme.spacing.space2))
        Text(text = tagLabel(tag), style = GharsTheme.type.label, color = GharsTheme.colors.ink, maxLines = 1)
    }
}

/** Day / week / month / year, with arrows to step through earlier periods. */
@Composable
fun PeriodSwitcher(
    state: PeriodSelection.State,
    onKind: (PeriodKind) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = GharsTheme.colors
    val spacing = GharsTheme.spacing
    val locale = currentLocale()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GharsTheme.radius.pillShape)
                .background(colors.surfaceSunk)
                .padding(spacing.space1)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(spacing.space1),
        ) {
            PeriodKind.entries.forEach { kind ->
                val selected = kind == state.kind
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = GharsTheme.sizes.touchTarget)
                        .clip(GharsTheme.radius.pillShape)
                        .then(
                            // Selected: a raised white pill with a brand outline, not colour alone.
                            if (selected) {
                                Modifier
                                    .background(colors.surface)
                                    .border(GharsTheme.sizes.focusRing, colors.brand, GharsTheme.radius.pillShape)
                            } else Modifier,
                        )
                        .selectable(selected = selected, role = Role.Tab, onClick = { onKind(kind) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(
                            when (kind) {
                                PeriodKind.Day -> R.string.period_day
                                PeriodKind.Week -> R.string.period_week
                                PeriodKind.Month -> R.string.period_month
                                PeriodKind.Year -> R.string.period_year
                            },
                        ),
                        style = GharsTheme.type.label,
                        color = colors.ink,
                    )
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_chevron_previous), stringResource(R.string.cd_period_previous), tint = colors.ink)
            }
            Text(
                text = periodLabel(state.period(locale = locale), locale),
                style = GharsTheme.type.label,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
            )
            IconButton(onClick = onForward, enabled = state.stepsBack > 0) {
                Icon(
                    painterResource(R.drawable.ic_chevron_next), stringResource(R.string.cd_period_next),
                    tint = if (state.stepsBack > 0) colors.ink else colors.line,
                )
            }
        }
    }
}

/** A labelled figure, such as total focus time. Label above, value below, both in ink. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, note: String? = null) {
    Column(
        modifier = modifier
            .clip(GharsTheme.radius.mdShape)
            .background(GharsTheme.colors.surface)
            .padding(GharsTheme.spacing.space4)
            .semantics(mergeDescendants = true) {},
    ) {
        Text(text = label, style = GharsTheme.type.caption, color = GharsTheme.colors.inkMuted)
        Text(text = value, style = GharsTheme.type.heading, color = GharsTheme.colors.ink)
        if (note != null) Text(text = note, style = GharsTheme.type.caption, color = GharsTheme.colors.inkMuted)
    }
}

/** The quiet sentence shown when there is nothing to show. */
@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(GharsTheme.spacing.space8), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = GharsTheme.type.body,
            color = GharsTheme.colors.inkMuted,
            textAlign = TextAlign.Center,
        )
    }
}
