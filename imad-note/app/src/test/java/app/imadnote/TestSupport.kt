package app.imadnote

import android.content.Context
import android.os.Looper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import app.imadnote.core.Clock
import app.imadnote.data.SettingsStore
import app.imadnote.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.robolectric.Shadows.shadowOf
import java.io.File
import java.util.UUID

class FakeClock(var time: Long = 1_700_000_000_000) : Clock {
    override fun now(): Long = time++
}

fun testContext(): Context = ApplicationProvider.getApplicationContext()

fun testSettings(): SettingsStore {
    val file = File(testContext().cacheDir, "settings-${UUID.randomUUID()}.preferences_pb")
    return SettingsStore(PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + SupervisorJob())) { file })
}

fun testContainer(db: AppDatabase = AppDatabase.inMemory(testContext()), clock: Clock = FakeClock()) =
    AppContainer(testContext(), db = db, settings = testSettings(), clock = clock)

/** Runs main-thread work and waits for background database work until [condition] holds. */
fun awaitUntil(timeoutMs: Long = 5_000, condition: () -> Boolean) {
    val deadline = System.currentTimeMillis() + timeoutMs
    while (true) {
        // idleFor also advances Robolectric's clock, so delayed work such as the autosave debounce runs.
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(50))
        if (condition()) return
        check(System.currentTimeMillis() < deadline) { "Condition not met within ${timeoutMs}ms" }
        Thread.sleep(5)
    }
}
