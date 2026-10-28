package com.imadnote.app.ui.capture

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.imadnote.app.R
import com.imadnote.app.ui.components.PrimaryButton
import com.imadnote.app.ui.components.SecondaryButton
import com.imadnote.app.ui.theme.Radius
import com.imadnote.app.ui.theme.Spacing

enum class CaptureType { NOTE, TASK, CHECKLIST, IMAGE, IDEA }

/**
 * Capture without choosing a folder or project first. The inline field saves straight away;
 * the tiles open the full editor for richer content.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSheet(
    onDismiss: () -> Unit,
    onPick: (CaptureType) -> Unit,
    onQuickSave: (text: String, asTask: Boolean) -> Unit,
) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet, shape = Radius.xl) {
        Column(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
        ) {
            Text(stringResource(R.string.quick_capture), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.size(Spacing.md))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(stringResource(R.string.capture_placeholder)) },
                shape = Radius.md,
                minLines = 2,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
            Spacer(Modifier.size(Spacing.md))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                SecondaryButton(
                    text = stringResource(R.string.save_as_task),
                    onClick = { onQuickSave(text.trim(), true) },
                    enabled = text.isNotBlank(),
                    icon = Icons.Outlined.AddTask,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(R.string.save_as_note),
                    onClick = { onQuickSave(text.trim(), false) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.size(Spacing.xl))
            Text(stringResource(R.string.capture_more), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.size(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Tile(Icons.Outlined.CheckBox, R.string.capture_checklist, Modifier.weight(1f)) { onPick(CaptureType.CHECKLIST) }
                Tile(Icons.Outlined.Image, R.string.capture_image, Modifier.weight(1f)) { onPick(CaptureType.IMAGE) }
                Tile(Icons.Outlined.Lightbulb, R.string.capture_idea, Modifier.weight(1f)) { onPick(CaptureType.IDEA) }
                Tile(Icons.Outlined.AddTask, R.string.capture_task_full, Modifier.weight(1f)) { onPick(CaptureType.TASK) }
            }
        }
    }
}

@Composable
private fun Tile(icon: ImageVector, label: Int, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = Radius.md,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.heightIn(min = 76.dp),
    ) {
        Column(
            Modifier.padding(vertical = Spacing.md, horizontal = Spacing.xs),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(Spacing.xs))
            Text(
                stringResource(label),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
