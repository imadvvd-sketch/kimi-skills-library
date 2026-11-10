package app.imadnote

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import app.imadnote.domain.editor.BlockType
import app.imadnote.ui.appViewModel
import app.imadnote.ui.shell.AppShell
import app.imadnote.ui.shell.Routes
import app.imadnote.ui.shell.ShellViewModel
import app.imadnote.ui.theme.ImadNoteTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        /** Notification taps pass the owner key, e.g. `task:<id>`. */
        const val EXTRA_OPEN = "open"
    }

    private var shell: ShellViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ImadNoteApp).container
        val personalName = getString(R.string.workspace_personal)
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val viewModel = appViewModel { ShellViewModel(it, personalName) }
                shell = viewModel
                LaunchedEffect(viewModel) { if (savedInstanceState == null) handle(intent) }
                val theme by viewModel.themeMode.collectAsStateWithLifecycle()
                // Nothing is drawn until the saved theme is known, so a dark-theme user never sees a light flash.
                theme?.let { mode -> ImadNoteTheme(mode) { AppShell(viewModel) } }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handle(intent)
    }

    /** Opens the item behind a notification, or saves text and images shared from other apps to the Inbox. */
    private fun handle(intent: Intent?) {
        val viewModel = shell ?: return
        intent ?: return
        intent.getStringExtra(EXTRA_OPEN)?.let { key ->
            intent.removeExtra(EXTRA_OPEN)
            Routes.fromReminderKey(key)?.let { viewModel.pendingRoute.value = it }
        }
        if (intent.action != Intent.ACTION_SEND) return
        intent.action = null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        val stream = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        lifecycleScope.launch {
            viewModel.workspace.first { it != null }
            when {
                stream != null && intent.type?.startsWith("image/") == true ->
                    viewModel.captureMedia(stream, BlockType.IMAGE, subject ?: getString(R.string.capture_image_title, ""), "🖼️")
                !text.isNullOrEmpty() -> {
                    val isLink = Regex("^https?://\\S+$").matches(text.substringBefore('\n').trim()) && '\n' !in text
                    if (isLink) viewModel.capture(ShellViewModel.CaptureKind.BOOKMARK, text)
                    else viewModel.capture(ShellViewModel.CaptureKind.NOTE, listOfNotNull(subject, text).joinToString("\n"))
                }
                else -> return@launch
            }
            Toast.makeText(this@MainActivity, R.string.capture_shared, Toast.LENGTH_SHORT).show()
        }
    }
}
