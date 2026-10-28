package com.imadnote.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class HomeSection { OVERDUE, TODAY, PINNED, ACTIVITY }

enum class TextSize(val scale: Float) { STANDARD(1f), LARGE(1.15f) }

enum class NotesLayout { GRID, LIST }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hiddenSections: Set<HomeSection> = emptySet(),
    val textSize: TextSize = TextSize.STANDARD,
    val notesLayout: NotesLayout = NotesLayout.LIST,
    /** Optional first name used in the Home greeting. */
    val displayName: String = "",
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val hiddenKey = stringSetPreferencesKey("hidden_home_sections")
    private val textSizeKey = stringPreferencesKey("text_size")
    private val notesLayoutKey = stringPreferencesKey("notes_layout")
    private val nameKey = stringPreferencesKey("display_name")

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM,
            hiddenSections = prefs[hiddenKey].orEmpty()
                .mapNotNull { name -> HomeSection.entries.firstOrNull { it.name == name } }
                .toSet(),
            textSize = TextSize.entries.firstOrNull { it.name == prefs[textSizeKey] } ?: TextSize.STANDARD,
            notesLayout = NotesLayout.entries.firstOrNull { it.name == prefs[notesLayoutKey] } ?: NotesLayout.LIST,
            displayName = prefs[nameKey].orEmpty(),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setTextSize(size: TextSize) {
        context.dataStore.edit { it[textSizeKey] = size.name }
    }

    suspend fun setNotesLayout(layout: NotesLayout) {
        context.dataStore.edit { it[notesLayoutKey] = layout.name }
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { it[nameKey] = name.trim().take(40) }
    }

    suspend fun setSectionVisible(section: HomeSection, visible: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[hiddenKey].orEmpty()
            prefs[hiddenKey] = if (visible) current - section.name else current + section.name
        }
    }
}
