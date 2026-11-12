package app.ghars.feature.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import app.ghars.R
import app.ghars.core.designsystem.GharsMotion
import app.ghars.core.designsystem.GharsTheme
import app.ghars.core.ui.GharsButton
import app.ghars.domain.session.SessionRules
import app.ghars.domain.session.SessionState
import app.ghars.domain.session.SessionStateMachine
import app.ghars.feature.tree.SpeciesCatalog
import app.ghars.feature.tree.TreeView
import app.ghars.feature.tree.rememberReduceMotion

/** S07: what became of the tree. No blame on failure, no fanfare on success. */
@Composable
fun ResultScreen(state: SessionState.Finished, onDone: () -> Unit) {
    val colors = GharsTheme.colors
    val spacing = GharsTheme.spacing
    val species = SpeciesCatalog.get(state.session.speciesId)
    val success = state is SessionState.Success
    val reduceMotion = rememberReduceMotion()

    BackHandler(onBack = onDone)

    // Show the tree as it was, then let it finish growing or wilt in front of the user.
    val lastStage = SessionStateMachine.stage(SessionStateMachine.progress(state, state.endedAt))
    var revealed by remember { mutableStateOf(false) }
    val glow = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        revealed = true
        if (success) {
            // The only glow in the app, and it happens once.
            if (reduceMotion) glow.snapTo(0.5f)
            else glow.animateTo(
                0.5f,
                keyframes {
                    durationMillis = GharsMotion.GLOW_MS
                    1f at GharsMotion.GLOW_MS / 3
                },
            )
        }
    }

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
        val glowColor = GharsTheme.shadows.glow.color
        Box(
            modifier = Modifier
                .widthIn(max = GharsTheme.sizes.ring)
                .fillMaxWidth()
                .aspectRatio(1f)
                .drawBehind {
                    if (glow.value > 0f) {
                        drawCircle(
                            Brush.radialGradient(listOf(glowColor, Color.Transparent)),
                            alpha = glow.value,
                        )
                    }
                },
        ) {
            TreeView(
                speciesId = species.id,
                stage = if (success && revealed) SessionRules.STAGES else lastStage.coerceAtMost(SessionRules.STAGES - 1),
                modifier = Modifier.fillMaxSize().padding(spacing.space6),
                withered = !success && revealed,
                description = if (success) stringResource(species.name) else stringResource(R.string.cd_tree_withered),
            )
        }
        Spacer(Modifier.height(spacing.space12))
        Text(
            text = stringResource(if (success) species.grownMessage else R.string.result_withered),
            style = GharsTheme.type.display,
            color = if (success) colors.ink else colors.withered,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        if (success) {
            Spacer(Modifier.height(spacing.space2))
            Text(
                text = pluralStringResource(
                    R.plurals.duration_minutes, state.session.plannedMinutes, state.session.plannedMinutes,
                ),
                style = GharsTheme.type.body,
                color = colors.inkMuted,
            )
        }
        Spacer(Modifier.height(spacing.space8))
        GharsButton(
            text = stringResource(R.string.action_done),
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.space6))
    }
}
