package app.ghars.di

import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import app.ghars.data.db.GharsDatabase
import app.ghars.data.prefs.ActiveSessionStore
import app.ghars.data.prefs.DataStoreActiveSessionStore
import app.ghars.data.repo.SessionRecorder
import app.ghars.data.repo.SessionRepository
import app.ghars.domain.session.Clock
import app.ghars.service.FocusService
import app.ghars.session.SessionController
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): GharsDatabase =
        Room.databaseBuilder(context, GharsDatabase::class.java, "ghars.db")
            .addCallback(GharsDatabase.Seed)
            .build()

    @Provides fun sessionDao(db: GharsDatabase) = db.sessionDao()
    @Provides fun speciesDao(db: GharsDatabase) = db.speciesDao()

    @Provides @Singleton
    fun dataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile("ghars") }

    @Provides fun activeSessionStore(impl: DataStoreActiveSessionStore): ActiveSessionStore = impl
    @Provides fun sessionRecorder(impl: SessionRepository): SessionRecorder = impl

    @Provides @Singleton
    fun clock(): Clock = object : Clock {
        override fun elapsedMs() = SystemClock.elapsedRealtime()
        override fun wallMs() = System.currentTimeMillis()
    }

    @Provides @Singleton
    fun sessionController(
        @ApplicationContext context: Context,
        clock: Clock,
        store: ActiveSessionStore,
        recorder: SessionRecorder,
    ) = SessionController(
        clock = clock,
        store = store,
        recorder = recorder,
        serviceStarter = { FocusService.start(context) },
        bootCounter = { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0) },
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
    )
}
