package app.imadnote

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.imadnote.ui.appViewModel
import app.imadnote.ui.shell.AppShell
import app.imadnote.ui.shell.ShellViewModel
import app.imadnote.ui.theme.ImadNoteTheme

class MainActivity : AppCompatActivity() {
    companion object {
        /** Notification taps pass the owner key, e.g. `task:<id>`. */
        const val EXTRA_OPEN = "open"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as ImadNoteApp).container
        val personalName = getString(R.string.workspace_personal)
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                val shell = appViewModel { ShellViewModel(it, personalName) }
                val theme by shell.themeMode.collectAsStateWithLifecycle()
                // Nothing is drawn until the saved theme is known, so a dark-theme user never sees a light flash.
                theme?.let { mode -> ImadNoteTheme(mode) { AppShell(shell) } }
            }
        }
    }
}
