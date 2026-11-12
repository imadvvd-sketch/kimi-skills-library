package app.ghars.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.ghars.data.prefs.SettingsStore
import app.ghars.domain.session.SessionRules
import app.ghars.session.SessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val minutes: Int = SessionRules.DEFAULT_MINUTES,
    val speciesId: String = SettingsStore.DEFAULT_SPECIES,
    val askedForNotifications: Boolean = false,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settings: SettingsStore,
    private val controller: SessionController,
) : ViewModel() {

    // Held in memory so the slider follows the finger; saved in the background.
    private val minutes = MutableStateFlow<Int?>(null)

    val ui: StateFlow<HomeUiState> =
        combine(minutes, settings.minutes, settings.speciesId, settings.askedForNotifications) { local, saved, species, asked ->
            HomeUiState(local ?: saved, species, asked)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun setMinutes(value: Int) {
        val snapped = SessionRules.snapDuration(value)
        minutes.value = snapped
        viewModelScope.launch { settings.setMinutes(snapped) }
    }

    fun stepMinutes(direction: Int) = setMinutes(ui.value.minutes + direction * SessionRules.STEP_MINUTES)

    fun markAskedForNotifications() {
        viewModelScope.launch { settings.setAskedForNotifications() }
    }

    fun plant() {
        viewModelScope.launch {
            controller.start(settings.speciesId.first(), minutes.value ?: settings.minutes.first())
        }
    }
}
