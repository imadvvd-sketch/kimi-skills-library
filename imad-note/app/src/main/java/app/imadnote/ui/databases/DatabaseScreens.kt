package app.imadnote.ui.databases

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.imadnote.LocalAppContainer
import app.imadnote.R
import app.imadnote.data.db.CollectionEntity
import app.imadnote.data.db.PropertyEntity
import app.imadnote.data.db.PropertyType
import app.imadnote.data.db.ViewEntity
import app.imadnote.data.db.ViewType
import app.imadnote.domain.db.DbJson
import app.imadnote.domain.db.Query
import app.imadnote.domain.db.Row as DbRow
import app.imadnote.domain.db.Value
import app.imadnote.domain.db.ViewConfig
import app.imadnote.ui.appViewModel
import app.imadnote.ui.components.AppCard
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.ChoiceDialog
import app.imadnote.ui.components.ColorDot
import app.imadnote.ui.components.ConfirmDialog
import app.imadnote.ui.components.EmojiTile
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.SkeletonList
import app.imadnote.ui.components.TextInputDialog
import app.imadnote.ui.components.currentLocale
import app.imadnote.ui.components.optionColor
import app.imadnote.ui.editor.EmojiPickerDialog
import app.imadnote.ui.theme.ImadTheme
import app.imadnote.ui.workspaceId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.WeekFields

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun DatabasesScreen(onOpenMenu: (() -> Unit)?, onOpen: (String) -> Unit) {
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val wsFlow = remember { container.workspaceId() }
    val ws by wsFlow.collectAsStateWithLifecycle(null)
    val collections by remember(ws) { ws?.let { container.collections.observeAll(it) } ?: flowOf(emptyList()) }.collectAsStateWithLifecycle(null)
    var creating by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = stringResource(R.string.nav_databases), onOpenMenu = onOpenMenu,
        fabIcon = Icons.Rounded.Add, fabLabel = stringResource(R.string.db_new), onFab = { creating = true },
    ) {
        val list = collections
        when {
            list == null -> SkeletonList()
            list.isEmpty() -> EmptyState(Icons.Rounded.Storage, stringResource(R.string.db_empty_title), stringResource(R.string.db_empty_message),
                actionLabel = stringResource(R.string.db_new), onAction = { creating = true })
            else -> LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(list, key = { it.id }) { c -> DatabaseCard(c) { onOpen(c.id) } }
            }
        }
    }
    if (creating) {
        TextInputDialog(stringResource(R.string.db_new), stringResource(R.string.action_create), stringResource(R.string.action_cancel), placeholder = stringResource(R.string.db_name_hint),
            onConfirm = { name -> creating = false; ws?.let { id -> scope.launch { onOpen(container.collections.create(id, name).id) } } }, onDismiss = { creating = false })
    }
}

@Composable
private fun DatabaseCard(collection: CollectionEntity, onClick: () -> Unit) {
    val container = LocalAppContainer.current
    val count by remember(collection.id) { container.collections.observeRecordCount(collection.id) }.collectAsStateWithLifecycle(0)
    AppCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiTile(collection.icon, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(collection.name, style = MaterialTheme.typography.titleMedium, color = ImadTheme.colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(pluralStringResource(R.plurals.db_record_count, count, count), style = MaterialTheme.typography.bodySmall, color = ImadTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
fun DatabaseScreen(collectionId: String, onBack: () -> Unit) {
    val viewModel = appViewModel(key = "db-$collectionId") { DatabaseViewModel(collectionId, it) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val missing by viewModel.missing.collectAsStateWithLifecycle()
    var viewId by rememberSaveable { mutableStateOf<String?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var openRecord by rememberSaveable { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf<String?>(null) }
    var propertyMenu by remember { mutableStateOf<PropertyEntity?>(null) }
    var menu by remember { mutableStateOf(false) }

    val current = state
    ScreenScaffold(
        title = current?.collection?.let { "${it.icon}  ${it.name}" }.orEmpty(), onBack = onBack,
        actions = {
            AppIconButton(Icons.Rounded.Search, stringResource(R.string.nav_search), { searching = !searching; if (!searching) search = "" })
            Box {
                AppIconButton(Icons.Rounded.MoreVert, stringResource(R.string.action_more), { menu = true })
                DropdownMenu(menu, { menu = false }, containerColor = ImadTheme.colors.surface) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.db_rename)) }, onClick = { menu = false; dialog = "rename" })
                    DropdownMenuItem(text = { Text(stringResource(R.string.page_choose_icon)) }, onClick = { menu = false; dialog = "icon" })
                    DropdownMenuItem(text = { Text(stringResource(R.string.db_add_property)) }, onClick = { menu = false; dialog = "addProperty" })
                    DropdownMenuItem(text = { Text(stringResource(R.string.db_delete), color = ImadTheme.colors.danger) }, onClick = { menu = false; dialog = "delete" })
                }
            }
        },
        fabIcon = if (current != null) Icons.Rounded.Add else null, fabLabel = stringResource(R.string.db_new_record),
        onFab = { viewModel.addRecord { openRecord = it } },
    ) {
        if (current == null) {
            if (missing) EmptyState(Icons.Rounded.Close, stringResource(R.string.db_not_found), "") else SkeletonList()
            return@ScreenScaffold
        }
        val view = current.views.firstOrNull { it.id == viewId } ?: current.views.firstOrNull() ?: return@ScreenScaffold
        val config = DbJson.view(view.config)
        val rows = Query.apply(current.rows, current.properties, config, search)
        val visible = current.properties.filter { it.type == PropertyType.TITLE || it.id !in config.hidden }

        Column(Modifier.fillMaxSize()) {
            ViewTabs(current.views, view, onSelect = { viewId = it.id }, onNew = { dialog = "newView" }, onRename = { dialog = "renameView" }, onDelete = { viewModel.deleteView(view); viewId = null })
            if (searching) AppTextField(search, { search = it }, stringResource(R.string.db_search_hint), Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp))
            Toolbar(view, config, onOpen = { dialog = it })
            Box(Modifier.weight(1f)) {
                val open: (String) -> Unit = { openRecord = it }
                when (view.type) {
                    ViewType.TABLE -> TableView(current, rows, visible, config, open, onHeader = { propertyMenu = it }, onAddProperty = { dialog = "addProperty" }, onAdd = { viewModel.addRecord(it) { id -> openRecord = id } }, viewModel)
                    ViewType.BOARD -> BoardView(current, rows, visible, config, open, viewModel)
                    ViewType.CALENDAR -> CalendarView(current, rows, config, open, viewModel)
                    ViewType.GALLERY -> GalleryView(current, rows, visible, open)
                    ViewType.LIST -> ListView(current, rows, visible, config, open)
                }
            }
        }

        openRecord?.let { RecordSheet(it, current, viewModel) { openRecord = null } }
        propertyMenu?.let { PropertyMenu(it, view, current, viewModel) { propertyMenu = null } }
        when (dialog) {
            "filter", "sort", "group", "properties" -> ViewSettingsDialog(dialog!!, view, current, viewModel) { dialog = null }
            "addProperty" -> AddPropertyDialog({ name, type ->
                dialog = null
                viewModel.addProperty(name, type) { created -> if (type == PropertyType.FORMULA || type == PropertyType.RELATION || type == PropertyType.SELECT || type == PropertyType.MULTI_SELECT) propertyMenu = created }
            }, { dialog = null })
            "newView" -> ChoiceDialog(stringResource(R.string.db_new_view), ViewType.entries.map { it to stringResource(it.label()) }, null,
                { type -> viewModel.addView("", type) { viewId = it } }, { dialog = null })
            "renameView" -> TextInputDialog(stringResource(R.string.db_rename_view), stringResource(R.string.action_save), stringResource(R.string.action_cancel), initial = view.name,
                onConfirm = { viewModel.renameView(view, it); dialog = null }, onDismiss = { dialog = null })
            "rename" -> TextInputDialog(stringResource(R.string.db_rename), stringResource(R.string.action_save), stringResource(R.string.action_cancel), initial = current.collection.name,
                onConfirm = { viewModel.rename(it); dialog = null }, onDismiss = { dialog = null })
            "icon" -> EmojiPickerDialog(stringResource(R.string.page_choose_icon), { e -> dialog = null; if (e != null) viewModel.setIcon(e) }, { dialog = null }, allowRemove = false)
            "delete" -> ConfirmDialog(stringResource(R.string.db_delete), stringResource(R.string.db_delete_message, current.collection.name), stringResource(R.string.action_delete), stringResource(R.string.action_cancel),
                destructive = true, onConfirm = { dialog = null; viewModel.delete(); onBack() }, onDismiss = { dialog = null })
        }
    }
}

@Composable
private fun ViewTabs(views: List<ViewEntity>, selected: ViewEntity, onSelect: (ViewEntity) -> Unit, onNew: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val colors = ImadTheme.colors
    var menuFor by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        for (v in views) {
            val active = v.id == selected.id
            Box {
                Row(
                    Modifier.padding(end = 4.dp).clip(RoundedCornerShape(10.dp)).background(if (active) colors.primarySoft else Color.Transparent)
                        .combinedClickable(role = Role.Tab, onClick = { onSelect(v) }, onLongClick = { onSelect(v); menuFor = v.id })
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(v.type.icon(), null, tint = if (active) colors.primary else colors.textSecondary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(v.name, style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium), color = if (active) colors.primary else colors.textSecondary)
                }
                DropdownMenu(menuFor == v.id, { menuFor = null }, containerColor = colors.surface) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.db_rename_view)) }, onClick = { menuFor = null; onRename() })
                    if (views.size > 1) DropdownMenuItem(text = { Text(stringResource(R.string.db_delete_view), color = colors.danger) }, onClick = { menuFor = null; onDelete() })
                }
            }
        }
        AppIconButton(Icons.Rounded.Add, stringResource(R.string.db_new_view), onNew)
    }
}

@Composable
private fun Toolbar(view: ViewEntity, config: ViewConfig, onOpen: (String) -> Unit) {
    val colors = ImadTheme.colors
    @Composable
    fun tool(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, key: String) {
        Row(
            Modifier.padding(end = 6.dp).clip(RoundedCornerShape(50)).border(1.dp, if (active) colors.primary else colors.border, RoundedCornerShape(50))
                .clickable(role = Role.Button) { onOpen(key) }.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = if (active) colors.primary else colors.textSecondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) colors.primary else colors.textSecondary)
        }
    }
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp)) {
        tool(Icons.Rounded.FilterList, stringResource(R.string.db_filter) + if (config.filters.isNotEmpty()) " · ${config.filters.size}" else "", config.filters.isNotEmpty(), "filter")
        tool(Icons.Rounded.Sort, stringResource(R.string.db_sort) + if (config.sorts.isNotEmpty()) " · ${config.sorts.size}" else "", config.sorts.isNotEmpty(), "sort")
        if (view.type != ViewType.GALLERY) tool(Icons.Rounded.Workspaces, stringResource(if (view.type == ViewType.CALENDAR) R.string.db_date_property else R.string.db_group), config.groupBy != null || config.dateProperty != null, "group")
        tool(Icons.Rounded.ViewColumn, stringResource(R.string.db_properties), config.hidden.isNotEmpty(), "properties")
    }
}

private fun colWidth(p: PropertyEntity): Dp = if (p.type == PropertyType.TITLE) 200.dp else if (p.type == PropertyType.CHECKBOX) 96.dp else 160.dp

@Composable
private fun TableView(
    state: DbState, rows: List<DbRow>, visible: List<PropertyEntity>, config: ViewConfig, onOpen: (String) -> Unit,
    onHeader: (PropertyEntity) -> Unit, onAddProperty: () -> Unit, onAdd: (Map<String, String>) -> Unit, viewModel: DatabaseViewModel,
) {
    val colors = ImadTheme.colors
    val ordered = visible.sortedBy { if (it.type == PropertyType.TITLE) -1 else it.sortIndex }
    val width = ordered.fold(56.dp) { acc, p -> acc + colWidth(p) }
    val groupProp = config.groupBy?.let { id -> state.properties.firstOrNull { it.id == id } }
    val groups = Query.group(rows, groupProp)
    Box(Modifier.fillMaxSize().horizontalScroll(rememberScrollState())) {
        LazyColumn(Modifier.width(width).fillMaxHeight().testTag("db-table"), contentPadding = PaddingValues(start = 16.dp, bottom = 96.dp)) {
            item(key = "header") {
                Row(Modifier.background(colors.surfaceAlt, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))) {
                    for (p in ordered) {
                        Row(
                            Modifier.width(colWidth(p)).clickable(role = Role.Button) { onHeader(p) }.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(p.type.icon(), null, tint = colors.textTertiary, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(p.name, style = MaterialTheme.typography.labelLarge, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Box(Modifier.width(40.dp).clickable(role = Role.Button, onClick = onAddProperty).padding(10.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Add, stringResource(R.string.db_add_property), tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
            for (group in groups) {
                if (groupProp != null) {
                    item(key = "g-${group.key}") { GroupHeader(group.label.ifEmpty { stringResource(R.string.db_no_value) }, group.color, group.rows.size) }
                }
                items(group.rows, key = { "${group.key}-${it.id}" }) { row ->
                    Column {
                        Row(Modifier.background(colors.surface).clickable { onOpen(row.id) }, verticalAlignment = Alignment.CenterVertically) {
                            for (p in ordered) {
                                Box(Modifier.width(colWidth(p)).heightIn(min = 44.dp).padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.CenterStart) {
                                    if (p.type == PropertyType.TITLE) Text(rowTitle(row, state), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    else if (p.type == PropertyType.CHECKBOX) Box(Modifier.clickable { viewModel.setCell(row.id, p.id, if (row.value(p.id) == Value.Bool(true)) "" else "true") }) { CellValue(row, p) }
                                    else CellValue(row, p, maxPills = 2)
                                }
                            }
                        }
                        HorizontalDivider(color = colors.border)
                    }
                }
                item(key = "add-${group.key}") {
                    Row(
                        Modifier.fillMaxWidth().clickable { onAdd(if (groupProp != null && group.key != null) mapOf(groupProp.id to groupValue(groupProp, group.key)) else emptyMap()) }.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Add, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.db_new_record), style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary)
                    }
                }
            }
        }
    }
}

/** Stored value that puts a new record into a group. */
private fun groupValue(property: PropertyEntity, key: String): String = when (property.type) {
    PropertyType.MULTI_SELECT -> DbJson.encodeIds(listOf(key))
    PropertyType.CHECKBOX -> if (key == "true") "true" else ""
    else -> key
}

@Composable
private fun GroupHeader(label: String, color: String?, count: Int) {
    Row(Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        ColorDot(if (color != null) optionColor(color, ImadTheme.colors) else ImadTheme.colors.textTertiary)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = ImadTheme.colors.text)
        Spacer(Modifier.width(6.dp))
        Text("$count", style = MaterialTheme.typography.labelMedium, color = ImadTheme.colors.textTertiary)
    }
}

@Composable
private fun RecordCard(row: DbRow, state: DbState, visible: List<PropertyEntity>, onOpen: () -> Unit, modifier: Modifier = Modifier, onLongPress: (() -> Unit)? = null, skip: String? = null) {
    val colors = ImadTheme.colors
    Column(
        modifier.clip(MaterialTheme.shapes.medium).background(colors.surface).border(1.dp, colors.border, MaterialTheme.shapes.medium)
            .combinedClickable(onClick = onOpen, onLongClick = onLongPress).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(rowTitle(row, state), style = MaterialTheme.typography.titleSmall, color = colors.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        for (p in visible.filter { it.type != PropertyType.TITLE && it.id != skip && !row.value(it.id).isEmpty && row.value(it.id) != Value.Bool(false) }.take(4)) {
            CellValue(row, p, maxPills = 2)
        }
    }
}

@Composable
private fun BoardView(state: DbState, rows: List<DbRow>, visible: List<PropertyEntity>, config: ViewConfig, onOpen: (String) -> Unit, viewModel: DatabaseViewModel) {
    val colors = ImadTheme.colors
    val groupProp = (config.groupBy?.let { id -> state.properties.firstOrNull { it.id == id } } ?: state.properties.firstOrNull { it.type == PropertyType.STATUS || it.type == PropertyType.SELECT })
        ?.takeIf { Query.groupable(it.type) }
    if (groupProp == null) {
        EmptyState(Icons.Rounded.Workspaces, stringResource(R.string.db_no_group_property), "")
        return
    }
    var moving by remember { mutableStateOf<DbRow?>(null) }
    val groups = Query.group(rows, groupProp)
    LazyRow(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
        items(groups, key = { it.key ?: "none" }) { group ->
            Column(Modifier.width(272.dp).fillMaxHeight().clip(MaterialTheme.shapes.medium).background(colors.surfaceAlt.copy(alpha = 0.6f)).padding(8.dp)) {
                GroupHeader(group.label.ifEmpty { stringResource(R.string.db_no_value) }, group.color, group.rows.size)
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(group.rows, key = { it.id }) { row ->
                        RecordCard(row, state, visible, { onOpen(row.id) }, Modifier.fillMaxWidth(), onLongPress = { moving = row }, skip = groupProp.id)
                    }
                    item {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                                viewModel.addRecord(if (group.key != null) mapOf(groupProp.id to groupValue(groupProp, group.key)) else emptyMap()) { onOpen(it) }
                            }.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Add, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.db_new_record), style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary)
                        }
                    }
                }
            }
        }
    }
    moving?.let { row ->
        ChoiceDialog<String?>(
            stringResource(R.string.db_move_to), groups.map { it.key to it.label.ifEmpty { stringResource(R.string.db_no_value) } }, null,
            { key -> viewModel.setCell(row.id, groupProp.id, if (key == null) "" else groupValue(groupProp, key)) }, { moving = null },
        )
    }
}

@Composable
private fun CalendarView(state: DbState, rows: List<DbRow>, config: ViewConfig, onOpen: (String) -> Unit, viewModel: DatabaseViewModel) {
    val colors = ImadTheme.colors
    val dateProp = config.dateProperty?.let { id -> state.properties.firstOrNull { it.id == id } } ?: state.properties.firstOrNull { it.type == PropertyType.DATE }
    if (dateProp == null) {
        EmptyState(Icons.Rounded.Workspaces, stringResource(R.string.db_no_date_property), "")
        return
    }
    val locale = currentLocale()
    val firstDay = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    var month by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val ym = YearMonth.parse(month)
    var selected by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    val selectedDay = LocalDate.parse(selected)
    val byDay = rows.groupBy { (it.value(dateProp.id) as? Value.Date)?.v }
    val start = ym.atDay(1).let { it.minusDays(((it.dayOfWeek.value - firstDay.value + 7) % 7).toLong()) }
    val weeks = ((ym.atEndOfMonth().toEpochDay() - start.toEpochDay()) / 7 + 1).toInt()
    LazyColumn(contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, stringResource(R.string.cal_previous), { month = ym.minusMonths(1).toString() })
                Text(ym.month.getDisplayName(TextStyle.FULL_STANDALONE, locale) + " " + ym.year, style = MaterialTheme.typography.titleMedium, color = colors.text, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                AppIconButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, stringResource(R.string.cal_next), { month = ym.plusMonths(1).toString() })
            }
        }
        item {
            Row { for (i in 0 until 7) Text(firstDay.plus(i.toLong()).getDisplayName(TextStyle.NARROW_STANDALONE, locale), style = MaterialTheme.typography.labelMedium, color = colors.textTertiary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        items(weeks) { w ->
            Row {
                for (d in 0 until 7) {
                    val day = start.plusDays((w * 7 + d).toLong())
                    val items = byDay[day].orEmpty()
                    Column(
                        Modifier.weight(1f).aspectRatio(0.75f).padding(1.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (day == selectedDay) colors.primarySoft else colors.surface).clickable { selected = day.toString() }.padding(3.dp),
                    ) {
                        Text("${day.dayOfMonth}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (day == LocalDate.now()) FontWeight.Bold else FontWeight.Normal),
                            color = if (day.month != ym.month) colors.textTertiary else if (day == LocalDate.now()) colors.primary else colors.text)
                        for (row in items.take(2)) {
                            Text(rowTitle(row, state), style = MaterialTheme.typography.labelSmall, color = colors.primary, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp).clip(RoundedCornerShape(3.dp)).background(colors.primary.copy(alpha = 0.12f)).padding(horizontal = 2.dp))
                        }
                        if (items.size > 2) Text("+${items.size - 2}", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
                    }
                }
            }
        }
        item {
            Row(Modifier.padding(top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(app.imadnote.ui.components.formatDay(selectedDay), style = MaterialTheme.typography.titleSmall, color = colors.textSecondary, modifier = Modifier.weight(1f))
                AppIconButton(Icons.Rounded.Add, stringResource(R.string.db_new_record), { viewModel.addRecord(mapOf(dateProp.id to selectedDay.toString())) { onOpen(it) } }, tint = colors.primary)
            }
        }
        items(byDay[selectedDay].orEmpty(), key = { it.id }) { row ->
            RecordCard(row, state, emptyList(), { onOpen(row.id) }, Modifier.fillMaxWidth().padding(vertical = 3.dp))
        }
    }
}

@Composable
private fun GalleryView(state: DbState, rows: List<DbRow>, visible: List<PropertyEntity>, onOpen: (String) -> Unit) {
    if (rows.isEmpty()) { Text(stringResource(R.string.db_no_records), color = ImadTheme.colors.textSecondary, modifier = Modifier.padding(24.dp)); return }
    LazyVerticalGrid(GridCells.Adaptive(160.dp), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(rows, key = { it.id }) { row ->
            Column(Modifier.clip(MaterialTheme.shapes.medium).border(1.dp, ImadTheme.colors.border, MaterialTheme.shapes.medium).background(ImadTheme.colors.surface).clickable { onOpen(row.id) }) {
                Box(Modifier.fillMaxWidth().height(72.dp).background(ImadTheme.colors.primarySoft), contentAlignment = Alignment.Center) {
                    Text(state.collection.icon, style = MaterialTheme.typography.headlineMedium)
                }
                RecordCard(row, state, visible, { onOpen(row.id) }, Modifier.fillMaxWidth().border(0.dp, Color.Transparent))
            }
        }
    }
}

@Composable
private fun ListView(state: DbState, rows: List<DbRow>, visible: List<PropertyEntity>, config: ViewConfig, onOpen: (String) -> Unit) {
    val colors = ImadTheme.colors
    val groupProp = config.groupBy?.let { id -> state.properties.firstOrNull { it.id == id } }
    val groups = Query.group(rows, groupProp)
    LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (rows.isEmpty()) item { Text(stringResource(R.string.db_no_records), color = colors.textSecondary, modifier = Modifier.padding(vertical = 24.dp)) }
        for (group in groups) {
            if (groupProp != null && group.rows.isNotEmpty()) item(key = "g-${group.key}") { GroupHeader(group.label.ifEmpty { stringResource(R.string.db_no_value) }, group.color, group.rows.size) }
            items(group.rows, key = { "${group.key}-${it.id}" }) { row ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(colors.surface).clickable { onOpen(row.id) }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(rowTitle(row, state), style = MaterialTheme.typography.bodyLarge, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        for (p in visible.filter { it.type != PropertyType.TITLE && it.id != groupProp?.id && !row.value(it.id).isEmpty && row.value(it.id) != Value.Bool(false) }.take(2)) CellValue(row, p, maxPills = 1)
                    }
                }
            }
        }
    }
}
