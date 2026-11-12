package app.ghars.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.ghars.domain.session.SessionState
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** @param bootCount which boot the monotonic times in [state] belong to. */
@Serializable
data class SessionSnapshot(val state: SessionState, val bootCount: Int)

/** Where the live session survives process death. */
interface ActiveSessionStore {
    suspend fun load(): SessionSnapshot?
    suspend fun save(snapshot: SessionSnapshot)
}

class DataStoreActiveSessionStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : ActiveSessionStore {

    override suspend fun load(): SessionSnapshot? =
        dataStore.data.first()[KEY]?.let { raw ->
            // An unreadable snapshot (older app version) is treated as no session.
            runCatching { json.decodeFromString<SessionSnapshot>(raw) }.getOrNull()
        }

    override suspend fun save(snapshot: SessionSnapshot) {
        dataStore.edit { it[KEY] = json.encodeToString(snapshot) }
    }

    private companion object {
        val KEY = stringPreferencesKey("active_session")
        val json = Json { ignoreUnknownKeys = true }
    }
}
