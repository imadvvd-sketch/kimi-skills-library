package app.imadnote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.imadnote.R
import app.imadnote.ui.theme.ImadTheme

/**
 * Common frame for top-level screens: a quiet top bar, centred content capped at a readable
 * width on tablets, and an optional floating action button.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onOpenMenu: (() -> Unit)? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    fabIcon: ImageVector? = null,
    fabLabel: String? = null,
    onFab: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colors = ImadTheme.colors
    Box(modifier.fillMaxSize().background(colors.background).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    onBack != null -> AppIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onBack, tint = colors.text)
                    onOpenMenu != null -> AppIconButton(Icons.Rounded.Menu, stringResource(R.string.nav_open_menu), onOpenMenu, tint = colors.text)
                    else -> Spacer(Modifier.width(16.dp))
                }
                Text(
                    title, style = MaterialTheme.typography.headlineSmall, color = colors.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp).semantics { heading() },
                )
                actions()
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 760.dp).fillMaxSize()) { content() }
            }
        }
        if (fabIcon != null && onFab != null) {
            FloatingActionButton(
                onClick = onFab,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
                containerColor = colors.primary, contentColor = colors.onPrimary, shape = MaterialTheme.shapes.large,
            ) { Icon(fabIcon, fabLabel) }
        }
    }
}
