package com.imadnote.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.imadnote.app.ui.theme.AppTheme
import com.imadnote.app.ui.theme.Motion
import com.imadnote.app.ui.theme.Radius
import com.imadnote.app.ui.theme.Spacing

/** Pill segmented control (Today · Week · All). The selected segment is filled with primary. */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(Radius.full)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            val bg by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, tween(Motion.FAST), label = "seg")
            val fg by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, tween(Motion.FAST), label = "segText")
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp)
                    .clip(Radius.full)
                    .background(bg)
                    .selectable(selected = isSelected, role = Role.Tab, onClick = { onSelect(index) }),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = Spacing.sm))
            }
        }
    }
}

enum class Status { SUCCESS, WARNING, INFO, DANGER }

/** Small status label ("Active", "Late", "Done"). Text colour carries the meaning, never colour alone. */
@Composable
fun StatusChip(text: String, status: Status, modifier: Modifier = Modifier) {
    val ext = AppTheme.extended
    val scheme = MaterialTheme.colorScheme
    val (fg, bg) = when (status) {
        Status.SUCCESS -> ext.success to scheme.tertiaryContainer
        Status.WARNING -> ext.warning to ext.accent.copy(alpha = 0.18f)
        Status.INFO -> scheme.secondary to scheme.secondaryContainer
        Status.DANGER -> scheme.error to scheme.errorContainer
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = fg,
        modifier = modifier.clip(Radius.full).background(bg).padding(horizontal = 10.dp, vertical = 3.dp),
    )
}

/** Rounded-square task checkbox (22 dp visual inside the caller's 48 dp target). */
@Composable
fun SquareCheck(checked: Boolean, tint: Color, modifier: Modifier = Modifier) {
    val success = AppTheme.extended.success
    val fill by animateColorAsState(if (checked) success else Color.Transparent, tween(Motion.FAST), label = "checkFill")
    val border by animateColorAsState(if (checked) success else tint, tween(Motion.FAST), label = "checkBorder")
    Box(
        modifier
            .size(22.dp)
            .clip(Radius.sm)
            .background(fill)
            .border(2.dp, border, Radius.sm),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}
