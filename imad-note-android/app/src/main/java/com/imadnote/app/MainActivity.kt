package com.imadnote.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imadnote.app.data.UserPreferences
import com.imadnote.app.ui.LocalAppContainer
import com.imadnote.app.ui.navigation.ImadNoteRoot
import com.imadnote.app.ui.navigation.QUICK_CAPTURE
import com.imadnote.app.ui.navigation.Routes
import com.imadnote.app.ui.notes.NoteEditorViewModel
import com.imadnote.app.ui.theme.ImadNoteTheme

class MainActivity : ComponentActivity() {
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingRoute = routeFor(intent)
        val container = (application as ImadNoteApp).container
        setContent {
            val prefs by container.settings.preferences.collectAsStateWithLifecycle(UserPreferences())
            CompositionLocalProvider(LocalAppContainer provides container) {
                ImadNoteTheme(prefs.themeMode, prefs.textSize.scale) {
                    ImadNoteRoot(pendingRoute = pendingRoute, onPendingRouteHandled = { pendingRoute = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        routeFor(intent)?.let { pendingRoute = it }
    }

    /** Maps reminder notifications and launcher shortcuts to an in-app destination. */
    private fun routeFor(intent: Intent?): String? {
        intent ?: return null
        intent.getStringExtra(EXTRA_OPEN_TASK)?.let { return Routes.task(it) }
        return when (intent.getStringExtra(EXTRA_CAPTURE)) {
            "note" -> Routes.note()
            "task" -> Routes.task(today = true)
            "checklist" -> Routes.note(mode = NoteEditorViewModel.MODE_CHECKLIST)
            "quick" -> QUICK_CAPTURE
            else -> null
        }
    }

    companion object {
        const val EXTRA_OPEN_TASK = "open_task_id"
        const val EXTRA_CAPTURE = "capture"
    }
}
