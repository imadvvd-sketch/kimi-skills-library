package app.imadnote.ui.databases

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.core.newId
import app.imadnote.data.db.PropertyEntity
import app.imadnote.data.db.PropertyType
import app.imadnote.data.db.ViewEntity
import app.imadnote.domain.db.Cells
import app.imadnote.domain.db.DbJson
import app.imadnote.domain.db.FilterOp
import app.imadnote.domain.db.Formula
import app.imadnote.domain.db.Query
import app.imadnote.domain.db.Row as DbRow
import app.imadnote.domain.db.SelectOption
import app.imadnote.domain.db.Value
import app.imadnote.domain.db.ViewConfig
import app.imadnote.domain.db.ViewFilter
import app.imadnote.domain.db.ViewSort
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.ChoiceDialog
import app.imadnote.ui.components.ConfirmDialog
import app.imadnote.ui.components.DatePickerModal
import app.imadnote.ui.components.SelectChip
import app.imadnote.ui.components.TextInputDialog
import app.imadnote.ui.components.formatDateTime
import app.imadnote.ui.components.formatDay
import app.imadnote.ui.components.optionColor
import app.imadnote.ui.editor.InlineColor
import app.imadnote.ui.theme.ImadTheme
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
private fun DialogFrame(title: String, onDismiss: () -> Unit, confirm: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) { content() } },
        confirmButton = confirm ?: { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_done)) } },
        containerColor = ImadTheme.colors.surface,
        shape = MaterialTheme.shapes.large,
    )
}

/** Editor for one record: every property with an input suited to its type. */
@Composable
fun RecordSheet(recordId: String, state: DbState, viewModel: DatabaseViewModel, onDismiss: () -> Unit) {
    val colors = ImadTheme.colors
    val row = state.rows.firstOrNull { it.id == recordId }
    var confirmDelete by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surface) {
        if (row == null) { onDismiss(); return@ModalBottomSheet }
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp).verticalScroll(rememberScrollState())) {
            state.title?.let { title -> TitleEditor(row, title, viewModel) }
            Spacer(Modifier.size(8.dp))
            for (property in state.properties.filter { it.type != PropertyType.TITLE }) {
                PropertyEditor(row, property, state, viewModel)
                HorizontalDivider(color = colors.border)
            }
            Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { viewModel.duplicateRecord(row.id); onDismiss() }) {
                    Icon(Icons.Rounded.ContentCopy, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.db_record_duplicate))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Rounded.DeleteOutline, null, Modifier.size(18.dp), tint = colors.danger); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.db_record_delete), color = colors.danger)
                }
            }
        }
    }
    if (confirmDelete && row != null) {
        ConfirmDialog(stringResource(R.string.db_record_delete), rowTitle(row, state), stringResource(R.string.action_delete), stringResource(R.string.action_cancel), destructive = true,
            onConfirm = { confirmDelete = false; viewModel.deleteRecord(row.id); onDismiss() }, onDismiss = { confirmDelete = false })
    }
}

@Composable
private fun TitleEditor(row: DbRow, title: PropertyEntity, viewModel: DatabaseViewModel) {
    val colors = ImadTheme.colors
    var text by remember(row.id) { mutableStateOf(row.raw[title.id].orEmpty()) }
    LaunchedEffect(text) { delay(350); if (text != row.raw[title.id].orEmpty()) viewModel.setCell(row.id, title.id, text) }
    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (text.isEmpty()) Text(stringResource(R.string.untitled), style = MaterialTheme.typography.headlineSmall, color = colors.textTertiary)
        BasicTextField(text, { text = it.replace("\n", "") }, textStyle = MaterialTheme.typography.headlineSmall.copy(color = colors.text), cursorBrush = SolidColor(colors.primary), modifier = Modifier.fillMaxWidth().testTag("record-title"))
    }
}

@Composable
private fun PropertyEditor(row: DbRow, property: PropertyEntity, state: DbState, viewModel: DatabaseViewModel) {
    val colors = ImadTheme.colors
    val context = LocalContext.current
    val config = state.config(property)
    var dialog by remember { mutableStateOf<String?>(null) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Row(Modifier.width(128.dp).padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(property.type.icon(), null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(property.name, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.weight(1f)) {
            when (property.type) {
                PropertyType.TEXT, PropertyType.URL, PropertyType.EMAIL, PropertyType.PHONE, PropertyType.PERSON, PropertyType.NUMBER -> {
                    var text by remember(row.id, property.id) { mutableStateOf(row.raw[property.id].orEmpty()) }
                    LaunchedEffect(text) {
                        delay(350)
                        val clean = if (property.type == PropertyType.NUMBER) text.replace(',', '.').trim().takeIf { it.isEmpty() || it.toDoubleOrNull() != null } ?: return@LaunchedEffect else text
                        if (clean != row.raw[property.id].orEmpty()) viewModel.setCell(row.id, property.id, clean)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.weight(1f).padding(vertical = 6.dp)) {
                            if (text.isEmpty()) Text(stringResource(R.string.db_empty_cell), style = MaterialTheme.typography.bodyLarge, color = colors.textTertiary)
                            BasicTextField(
                                text, { text = it }, textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text), cursorBrush = SolidColor(colors.primary),
                                keyboardOptions = KeyboardOptions(keyboardType = when (property.type) {
                                    PropertyType.NUMBER -> KeyboardType.Decimal; PropertyType.EMAIL -> KeyboardType.Email
                                    PropertyType.PHONE -> KeyboardType.Phone; PropertyType.URL -> KeyboardType.Uri; else -> KeyboardType.Text
                                }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (text.isNotBlank() && property.type in setOf(PropertyType.URL, PropertyType.EMAIL, PropertyType.PHONE)) {
                            AppIconButton(Icons.AutoMirrored.Rounded.OpenInNew, stringResource(R.string.action_open), { openContact(context, property.type, text.trim()) })
                        }
                    }
                }
                PropertyType.SELECT, PropertyType.STATUS, PropertyType.MULTI_SELECT -> {
                    val multi = property.type == PropertyType.MULTI_SELECT
                    val selected = if (multi) DbJson.ids(row.raw[property.id].orEmpty()).toSet() else setOfNotNull(row.raw[property.id])
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (option in config.options) {
                            SelectChip(option.name, option.id in selected, optionColor(option.color, colors)) {
                                val value = if (multi) DbJson.encodeIds((if (option.id in selected) selected - option.id else selected + option.id).let { s -> config.options.map { it.id }.filter { it in s } }).takeIf { it != "[]" }.orEmpty()
                                else if (option.id in selected) "" else option.id
                                viewModel.setCell(row.id, property.id, value)
                            }
                        }
                        SelectChip("+", false) { dialog = "option" }
                    }
                }
                PropertyType.DATE -> {
                    val date = (row.value(property.id) as? Value.Date)?.v
                    Text(
                        date?.let { formatDay(it) } ?: stringResource(R.string.db_empty_cell), style = MaterialTheme.typography.bodyLarge,
                        color = if (date == null) colors.textTertiary else colors.text,
                        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { dialog = "date" }.padding(vertical = 6.dp),
                    )
                }
                PropertyType.CHECKBOX -> Checkbox(
                    row.value(property.id) == Value.Bool(true), { viewModel.setCell(row.id, property.id, if (it) "true" else "") },
                    colors = CheckboxDefaults.colors(checkedColor = colors.primary, checkmarkColor = colors.onPrimary),
                )
                PropertyType.RELATION -> {
                    val links = row.links[property.id].orEmpty()
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (id in links) SelectChip("↗ " + state.titleOf(id).ifEmpty { stringResource(R.string.untitled) }, true) { dialog = "relation" }
                        SelectChip(if (config.relationTarget == null) stringResource(R.string.db_relation_target) else "+", false) { dialog = if (config.relationTarget == null) "target" else "relation" }
                    }
                }
                PropertyType.FORMULA -> {
                    val error = row.formulaErrors[property.id]
                    Text(error?.let { "⚠ $it" } ?: row.value(property.id).asText().ifEmpty { "—" }, style = MaterialTheme.typography.bodyLarge, color = if (error != null) colors.danger else colors.text, modifier = Modifier.padding(vertical = 6.dp))
                }
                PropertyType.CREATED_TIME, PropertyType.LAST_EDITED_TIME -> Text(
                    (row.value(property.id) as? Value.Date)?.let { formatDay(it.v) }.orEmpty(), style = MaterialTheme.typography.bodyLarge, color = colors.textSecondary, modifier = Modifier.padding(vertical = 6.dp),
                )
                PropertyType.TITLE -> Unit
            }
        }
    }
    when (dialog) {
        "date" -> DatePickerModal((row.value(property.id) as? Value.Date)?.v, { viewModel.setCell(row.id, property.id, it.toString()) }, { dialog = null }, onClear = { viewModel.setCell(row.id, property.id, "") })
        "option" -> TextInputDialog(stringResource(R.string.db_option_name_hint), stringResource(R.string.action_add), stringResource(R.string.action_cancel), placeholder = stringResource(R.string.db_option_name_hint),
            onConfirm = { name ->
                dialog = null
                val option = SelectOption(newId(), name, InlineColor.entries[config.options.size % InlineColor.entries.size].key)
                viewModel.updateConfig(property, config.copy(options = config.options + option))
                val value = if (property.type == PropertyType.MULTI_SELECT) DbJson.encodeIds(DbJson.ids(row.raw[property.id].orEmpty()) + option.id) else option.id
                viewModel.setCell(row.id, property.id, value)
            }, onDismiss = { dialog = null })
        "target" -> RelationTargetDialog(state, property, viewModel) { dialog = null }
        "relation" -> {
            val targetId = config.relationTarget
            val candidates = state.titles.filter { it.collectionId == targetId && it.recordId != row.id }
            var picked by remember { mutableStateOf(row.links[property.id].orEmpty().toSet()) }
            DialogFrame(property.name, { dialog = null }, confirm = { TextButton(onClick = { viewModel.setRelations(row.id, property.id, picked.toList()); dialog = null }) { Text(stringResource(R.string.action_done)) } }) {
                for (c in candidates) {
                    Row(Modifier.fillMaxWidth().clickable { picked = if (c.recordId in picked) picked - c.recordId else picked + c.recordId }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(c.recordId in picked, null, colors = CheckboxDefaults.colors(checkedColor = colors.primary))
                        Text(c.title.orEmpty().ifEmpty { stringResource(R.string.untitled) }, style = MaterialTheme.typography.bodyLarge, color = colors.text)
                    }
                }
                if (candidates.isEmpty()) Text(stringResource(R.string.db_no_records), color = colors.textSecondary)
            }
        }
    }
}

@Composable
fun RelationTargetDialog(state: DbState, property: PropertyEntity, viewModel: DatabaseViewModel, onDismiss: () -> Unit) {
    ChoiceDialog(
        stringResource(R.string.db_relation_pick), state.collections.map { it.id to "${it.icon} ${it.name}" }, state.config(property).relationTarget,
        { id -> viewModel.updateConfig(property, state.config(property).copy(relationTarget = id)) }, onDismiss,
    )
}

@Composable
fun AddPropertyDialog(onCreate: (String, PropertyType) -> Unit, onDismiss: () -> Unit) {
    val colors = ImadTheme.colors
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(PropertyType.TEXT) }
    DialogFrame(stringResource(R.string.db_add_property), onDismiss, confirm = {
        TextButton(onClick = { onCreate(name.ifBlank { "" }, type) }) { Text(stringResource(R.string.action_create)) }
    }) {
        AppTextField(name, { name = it }, stringResource(R.string.db_property_name_hint), Modifier.fillMaxWidth())
        Spacer(Modifier.size(12.dp))
        Text(stringResource(R.string.db_property_type), style = MaterialTheme.typography.titleSmall, color = colors.textSecondary)
        Spacer(Modifier.size(6.dp))
        for (t in PropertyType.entries.filter { it != PropertyType.TITLE }) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (t == type) colors.primarySoft else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable(role = Role.RadioButton) { type = t }.padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(t.icon(), null, tint = if (t == type) colors.primary else colors.textSecondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(t.label()), style = MaterialTheme.typography.bodyLarge, color = colors.text)
            }
        }
    }
}

@Composable
fun OptionsEditorDialog(property: PropertyEntity, state: DbState, viewModel: DatabaseViewModel, onDismiss: () -> Unit) {
    val colors = ImadTheme.colors
    var options by remember { mutableStateOf(state.config(property).options) }
    var draft by remember { mutableStateOf("") }
    DialogFrame(stringResource(R.string.db_edit_options), onDismiss, confirm = {
        TextButton(onClick = { viewModel.updateConfig(property, state.config(property).copy(options = options.filter { it.name.isNotBlank() })); onDismiss() }) { Text(stringResource(R.string.action_save)) }
    }) {
        options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(24.dp).clip(RoundedCornerShape(6.dp)).background(optionColor(option.color, colors))
                        .clickable(role = Role.Button) {
                            val next = InlineColor.entries[(InlineColor.entries.indexOfFirst { it.key == option.color } + 1) % InlineColor.entries.size].key
                            options = options.toMutableList().also { it[index] = option.copy(color = next) }
                        },
                )
                Spacer(Modifier.width(10.dp))
                BasicTextField(
                    option.name, { v -> options = options.toMutableList().also { it[index] = option.copy(name = v) } },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colors.text), cursorBrush = SolidColor(colors.primary), singleLine = true,
                    modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                )
                AppIconButton(Icons.Rounded.Close, stringResource(R.string.action_remove), { options = options.filterIndexed { i, _ -> i != index } })
            }
        }
        fun add() { if (draft.isNotBlank()) { options = options + SelectOption(newId(), draft.trim(), InlineColor.entries[options.size % InlineColor.entries.size].key); draft = "" } }
        AppTextField(draft, { draft = it }, stringResource(R.string.db_option_name_hint), Modifier.fillMaxWidth().padding(top = 6.dp), onDone = ::add,
            trailing = { AppIconButton(Icons.Rounded.Add, stringResource(R.string.action_add), ::add, enabled = draft.isNotBlank(), tint = colors.primary) })
    }
}

@Composable
fun FormulaDialog(property: PropertyEntity, state: DbState, viewModel: DatabaseViewModel, onDismiss: () -> Unit) {
    val colors = ImadTheme.colors
    var source by remember { mutableStateOf(state.config(property).formula.orEmpty()) }
    val sample = state.rows.firstOrNull()
    val preview = remember(source, sample) {
        Formula.evaluate(source, LocalDate.now()) { name -> state.properties.firstOrNull { it.name.equals(name, true) }?.let { p -> sample?.value(p.id) ?: Value.Empty } }
    }
    DialogFrame(stringResource(R.string.db_formula), onDismiss, confirm = {
        TextButton(onClick = { viewModel.updateConfig(property, state.config(property).copy(formula = source)); onDismiss() }) { Text(stringResource(R.string.action_save)) }
    }) {
        AppTextField(source, { source = it }, "prop(\"Price\") * 2", Modifier.fillMaxWidth(), singleLine = false, textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
        Spacer(Modifier.size(8.dp))
        when (preview) {
            is Formula.Result.Ok -> Text("= " + preview.value.asText(), style = MaterialTheme.typography.bodyMedium, color = colors.success)
            is Formula.Result.Error -> if (source.isNotBlank()) Text("⚠ " + preview.message, style = MaterialTheme.typography.bodyMedium, color = colors.danger)
        }
        Spacer(Modifier.size(10.dp))
        Text(stringResource(R.string.db_formula_help), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Spacer(Modifier.size(6.dp))
        Text(state.properties.filter { it.id != property.id }.joinToString("  ·  ") { "prop(\"${it.name}\")" }, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace), color = colors.textTertiary)
    }
}

/** Filters, sorts, grouping and visible properties of a view, edited in one place. */
@Composable
fun ViewSettingsDialog(mode: String, view: ViewEntity, state: DbState, viewModel: DatabaseViewModel, onDismiss: () -> Unit) {
    val colors = ImadTheme.colors
    val config = DbJson.view(view.config)
    fun update(c: ViewConfig) = viewModel.updateView(view, c)
    var picker by remember { mutableStateOf<String?>(null) }
    var draftFilter by remember { mutableStateOf<ViewFilter?>(null) }
    val props = state.properties
    fun name(id: String) = props.firstOrNull { it.id == id }?.name ?: "?"

    when (mode) {
        "filter" -> DialogFrame(stringResource(R.string.db_filter), onDismiss) {
            for (f in config.filters) {
                val p = props.firstOrNull { it.id == f.propertyId } ?: continue
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${p.name} ${stringResource(f.op.label())} ${filterValueLabel(p, f.value, state)}", style = MaterialTheme.typography.bodyLarge, color = colors.text, modifier = Modifier.weight(1f))
                    AppIconButton(Icons.Rounded.Close, stringResource(R.string.action_remove), { update(config.copy(filters = config.filters - f)) })
                }
            }
            TextButton(onClick = { picker = "filterProp" }) { Text("+ " + stringResource(R.string.db_add_filter)) }
        }
        "sort" -> DialogFrame(stringResource(R.string.db_sort), onDismiss) {
            for (s in config.sorts) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name(s.propertyId), style = MaterialTheme.typography.bodyLarge, color = colors.text, modifier = Modifier.weight(1f))
                    TextButton(onClick = { update(config.copy(sorts = config.sorts.map { if (it == s) it.copy(descending = !it.descending) else it })) }) {
                        Icon(Icons.Rounded.SwapVert, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp))
                        Text(stringResource(if (s.descending) R.string.db_sort_descending else R.string.db_sort_ascending))
                    }
                    AppIconButton(Icons.Rounded.Close, stringResource(R.string.action_remove), { update(config.copy(sorts = config.sorts - s)) })
                }
            }
            TextButton(onClick = { picker = "sortProp" }) { Text("+ " + stringResource(R.string.db_add_sort)) }
        }
        "group" -> {
            val calendar = view.type == app.imadnote.data.db.ViewType.CALENDAR
            val options = if (calendar) props.filter { it.type == PropertyType.DATE || it.type == PropertyType.CREATED_TIME || it.type == PropertyType.LAST_EDITED_TIME }
            else props.filter { Query.groupable(it.type) }
            ChoiceDialog(
                stringResource(if (calendar) R.string.db_date_property else R.string.db_group_by),
                (if (calendar) emptyList() else listOf<Pair<String?, String>>(null to stringResource(R.string.db_none))) + options.map { it.id to it.name },
                if (calendar) config.dateProperty else config.groupBy,
                { id -> update(if (calendar) config.copy(dateProperty = id) else config.copy(groupBy = id)) }, onDismiss,
            )
        }
        "properties" -> DialogFrame(stringResource(R.string.db_properties), onDismiss) {
            for (p in props.filter { it.type != PropertyType.TITLE }) {
                Row(Modifier.fillMaxWidth().clickable { update(config.copy(hidden = if (p.id in config.hidden) config.hidden - p.id else config.hidden + p.id)) }, verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(p.id !in config.hidden, null, colors = CheckboxDefaults.colors(checkedColor = colors.primary))
                    Icon(p.type.icon(), null, tint = colors.textTertiary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(p.name, style = MaterialTheme.typography.bodyLarge, color = colors.text)
                }
            }
        }
    }

    when (picker) {
        "sortProp" -> ChoiceDialog(stringResource(R.string.db_add_sort), props.filter { it.id !in config.sorts.map { s -> s.propertyId } }.map { it.id to it.name }, null,
            { id -> update(config.copy(sorts = config.sorts + ViewSort(id))) }, { picker = null })
        "filterProp" -> ChoiceDialog(stringResource(R.string.db_add_filter), props.map { it.id to it.name }, null,
            { id -> val p = props.first { it.id == id }; draftFilter = ViewFilter(id, Cells.opsFor(p.type).first()); picker = "filterOp" }, { if (picker == "filterProp") picker = null })
        "filterOp" -> draftFilter?.let { f ->
            val p = props.first { it.id == f.propertyId }
            ChoiceDialog(p.name, Cells.opsFor(p.type).map { it to stringResource(it.label()) }, f.op, { op ->
                val next = f.copy(op = op)
                if (op.needsValue) { draftFilter = next; picker = "filterValue" } else { update(config.copy(filters = config.filters + next)); draftFilter = null; picker = null }
            }, { if (picker == "filterOp") picker = null })
        }
        "filterValue" -> draftFilter?.let { f ->
            val p = props.first { it.id == f.propertyId }
            val done: (String) -> Unit = { v -> update(config.copy(filters = config.filters + f.copy(value = v))); draftFilter = null; picker = null }
            when (p.type) {
                PropertyType.SELECT, PropertyType.STATUS -> ChoiceDialog(p.name, state.config(p).options.map { it.id to it.name }, null, done) { picker = null }
                PropertyType.MULTI_SELECT -> ChoiceDialog(p.name, state.config(p).options.map { it.name to it.name }, null, done) { picker = null }
                PropertyType.DATE, PropertyType.CREATED_TIME, PropertyType.LAST_EDITED_TIME -> DatePickerModal(LocalDate.now(), { done(it.toString()) }, { picker = null })
                else -> TextInputDialog(p.name, stringResource(R.string.action_add), stringResource(R.string.action_cancel), placeholder = stringResource(R.string.db_value_hint), onConfirm = done, onDismiss = { picker = null })
            }
        }
    }
}

@Composable
private fun filterValueLabel(property: PropertyEntity, value: String, state: DbState): String = when (property.type) {
    PropertyType.SELECT, PropertyType.STATUS -> state.config(property).options.firstOrNull { it.id == value }?.name ?: value
    PropertyType.DATE -> runCatching { formatDay(LocalDate.parse(value)) }.getOrDefault(value)
    else -> value
}

/** Header menu for a property: rename, type, options, formula, relation target, sort, hide, delete. */
@Composable
fun PropertyMenu(property: PropertyEntity, view: ViewEntity, state: DbState, viewModel: DatabaseViewModel, onDismiss: () -> Unit) {
    val colors = ImadTheme.colors
    var sub by remember { mutableStateOf<String?>(null) }
    val config = DbJson.view(view.config)
    if (sub == null) {
        DialogFrame(property.name, onDismiss) {
            @Composable
            fun item(label: Int, destructive: Boolean = false, onClick: () -> Unit) = Text(
                stringResource(label), style = MaterialTheme.typography.bodyLarge, color = if (destructive) colors.danger else colors.text,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button, onClick = onClick).padding(vertical = 12.dp, horizontal = 4.dp),
            )
            item(R.string.db_rename_property) { sub = "rename" }
            if (property.type != PropertyType.TITLE) item(R.string.db_change_type) { sub = "type" }
            if (property.type in setOf(PropertyType.SELECT, PropertyType.STATUS, PropertyType.MULTI_SELECT)) item(R.string.db_edit_options) { sub = "options" }
            if (property.type == PropertyType.FORMULA) item(R.string.db_formula) { sub = "formula" }
            if (property.type == PropertyType.RELATION) item(R.string.db_relation_target) { sub = "target" }
            item(R.string.db_sort_ascending) { viewModel.updateView(view, config.copy(sorts = listOf(ViewSort(property.id)))); onDismiss() }
            item(R.string.db_sort_descending) { viewModel.updateView(view, config.copy(sorts = listOf(ViewSort(property.id, true)))); onDismiss() }
            if (property.type != PropertyType.TITLE) {
                item(R.string.db_hide) { viewModel.updateView(view, config.copy(hidden = config.hidden + property.id)); onDismiss() }
                item(R.string.db_delete_property, destructive = true) { sub = "delete" }
            }
        }
    }
    when (sub) {
        "rename" -> TextInputDialog(stringResource(R.string.db_rename_property), stringResource(R.string.action_save), stringResource(R.string.action_cancel), initial = property.name,
            onConfirm = { viewModel.renameProperty(property, it); onDismiss() }, onDismiss = onDismiss)
        "type" -> ChoiceDialog(stringResource(R.string.db_change_type), PropertyType.entries.filter { it != PropertyType.TITLE }.map { it to stringResource(it.label()) }, property.type,
            { viewModel.changeType(property, it) }, onDismiss)
        "options" -> OptionsEditorDialog(property, state, viewModel, onDismiss)
        "formula" -> FormulaDialog(property, state, viewModel, onDismiss)
        "target" -> RelationTargetDialog(state, property, viewModel, onDismiss)
        "delete" -> ConfirmDialog(stringResource(R.string.db_delete_property), stringResource(R.string.db_delete_property_message, property.name), stringResource(R.string.action_delete), stringResource(R.string.action_cancel),
            destructive = true, onConfirm = { viewModel.deleteProperty(property); onDismiss() }, onDismiss = onDismiss)
    }
}

@Composable
fun dateTimeOrEmpty(millis: Long?) = millis?.let { formatDateTime(it) }.orEmpty()
