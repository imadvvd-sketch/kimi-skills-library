package app.imadnote.ui.analytics

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.imadnote.R
import app.imadnote.domain.analytics.Analytics
import app.imadnote.domain.analytics.AnalyticsReport
import app.imadnote.ui.appViewModel
import app.imadnote.ui.components.AppCard
import app.imadnote.ui.components.ColorDot
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.SectionHeader
import app.imadnote.ui.components.SkeletonList
import app.imadnote.ui.components.currentLocale
import app.imadnote.ui.components.priorityColor
import app.imadnote.ui.components.storedColor
import app.imadnote.ui.projects.ProgressBar
import app.imadnote.ui.tasks.TasksViewModel
import app.imadnote.ui.tasks.priorityLabel
import app.imadnote.ui.theme.ImadTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

@Composable
fun AnalyticsScreen(onOpenMenu: (() -> Unit)?) {
    val tasks = appViewModel { TasksViewModel(it) }
    val data by tasks.data.collectAsStateWithLifecycle()
    val locale = currentLocale()
    ScreenScaffold(title = stringResource(R.string.nav_analytics), onOpenMenu = onOpenMenu) {
        val current = data
        if (current == null) {
            SkeletonList()
            return@ScreenScaffold
        }
        val report = remember(current, locale) { Analytics.compute(current.tasks, current.projects, LocalDate.now(), weekStart = WeekFields.of(locale).firstDayOfWeek) }
        if (report.isEmpty) {
            EmptyState(Icons.Rounded.Insights, stringResource(R.string.analytics_empty_title), stringResource(R.string.analytics_empty_message))
            return@ScreenScaffold
        }
        Report(report)
    }
}

@Composable
private fun Report(r: AnalyticsReport) {
    val colors = ImadTheme.colors
    val locale = currentLocale()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi(r.completed.toString(), stringResource(R.string.analytics_completed), colors.success, Modifier.weight(1f))
            Kpi(r.open.toString(), stringResource(R.string.analytics_pending), colors.primary, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Kpi(r.overdue.toString(), stringResource(R.string.analytics_overdue), if (r.overdue > 0) colors.danger else colors.textTertiary, Modifier.weight(1f))
            Kpi("${(r.completionRate * 100).toInt()}%", stringResource(R.string.analytics_rate), colors.navy, Modifier.weight(1f))
        }
        AppCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥 " + stringResource(R.string.analytics_streak, r.streak), style = MaterialTheme.typography.titleSmall, color = colors.text, modifier = Modifier.weight(1f))
                val diff = r.doneThisWeek - r.doneLastWeek
                Text(stringResource(R.string.analytics_vs_last_week, diff), style = MaterialTheme.typography.labelMedium, color = if (diff >= 0) colors.success else colors.danger)
            }
            Text(androidx.compose.ui.res.pluralStringResource(R.plurals.analytics_done_this_week, r.doneThisWeek, r.doneThisWeek), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        }

        SectionHeader(stringResource(R.string.analytics_last7))
        AppCard(Modifier.fillMaxWidth()) {
            Legend()
            Spacer(Modifier.height(10.dp))
            val summary = r.last7.joinToString { "${it.day}: ${it.done}/${it.created}" }
            BarChart(
                values = r.last7.map { listOf(it.done.toFloat(), it.created.toFloat()) },
                labels = r.last7.map { it.day.dayOfWeek.getDisplayName(TextStyle.SHORT_STANDALONE, locale) },
                seriesColors = listOf(colors.primary, colors.border),
                description = summary,
            )
        }

        SectionHeader(stringResource(R.string.analytics_weeks))
        AppCard(Modifier.fillMaxWidth()) {
            val fmt = remember(locale) { DateTimeFormatter.ofPattern("d/M", locale) }
            BarChart(
                values = r.weeks.map { listOf(it.done.toFloat()) },
                labels = r.weeks.mapIndexed { i, w -> if (i % 3 == 0 || i == r.weeks.lastIndex) w.weekStart.format(fmt) else "" },
                seriesColors = listOf(colors.success),
                description = r.weeks.joinToString { "${it.weekStart}: ${it.done}" },
            )
        }

        SectionHeader(stringResource(R.string.analytics_months))
        AppCard(Modifier.fillMaxWidth()) {
            Legend()
            Spacer(Modifier.height(10.dp))
            BarChart(
                values = r.months.map { listOf(it.done.toFloat(), it.created.toFloat()) },
                labels = r.months.map { it.month.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale) },
                seriesColors = listOf(colors.primary, colors.border),
                description = r.months.joinToString { "${it.month}: ${it.done}/${it.created}" },
            )
        }

        SectionHeader(stringResource(R.string.analytics_by_priority))
        AppCard(Modifier.fillMaxWidth()) {
            val max = (r.openByPriority.values.maxOrNull() ?: 0).coerceAtLeast(1)
            for ((level, count) in r.openByPriority) {
                Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(priorityLabel(level), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.width(96.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    ProgressBar(count.toFloat() / max, if (level > 0) priorityColor(level, colors) else colors.textTertiary, Modifier.weight(1f))
                    Text("$count", style = MaterialTheme.typography.labelLarge, color = colors.text, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
                }
            }
        }

        if (r.projects.isNotEmpty()) {
            SectionHeader(stringResource(R.string.analytics_projects))
            AppCard(Modifier.fillMaxWidth()) {
                for (p in r.projects) {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${p.project.icon} ${p.project.name}", style = MaterialTheme.typography.bodyMedium, color = colors.text, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${p.done}/${p.total}", style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
                    }
                    Spacer(Modifier.height(6.dp))
                    ProgressBar(p.fraction, storedColor(p.project.color, colors))
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun Kpi(value: String, label: String, color: Color, modifier: Modifier) {
    AppCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelMedium, color = ImadTheme.colors.textSecondary)
    }
}

@Composable
private fun Legend() {
    val colors = ImadTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        ColorDot(colors.primary); Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.analytics_legend_done), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
        Spacer(Modifier.width(16.dp))
        ColorDot(colors.border, 10.dp); Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.analytics_legend_created), style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
    }
}

/**
 * Grouped bar chart drawn on a Canvas. Each entry of [values] is one group (one bar per series).
 * The scale starts at zero; values are written above non-zero bars so no axis is needed.
 */
@Composable
private fun BarChart(values: List<List<Float>>, labels: List<String>, seriesColors: List<Color>, description: String) {
    val colors = ImadTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val max = (values.flatten().maxOrNull() ?: 0f).coerceAtLeast(1f)
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val valueStyle = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
    Column(Modifier.semantics { contentDescription = description }) {
        Canvas(Modifier.fillMaxWidth().height(140.dp)) {
            val groups = values.size
            val slot = size.width / groups
            val series = values.firstOrNull()?.size ?: 1
            val gap = slot * 0.28f
            val barWidth = ((slot - gap) / series).coerceAtMost(28.dp.toPx())
            val top = 16.dp.toPx()
            val usable = size.height - top
            drawLine(colors.border, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            values.forEachIndexed { g, group ->
                val index = if (rtl) groups - 1 - g else g
                val groupWidth = barWidth * series
                val startX = index * slot + (slot - groupWidth) / 2
                group.forEachIndexed { s, v ->
                    val h = usable * (v / max)
                    val x = startX + s * barWidth
                    if (v > 0) {
                        drawRoundRect(seriesColors[s % seriesColors.size], Offset(x + 1.dp.toPx(), size.height - h), Size(barWidth - 2.dp.toPx(), h), CornerRadius(4.dp.toPx()))
                        if (s == 0) {
                            val text = measurer.measure(v.toInt().toString(), valueStyle)
                            drawText(text, topLeft = Offset(x + (barWidth - text.size.width) / 2, size.height - h - text.size.height - 2.dp.toPx()))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row {
            for (label in labels) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textTertiary, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.weight(1f))
            }
        }
    }
}
