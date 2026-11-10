package app.imadnote.ui.databases

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.Sell
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.ViewKanban
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.ArrowDropDownCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.data.db.PropertyEntity
import app.imadnote.data.db.PropertyType
import app.imadnote.data.db.ViewType
import app.imadnote.domain.db.DbJson
import app.imadnote.domain.db.FilterOp
import app.imadnote.domain.db.Row as DbRow
import app.imadnote.domain.db.Value
import app.imadnote.ui.components.Pill
import app.imadnote.ui.components.formatDay
import app.imadnote.ui.components.optionColor
import app.imadnote.ui.theme.ImadTheme

fun PropertyType.label(): Int = when (this) {
    PropertyType.TITLE -> R.string.prop_title
    PropertyType.TEXT -> R.string.prop_text
    PropertyType.NUMBER -> R.string.prop_number
    PropertyType.SELECT -> R.string.prop_select
    PropertyType.MULTI_SELECT -> R.string.prop_multi_select
    PropertyType.STATUS -> R.string.prop_status
    PropertyType.DATE -> R.string.prop_date
    PropertyType.CHECKBOX -> R.string.prop_checkbox
    PropertyType.URL -> R.string.prop_url
    PropertyType.EMAIL -> R.string.prop_email
    PropertyType.PHONE -> R.string.prop_phone
    PropertyType.PERSON -> R.string.prop_person
    PropertyType.RELATION -> R.string.prop_relation
    PropertyType.FORMULA -> R.string.prop_formula
    PropertyType.CREATED_TIME -> R.string.prop_created_time
    PropertyType.LAST_EDITED_TIME -> R.string.prop_last_edited_time
}

fun PropertyType.icon(): ImageVector = when (this) {
    PropertyType.TITLE -> Icons.Rounded.Title
    PropertyType.TEXT -> Icons.AutoMirrored.Rounded.Notes
    PropertyType.NUMBER -> Icons.Rounded.Numbers
    PropertyType.SELECT -> Icons.Rounded.ArrowDropDownCircle
    PropertyType.MULTI_SELECT -> Icons.Rounded.Sell
    PropertyType.STATUS -> Icons.Rounded.RadioButtonChecked
    PropertyType.DATE -> Icons.Rounded.Event
    PropertyType.CHECKBOX -> Icons.Rounded.CheckBox
    PropertyType.URL -> Icons.Rounded.Link
    PropertyType.EMAIL -> Icons.Rounded.AlternateEmail
    PropertyType.PHONE -> Icons.Rounded.Phone
    PropertyType.PERSON -> Icons.Rounded.Person
    PropertyType.RELATION -> Icons.Rounded.Share
    PropertyType.FORMULA -> Icons.Rounded.Functions
    PropertyType.CREATED_TIME -> Icons.Rounded.AccessTime
    PropertyType.LAST_EDITED_TIME -> Icons.Rounded.Update
}

fun ViewType.label(): Int = when (this) {
    ViewType.TABLE -> R.string.db_view_table
    ViewType.BOARD -> R.string.db_view_board
    ViewType.CALENDAR -> R.string.db_view_calendar
    ViewType.GALLERY -> R.string.db_view_gallery
    ViewType.LIST -> R.string.db_view_list
}

fun ViewType.icon(): ImageVector = when (this) {
    ViewType.TABLE -> Icons.Rounded.TableChart
    ViewType.BOARD -> Icons.Rounded.ViewKanban
    ViewType.CALENDAR -> Icons.Rounded.CalendarMonth
    ViewType.GALLERY -> Icons.Rounded.GridView
    ViewType.LIST -> Icons.AutoMirrored.Rounded.ViewList
}

fun FilterOp.label(): Int = when (this) {
    FilterOp.CONTAINS -> R.string.op_contains
    FilterOp.NOT_CONTAINS -> R.string.op_not_contains
    FilterOp.IS -> R.string.op_is
    FilterOp.IS_NOT -> R.string.op_is_not
    FilterOp.GREATER -> R.string.op_greater
    FilterOp.LESS -> R.string.op_less
    FilterOp.BEFORE -> R.string.op_before
    FilterOp.AFTER -> R.string.op_after
    FilterOp.EMPTY -> R.string.op_empty
    FilterOp.NOT_EMPTY -> R.string.op_not_empty
    FilterOp.CHECKED -> R.string.op_checked
    FilterOp.UNCHECKED -> R.string.op_unchecked
}

val FilterOp.needsValue get() = this !in setOf(FilterOp.EMPTY, FilterOp.NOT_EMPTY, FilterOp.CHECKED, FilterOp.UNCHECKED)

/** Types whose value is typed or picked by the user (not computed). */
val PropertyType.editable get() = this !in setOf(PropertyType.FORMULA, PropertyType.CREATED_TIME, PropertyType.LAST_EDITED_TIME)

/** Compact read-only rendering of a cell, used by every view. */
@Composable
fun CellValue(row: DbRow, property: PropertyEntity, modifier: Modifier = Modifier, maxPills: Int = 3) {
    val colors = ImadTheme.colors
    val value = row.value(property.id)
    val config = DbJson.property(property.config)
    val error = row.formulaErrors[property.id]
    when {
        error != null -> Text("⚠ $error", style = MaterialTheme.typography.bodySmall, color = colors.danger, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier)
        property.type == PropertyType.CHECKBOX -> Icon(
            if (value == Value.Bool(true)) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank, null,
            tint = if (value == Value.Bool(true)) colors.primary else colors.textTertiary, modifier = modifier.size(20.dp),
        )
        property.type == PropertyType.SELECT || property.type == PropertyType.STATUS -> {
            val option = config.options.firstOrNull { it.id == row.raw[property.id] }
            if (option != null) Pill(option.name, optionColor(option.color, colors), modifier)
        }
        property.type == PropertyType.MULTI_SELECT -> Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            val ids = DbJson.ids(row.raw[property.id].orEmpty())
            for (id in ids.take(maxPills)) config.options.firstOrNull { it.id == id }?.let { Pill(it.name, optionColor(it.color, colors)) }
            if (ids.size > maxPills) Text("+${ids.size - maxPills}", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
        }
        property.type == PropertyType.RELATION -> Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val names = (value as? Value.Many)?.v.orEmpty()
            for (name in names.take(maxPills)) Pill("↗ ${name.ifEmpty { "…" }}", colors.primary)
            if (names.size > maxPills) Text("+${names.size - maxPills}", style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
        }
        value is Value.Date -> Text(formatDay(value.v), style = MaterialTheme.typography.bodyMedium, color = colors.text, maxLines = 1, modifier = modifier)
        else -> Text(
            value.asText(), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = modifier,
            color = if (property.type == PropertyType.URL || property.type == PropertyType.EMAIL || property.type == PropertyType.PHONE) colors.primary else colors.text,
        )
    }
}

/** Opens a URL, email address or phone number in the matching app. */
fun openContact(context: android.content.Context, type: PropertyType, raw: String) {
    val uri = when (type) {
        PropertyType.EMAIL -> Uri.parse("mailto:$raw")
        PropertyType.PHONE -> Uri.parse("tel:$raw")
        else -> Uri.parse(if ("://" in raw) raw else "https://$raw")
    }
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
fun rowTitle(row: DbRow, state: DbState): String = state.title?.let { row.value(it.id).asText() }.orEmpty().ifEmpty { stringResource(R.string.untitled) }
