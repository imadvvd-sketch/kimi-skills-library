package app.ghars.feature.challenges

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.stateDescription
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.core.ui.CoinBadge
import app.ghars.core.ui.GharsButton
import app.ghars.data.repo.EconomyRepository
import app.ghars.domain.economy.ChallengeState
import app.ghars.domain.economy.ChallengeStatus
import app.ghars.domain.economy.DailyChallenge
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ChallengesViewModel @Inject constructor(private val economy: EconomyRepository) : ViewModel() {
    // Started fresh each time the screen is opened, so "today" is right after midnight.
    val challenges = economy.observeChallenges()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    fun claim(challenge: DailyChallenge) {
        viewModelScope.launch { economy.claim(challenge) }
    }
}

private fun DailyChallenge.titleRes(): Int = when (this) {
    DailyChallenge.FirstSession -> R.string.challenge_first_session
    DailyChallenge.HourOfFocus -> R.string.challenge_focus_60
    DailyChallenge.ThreeSessions -> R.string.challenge_three_sessions
}

/** S13: small daily goals that pay coins. */
@Composable
fun ChallengesScreen(viewModel: ChallengesViewModel = hiltViewModel()) {
    val challenges by viewModel.challenges.collectAsStateWithLifecycle()
    val spacing = GharsTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space3),
    ) {
        Text(
            text = stringResource(R.string.challenges_intro),
            style = GharsTheme.type.body,
            color = GharsTheme.colors.inkMuted,
        )
        challenges.forEach { status ->
            ChallengeCard(status = status, onClaim = { viewModel.claim(status.challenge) })
        }
    }
}

@Composable
private fun ChallengeCard(status: ChallengeStatus, onClaim: () -> Unit) {
    val colors = GharsTheme.colors
    val spacing = GharsTheme.spacing
    val challenge = status.challenge
    val progressText = stringResource(R.string.challenge_progress, status.progress, challenge.goal)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(GharsTheme.radius.mdShape)
            .background(colors.surface)
            .padding(spacing.space4),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(challenge.titleRes()),
                style = GharsTheme.type.heading,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.padding(start = spacing.space2))
            CoinBadge(
                text = "+${challenge.reward}",
                description = pluralStringResource(R.plurals.coins, challenge.reward, challenge.reward),
            )
        }
        Spacer(Modifier.height(spacing.space3))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(spacing.space2)
                .clip(GharsTheme.radius.pillShape)
                .background(colors.surfaceSunk)
                .clearAndSetSemantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(status.progress.toFloat(), 0f..challenge.goal.toFloat())
                    stateDescription = progressText
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(status.progress.toFloat() / challenge.goal)
                    .clip(GharsTheme.radius.pillShape)
                    .background(colors.brand),
            )
        }
        Spacer(Modifier.height(spacing.space2))
        when (status.state) {
            ChallengeState.InProgress ->
                Text(text = progressText, style = GharsTheme.type.caption, color = colors.inkMuted)
            ChallengeState.Complete ->
                GharsButton(text = stringResource(R.string.challenge_claim), onClick = onClaim, modifier = Modifier.fillMaxWidth())
            ChallengeState.Claimed ->
                Text(text = stringResource(R.string.challenge_claimed), style = GharsTheme.type.label, color = colors.inkMuted)
        }
    }
}
