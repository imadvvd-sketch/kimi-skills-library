package app.ghars.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.ghars.domain.session.SessionRules
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The user's last choices on the home screen. */
@Singleton
class SettingsStore @Inject constructor(private val dataStore: DataStore<Preferences>) {

    val minutes: Flow<Int> = dataStore.data.map { it[MINUTES] ?: SessionRules.DEFAULT_MINUTES }
    val speciesId: Flow<String> = dataStore.data.map { it[SPECIES] ?: DEFAULT_SPECIES }
    val askedForNotifications: Flow<Boolean> = dataStore.data.map { it[ASKED_NOTIFICATIONS] ?: false }

    suspend fun setMinutes(value: Int) {
        dataStore.edit { it[MINUTES] = SessionRules.snapDuration(value) }
    }

    suspend fun setSpeciesId(value: String) {
        dataStore.edit { it[SPECIES] = value }
    }

    suspend fun setAskedForNotifications() {
        dataStore.edit { it[ASKED_NOTIFICATIONS] = true }
    }

    companion object {
        const val DEFAULT_SPECIES = "olive"
        private val MINUTES = intPreferencesKey("minutes")
        private val SPECIES = stringPreferencesKey("species")
        private val ASKED_NOTIFICATIONS = booleanPreferencesKey("asked_notifications")
    }
}
