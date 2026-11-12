package app.ghars.feature.session

import androidx.lifecycle.ViewModel
import app.ghars.domain.session.Clock
import app.ghars.domain.session.SessionState
import app.ghars.session.SessionController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val controller: SessionController,
    private val clock: Clock,
) : ViewModel() {
    /** Null while the saved session is being read back at launch. */
    val state: StateFlow<SessionState?> = controller.state

    fun now(): Long = clock.elapsedMs()
    fun giveUp() { controller.giveUp() }
    fun acknowledgeResult() { controller.acknowledgeResult() }
}
