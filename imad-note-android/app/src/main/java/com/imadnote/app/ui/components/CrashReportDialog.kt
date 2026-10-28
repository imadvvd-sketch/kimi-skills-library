package com.imadnote.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.imadnote.app.R
import com.imadnote.app.ui.theme.Radius

/** Shown on the launch after a crash, so the user can copy the report and send it. */
@Composable
fun CrashReportDialog(report: String, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = Radius.lg,
        title = { Text(stringResource(R.string.crash_title)) },
        text = {
            Column {
                Text(stringResource(R.string.crash_body), style = MaterialTheme.typography.bodyMedium)
                SelectionContainer {
                    Text(
                        report,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                clipboard.setText(AnnotatedString(report))
                onDismiss()
            }) { Text(stringResource(R.string.crash_copy)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
    )
}
