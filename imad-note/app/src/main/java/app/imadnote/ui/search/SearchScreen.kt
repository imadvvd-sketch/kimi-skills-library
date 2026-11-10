package app.imadnote.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.imadnote.AppContainer
import app.imadnote.R
import app.imadnote.data.db.SearchHistoryEntity
import app.imadnote.data.repo.SearchIndexer
import app.imadnote.ui.appViewModel
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.ChipRow
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.theme.ImadColors
import app.imadnote.ui.theme.ImadTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResult(val kind: String, val refId: String, val parentRef: String, val title: String, val snippet: String, val context: String?)

enum class SearchFilter(val kinds: Set<String>) {
    ALL(emptySet()),
    PAGES(setOf(SearchIndexer.PAGE, SearchIndexer.BLOCK)),
    TASKS(setOf(SearchIndexer.TASK)),
    PROJECTS(setOf(SearchIndexer.PROJECT)),
    RECORDS(setOf(SearchIndexer.RECORD)),
    EVENTS(setOf(SearchIndexer.EVENT)),
    FILES(setOf(SearchIndexer.ATTACHMENT)),
}

/** Turns typed words into an FTS query: every word must match, as a prefix. */
fun ftsQuery(text: String): String? =
    text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { "\"" + it.replace("\"", "\"\"") + "*\"" }.ifEmpty { null }

class SearchViewModel(private val container: AppContainer) : ViewModel() {
    private val dao = container.db.search()
    val history: StateFlow<List<SearchHistoryEntity>> = dao.observeHistory().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _results = MutableStateFlow<List<SearchResult>?>(null)
    val results: StateFlow<List<SearchResult>?> = _results.asStateFlow()

    suspend fun search(text: String) {
        val query = ftsQuery(text)
        val ws = container.workspaces.current.first()?.id
        if (query == null || ws == null) { _results.value = null; return }
        val hits = runCatching { dao.search(ws, query) }.getOrDefault(emptyList())
        // Content hits show the page or database they belong to.
        _results.value = hits.map { hit ->
            val context = when (hit.kind) {
                SearchIndexer.BLOCK -> container.db.pages().get(hit.parentRef)?.title
                SearchIndexer.RECORD -> container.db.collections().get(hit.parentRef)?.name
                SearchIndexer.TASK -> if (hit.parentRef != hit.refId) container.db.tasks().get(hit.parentRef)?.title else null
                else -> null
            }
            SearchResult(hit.kind, hit.refId, hit.parentRef, hit.title, hit.snippet, context)
        }
    }

    fun remember(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { dao.remember(SearchHistoryEntity(text.trim(), container.clock.now())) }
    }

    fun forget(text: String) { viewModelScope.launch { dao.forget(text) } }
    fun clearHistory() { viewModelScope.launch { dao.clearHistory() } }
}

@Composable
fun SearchScreen(onBack: () -> Unit, onOpen: (SearchResult) -> Unit) {
    val viewModel = appViewModel { SearchViewModel(it) }
    val history by viewModel.history.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SearchFilter.ALL) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(query) { delay(150); viewModel.search(query) }
    val colors = ImadTheme.colors

    ScreenScaffold(title = stringResource(R.string.nav_search), onBack = onBack) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            item {
                AppTextField(
                    query, { query = it }, stringResource(R.string.search_hint), Modifier.fillMaxWidth().focusRequester(focus).testTag("search-field"),
                    imeAction = ImeAction.Search, onDone = { viewModel.remember(query) },
                    leading = { Icon(Icons.Rounded.Search, null, tint = colors.textTertiary, modifier = Modifier.size(20.dp)) },
                    trailing = { if (query.isNotEmpty()) AppIconButton(Icons.Rounded.Close, stringResource(R.string.action_clear), { query = "" }, Modifier.size(28.dp)) },
                )
            }
            item {
                ChipRow(
                    listOf(
                        SearchFilter.ALL to stringResource(R.string.search_all), SearchFilter.PAGES to stringResource(R.string.search_pages),
                        SearchFilter.TASKS to stringResource(R.string.search_tasks), SearchFilter.PROJECTS to stringResource(R.string.search_projects),
                        SearchFilter.RECORDS to stringResource(R.string.search_records), SearchFilter.EVENTS to stringResource(R.string.search_events),
                        SearchFilter.FILES to stringResource(R.string.search_files),
                    ),
                    filter, { filter = it }, Modifier.padding(vertical = 6.dp),
                )
            }
            val list = results
            if (list == null) {
                if (history.isEmpty()) item { EmptyState(Icons.Rounded.Search, stringResource(R.string.search_empty_title), stringResource(R.string.search_empty_message)) }
                else {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.search_recent), style = MaterialTheme.typography.titleSmall, color = colors.textSecondary, modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::clearHistory) { Text(stringResource(R.string.search_clear_history), color = colors.textSecondary) }
                        }
                    }
                    items(history, key = { it.query }) { h ->
                        Row(Modifier.fillMaxWidth().clickable { query = h.query }.padding(start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.History, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(h.query, style = MaterialTheme.typography.bodyLarge, color = colors.text, modifier = Modifier.weight(1f))
                            AppIconButton(Icons.Rounded.Close, stringResource(R.string.action_remove), { viewModel.forget(h.query) })
                        }
                    }
                }
            } else {
                val shown = if (filter == SearchFilter.ALL) list else list.filter { it.kind in filter.kinds }
                if (shown.isEmpty()) item { Text(stringResource(R.string.search_no_results, query.trim()), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, modifier = Modifier.padding(vertical = 24.dp)) }
                items(shown, key = { "${it.kind}-${it.refId}" }) { result ->
                    ResultRow(result) { viewModel.remember(query); onOpen(result) }
                }
            }
        }
    }
}

/** \u0002 and \u0003 mark matches in FTS snippets. */
fun highlight(snippet: String, colors: ImadColors): AnnotatedString = buildAnnotatedString {
    var bold = false
    val chunk = StringBuilder()
    fun flush() {
        if (chunk.isEmpty()) return
        if (bold) withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.primary, background = colors.primarySoft)) { append(chunk) } else append(chunk)
        chunk.clear()
    }
    for (c in snippet) when (c) {
        '\u0002' -> { flush(); bold = true }
        '\u0003' -> { flush(); bold = false }
        else -> chunk.append(c)
    }
    flush()
}

@Composable
private fun ResultRow(result: SearchResult, onClick: () -> Unit) {
    val colors = ImadTheme.colors
    val icon = when (result.kind) {
        SearchIndexer.PAGE -> Icons.Rounded.Description
        SearchIndexer.BLOCK -> Icons.AutoMirrored.Rounded.Notes
        SearchIndexer.TASK -> Icons.Rounded.TaskAlt
        SearchIndexer.PROJECT -> Icons.Rounded.RocketLaunch
        SearchIndexer.RECORD -> Icons.Rounded.Storage
        SearchIndexer.EVENT -> Icons.Rounded.Event
        else -> Icons.AutoMirrored.Rounded.InsertDriveFile
    }
    Row(
        Modifier.fillMaxWidth().background(colors.surface, MaterialTheme.shapes.small).clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, null, tint = colors.primary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val plainSnippet = result.snippet.replace("\u0002", "").replace("\u0003", "")
            val snippetIsTitle = result.title.isNotBlank() && plainSnippet == result.title
            val headline = when {
                snippetIsTitle -> highlight(result.snippet, colors)
                result.title.isNotBlank() -> AnnotatedString(result.title)
                else -> AnnotatedString(result.context ?: stringResource(R.string.search_in_page))
            }
            Text(headline, style = MaterialTheme.typography.titleSmall, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (result.snippet.isNotBlank() && !snippetIsTitle) {
                Text(highlight(result.snippet, colors), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (result.context != null && result.title.isNotBlank()) Text(result.context, style = MaterialTheme.typography.labelSmall, color = colors.textTertiary, maxLines = 1)
        }
    }
}
