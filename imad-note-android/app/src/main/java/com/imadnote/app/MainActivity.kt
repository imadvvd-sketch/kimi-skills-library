package com.imadnote.app

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
import com.imadnote.app.ui.theme.ImadNoteTheme

class MainActivity : ComponentActivity() {
    private var pendingTaskId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) pendingTaskId = intent.getStringExtra(EXTRA_OPEN_TASK)
        val container = (application as ImadNoteApp).container
        setContent {
            val prefs by container.settings.preferences.collectAsStateWithLifecycle(UserPreferences())
            CompositionLocalProvider(LocalAppContainer provides container) {
                ImadNoteTheme(prefs.themeMode) {
                    ImadNoteRoot(pendingTaskId = pendingTaskId, onPendingTaskHandled = { pendingTaskId = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(EXTRA_OPEN_TASK)?.let { pendingTaskId = it }
    }

    companion object {
        const val EXTRA_OPEN_TASK = "open_task_id"
    }
}
