package com.imadnote.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.imadnote.app.R
import com.imadnote.app.ui.theme.Radius
import com.imadnote.app.ui.theme.Spacing

/** Empty state: illustration, one-line title, one helpful sentence, optional action. */
@Composable
fun EmptyState(
    illustration: Illustration,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = if (compact) Spacing.lg else Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IllustrationArt(illustration, size = if (compact) 88.dp else 120.dp)
        Spacer(Modifier.height(Spacing.lg))
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        if (body.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xs))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 320.dp),
            )
        }
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            SecondaryButton(actionLabel, onClick = onAction)
        }
    }
}

/** Error state with a retry action. Data is local, so errors are rare (e.g. storage full). */
@Composable
fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        illustration = Illustration.ERROR,
        title = stringResource(R.string.error_title),
        body = stringResource(R.string.error_body),
        actionLabel = stringResource(R.string.action_retry),
        onAction = onRetry,
        modifier = modifier,
    )
}

/** Static skeleton placeholder shaped like the content that is loading (no looping animation). */
@Composable
fun LoadingState(modifier: Modifier = Modifier, rows: Int = 6) {
    val label = stringResource(R.string.loading)
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.lg, vertical = Spacing.lg)
            .semantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        repeat(rows) { i ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBlock(24.dp, 24.dp, round = true)
                Spacer(Modifier.width(Spacing.md))
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SkeletonBlock(if (i % 2 == 0) 220.dp else 160.dp, 14.dp)
                    SkeletonBlock(96.dp, 10.dp)
                }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(width: Dp, height: Dp, round: Boolean = false) {
    Box(
        Modifier
            .size(width, height)
            .clip(if (round) Radius.full else Radius.xs)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    )
}
