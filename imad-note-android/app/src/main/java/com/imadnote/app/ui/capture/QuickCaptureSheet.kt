package com.imadnote.app.ui.capture

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.imadnote.app.R
import com.imadnote.app.ui.theme.Spacing

enum class CaptureType { NOTE, TASK, CHECKLIST, IMAGE, IDEA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickCaptureSheet(onDismiss: () -> Unit, onPick: (CaptureType) -> Unit) {
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheet) {
        Column(Modifier.navigationBarsPadding().padding(bottom = Spacing.lg)) {
            Text(
                stringResource(R.string.quick_capture),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = Spacing.xl, vertical = Spacing.sm),
            )
            Option(Icons.AutoMirrored.Outlined.Notes, R.string.capture_note, R.string.capture_note_desc) { onPick(CaptureType.NOTE) }
            Option(Icons.Outlined.AddTask, R.string.capture_task, R.string.capture_task_desc) { onPick(CaptureType.TASK) }
            Option(Icons.Outlined.CheckBox, R.string.capture_checklist, R.string.capture_checklist_desc) { onPick(CaptureType.CHECKLIST) }
            Option(Icons.Outlined.Image, R.string.capture_image, R.string.capture_image_desc) { onPick(CaptureType.IMAGE) }
            Option(Icons.Outlined.Lightbulb, R.string.capture_idea, R.string.capture_idea_desc) { onPick(CaptureType.IDEA) }
        }
    }
}

@Composable
private fun Option(icon: ImageVector, title: Int, desc: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = Spacing.xl, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.padding(10.dp).size(22.dp))
        }
        Spacer(Modifier.width(Spacing.lg))
        Column {
            Text(stringResource(title), style = MaterialTheme.typography.bodyLarge)
            Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
