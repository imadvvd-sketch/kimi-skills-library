package app.imadnote.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.settingsDataStore by preferencesDataStore("settings")

/** Small persisted preferences. The app language is persisted by AppCompat's per-app locale support. */
class SettingsStore(private val store: DataStore<Preferences>) {
    constructor(context: Context) : this(context.applicationContext.settingsDataStore)

    val themeMode: Flow<ThemeMode> = store.data
        .map { prefs -> prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM }
        .distinctUntilChanged()

    val currentWorkspaceId: Flow<String?> = store.data.map { it[WORKSPACE] }.distinctUntilChanged()

    suspend fun setThemeMode(mode: ThemeMode) { store.edit { it[THEME] = mode.name } }

    suspend fun setCurrentWorkspace(id: String) { store.edit { it[WORKSPACE] = id } }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val WORKSPACE = stringPreferencesKey("current_workspace")
    }
}
