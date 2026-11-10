package app.imadnote.ui.inbox

import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.domain.editor.BlockType
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.AppTextField
import app.imadnote.ui.components.ChipRow
import app.imadnote.ui.components.EmptyState
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.SkeletonList
import app.imadnote.ui.home.PageCard
import app.imadnote.ui.shell.PageActions
import app.imadnote.ui.shell.PageTree
import app.imadnote.ui.shell.ShellViewModel
import app.imadnote.ui.shell.ShellViewModel.CaptureKind
import app.imadnote.ui.theme.ImadTheme
import kotlinx.coroutines.delay
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private enum class CaptureType { NOTE, TASK, CHECKLIST, IDEA, BOOKMARK, IMAGE, VOICE }

/** Capture in one step: pick a type (Note is the default), type or pick, send. */
@Composable
fun InboxScreen(tree: PageTree?, actions: PageActions, onOpenMenu: (() -> Unit)?, shell: ShellViewModel) {
    val context = LocalContext.current
    var draft by rememberSaveable { mutableStateOf("") }
    var type by rememberSaveable { mutableStateOf(CaptureType.NOTE) }
    var recording by remember { mutableStateOf(false) }
    val stamp = LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT))
    val saved = stringResource(R.string.capture_saved)
    val taskAdded = stringResource(R.string.tasks_added)
    val checklistLabel = stringResource(R.string.capture_checklist) + " · " + stamp
    val imageTitle = stringResource(R.string.capture_image_title, stamp)
    val voiceTitle = stringResource(R.string.voice_title, stamp)
    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) shell.captureMedia(uri, BlockType.IMAGE, imageTitle, "🖼️") { toast(saved) }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recording = true else toast(context.getString(R.string.voice_permission))
    }

    fun submit() {
        if (draft.isBlank()) return
        val kind = when (type) {
            CaptureType.TASK -> CaptureKind.TASK
            CaptureType.CHECKLIST -> CaptureKind.CHECKLIST
            CaptureType.IDEA -> CaptureKind.IDEA
            CaptureType.BOOKMARK -> CaptureKind.BOOKMARK
            else -> CaptureKind.NOTE
        }
        shell.capture(kind, draft, checklistLabel)
        toast(if (kind == CaptureKind.TASK) taskAdded else saved)
        draft = ""
    }

    ScreenScaffold(title = stringResource(R.string.nav_inbox), onOpenMenu = onOpenMenu) {
        if (tree == null) {
            SkeletonList()
            return@ScreenScaffold
        }
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item(key = "types") {
                ChipRow(
                    listOf(
                        CaptureType.NOTE to stringResource(R.string.capture_note), CaptureType.TASK to stringResource(R.string.capture_task),
                        CaptureType.CHECKLIST to stringResource(R.string.capture_checklist), CaptureType.IDEA to stringResource(R.string.capture_idea),
                        CaptureType.BOOKMARK to stringResource(R.string.capture_bookmark), CaptureType.IMAGE to stringResource(R.string.capture_image),
                        CaptureType.VOICE to stringResource(R.string.capture_voice),
                    ),
                    type,
                    { picked ->
                        when (picked) {
                            CaptureType.IMAGE -> imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            CaptureType.VOICE -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            else -> type = picked
                        }
                    },
                )
            }
            item(key = "capture") {
                AppTextField(
                    draft, { draft = it },
                    stringResource(
                        when (type) {
                            CaptureType.TASK -> R.string.capture_task_hint
                            CaptureType.CHECKLIST -> R.string.capture_checklist_hint
                            CaptureType.IDEA -> R.string.capture_idea_hint
                            CaptureType.BOOKMARK -> R.string.capture_bookmark_hint
                            else -> R.string.inbox_hint
                        }
                    ),
                    Modifier.fillMaxWidth().padding(bottom = 8.dp).testTag("capture-field"),
                    singleLine = type == CaptureType.TASK || type == CaptureType.BOOKMARK,
                    imeAction = if (type == CaptureType.TASK || type == CaptureType.BOOKMARK) ImeAction.Done else ImeAction.Default,
                    onDone = ::submit,
                    trailing = {
                        AppIconButton(Icons.AutoMirrored.Rounded.Send, stringResource(R.string.inbox_add), ::submit, enabled = draft.isNotBlank(), tint = ImadTheme.colors.primary)
                    },
                )
            }
            if (tree.inbox.isEmpty()) {
                item { EmptyState(Icons.Rounded.Inbox, stringResource(R.string.inbox_empty_title), stringResource(R.string.inbox_empty_message)) }
            }
            items(tree.inbox, key = { it.id }) { page ->
                PageCard(
                    page,
                    trailing = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppIconButton(Icons.AutoMirrored.Rounded.DriveFileMove, stringResource(R.string.inbox_file), { actions.onEdit(page.id) { it.copy(isInbox = false) } })
                            AppIconButton(Icons.Rounded.DeleteOutline, stringResource(R.string.page_move_to_trash), { actions.onTrash(page.id) })
                        }
                    },
                    onClick = { actions.onOpen(page) },
                )
            }
        }
    }
    if (recording) {
        VoiceRecorderDialog(
            onSaved = { file -> recording = false; shell.captureMedia(Uri.fromFile(file), BlockType.AUDIO, voiceTitle, "🎙️") { file.delete(); toast(saved) } },
            onDismiss = { recording = false },
        )
    }
}

/** Records AAC audio into the cache; the file is copied into attachments when saved. */
@Composable
private fun VoiceRecorderDialog(onSaved: (File) -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = ImadTheme.colors
    val file = remember { File(context.cacheDir, "voice-${System.currentTimeMillis()}.m4a") }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var failed by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val r = (if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder())
        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            recorder = r
        } catch (_: Exception) {
            failed = true
            r.release()
        }
        onDispose { recorder?.let { runCatching { it.stop() }; it.release() }; recorder = null }
    }
    LaunchedEffect(recorder) { while (recorder != null) { delay(1000); seconds++ } }

    fun stopAndSave() {
        val r = recorder ?: return
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        recorder = null
        if (ok && file.length() > 0) onSaved(file) else { file.delete(); onDismiss() }
    }

    AlertDialog(
        onDismissRequest = {},
        title = { Text(stringResource(if (failed) R.string.voice_failed else R.string.voice_recording), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(if (failed) colors.surfaceAlt else colors.danger.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Mic, null, tint = if (failed) colors.textTertiary else colors.danger, modifier = Modifier.size(36.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("%d:%02d".format(seconds / 60, seconds % 60), style = MaterialTheme.typography.headlineSmall, color = colors.text)
            }
        },
        confirmButton = {
            if (!failed) FilledIconButton(onClick = ::stopAndSave, colors = IconButtonDefaults.filledIconButtonColors(containerColor = colors.danger)) {
                Icon(Icons.Rounded.Stop, stringResource(R.string.voice_stop), tint = colors.surface)
            }
        },
        dismissButton = {
            TextButton(onClick = { recorder?.let { runCatching { it.stop() }; it.release() }; recorder = null; file.delete(); onDismiss() }) {
                Text(stringResource(R.string.action_cancel), color = colors.textSecondary)
            }
        },
        containerColor = colors.surface,
        shape = MaterialTheme.shapes.large,
    )
}
