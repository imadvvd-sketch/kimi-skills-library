package app.ghars.feature.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.style.TextAlign
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.core.ui.ConfirmDialog
import app.ghars.core.ui.FitText
import app.ghars.core.ui.GharsButton
import app.ghars.core.ui.GharsButtonStyle
import app.ghars.core.ui.drawRing
import app.ghars.domain.session.SessionRules
import app.ghars.domain.session.SessionState
import app.ghars.domain.session.SessionStateMachine
import app.ghars.domain.session.formatCountdown
import app.ghars.feature.tree.SpeciesCatalog
import app.ghars.feature.tree.TreeView
import app.ghars.service.SessionNotifier
import kotlinx.coroutines.delay

/** S05: the running session. Dark, quiet, nothing to do but leave the phone alone. */
@Composable
fun SessionScreen(state: SessionState.Active, now: () -> Long, onGiveUp: () -> Unit) {
    val colors = GharsTheme.colors
    val spacing = GharsTheme.spacing
    val sizes = GharsTheme.sizes

    val time by produceState(now(), state) {
        while (true) {
            value = now()
            delay(250)
        }
    }
    val remaining = SessionStateMachine.remainingMs(state, time)
    val progress = SessionStateMachine.progress(state, time)
    val stage = SessionStateMachine.stage(progress)
    val speciesName = stringResource(SpeciesCatalog.get(state.session.speciesId).name)

    var confirmGiveUp by rememberSaveable { mutableStateOf(false) }
    // Back would leave the app and start the wilting countdown, so ask instead.
    BackHandler { confirmGiveUp = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.space4),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(spacing.space12))
        Box(
            modifier = Modifier
                .widthIn(max = sizes.ring)
                .fillMaxWidth()
                .aspectRatio(1f)
                .drawBehind {
                    drawRing(
                        track = colors.brandSoft, arc = colors.brand,
                        strokePx = sizes.ringStroke.toPx(), insetPx = sizes.ringStroke.toPx() / 2,
                        trackSweep = 360f, arcSweep = 360f * progress,
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            TreeView(
                speciesId = state.session.speciesId,
                stage = stage,
                modifier = Modifier.fillMaxSize().padding(sizes.ringStroke + spacing.space6),
                sway = true,
                description = stringResource(R.string.cd_tree_stage, speciesName, stage, SessionRules.STAGES),
            )
        }
        Spacer(Modifier.height(spacing.space12))

        // Screen readers hear the time once a minute, not every second.
        val minutesLeft = SessionNotifier.minutesLeft(remaining)
        val spoken = pluralStringResource(R.plurals.minutes_left, minutesLeft, minutesLeft)
        FitText(
            text = formatCountdown(remaining),
            widest = if (state.session.plannedMinutes >= 100) "888:88" else "88:88",
            style = GharsTheme.type.timer,
            color = colors.ink,
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = spoken
                    liveRegion = LiveRegionMode.Polite
                },
        )
        Spacer(Modifier.height(spacing.space2))
        Text(
            text = stringResource(
                if (state is SessionState.Paused) R.string.session_paused else R.string.session_phrase,
            ),
            style = GharsTheme.type.body,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.space8))
        GharsButton(
            text = stringResource(R.string.action_give_up),
            onClick = { confirmGiveUp = true },
            style = GharsButtonStyle.Danger,
        )
        Spacer(Modifier.height(spacing.space6))
    }

    if (confirmGiveUp) {
        ConfirmDialog(
            title = stringResource(R.string.give_up_title),
            body = stringResource(R.string.give_up_body),
            primaryLabel = stringResource(R.string.give_up_cancel),
            onPrimary = { confirmGiveUp = false },
            secondaryLabel = stringResource(R.string.give_up_confirm),
            onSecondary = {
                confirmGiveUp = false
                onGiveUp()
            },
            onDismiss = { confirmGiveUp = false },
            secondaryStyle = GharsButtonStyle.Danger,
        )
    }
}
