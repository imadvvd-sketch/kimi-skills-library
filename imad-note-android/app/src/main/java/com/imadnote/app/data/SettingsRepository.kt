package com.imadnote.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class HomeSection { OVERDUE, TODAY, UPCOMING, PINNED, RECENT, PROJECTS }

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val hiddenSections: Set<HomeSection> = emptySet(),
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private val themeKey = stringPreferencesKey("theme_mode")
    private val hiddenKey = stringSetPreferencesKey("hidden_home_sections")

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[themeKey] } ?: ThemeMode.SYSTEM,
            hiddenSections = prefs[hiddenKey].orEmpty()
                .mapNotNull { name -> HomeSection.entries.firstOrNull { it.name == name } }
                .toSet(),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[themeKey] = mode.name }
    }

    suspend fun setSectionVisible(section: HomeSection, visible: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[hiddenKey].orEmpty()
            prefs[hiddenKey] = if (visible) current - section.name else current + section.name
        }
    }
}
