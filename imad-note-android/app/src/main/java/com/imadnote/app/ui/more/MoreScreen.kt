package com.imadnote.app.ui.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.imadnote.app.R
import com.imadnote.app.ui.components.ImadCard
import com.imadnote.app.ui.components.contentWidth
import com.imadnote.app.ui.theme.AppTheme
import com.imadnote.app.ui.theme.Radius
import com.imadnote.app.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onProjects: () -> Unit,
    onCalendar: () -> Unit,
    onArchive: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_more)) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).contentWidth().padding(horizontal = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            BrandCard()
            ImadCard {
                Column {
                    val ext = AppTheme.extended
                    MoreRow(Icons.Outlined.Folder, R.string.tab_projects, R.string.more_projects_desc, MaterialTheme.colorScheme.secondary, onProjects)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    MoreRow(Icons.Outlined.CalendarMonth, R.string.calendar, R.string.more_calendar_desc, ext.success, onCalendar)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    MoreRow(Icons.Outlined.Search, R.string.action_search, R.string.more_search_desc, ext.warning, onSearch)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    MoreRow(Icons.Outlined.Archive, R.string.archive, R.string.more_archive_desc, MaterialTheme.colorScheme.onSurfaceVariant, onArchive)
                }
            }
            ImadCard {
                MoreRow(Icons.Outlined.Settings, R.string.settings, R.string.more_settings_desc, MaterialTheme.colorScheme.primary, onSettings)
            }
            Spacer(Modifier.size(Spacing.lg))
        }
    }
}

@Composable
private fun BrandCard() {
    Surface(shape = Radius.lg, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(Spacing.lg), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.drawable.ic_brand_mark),
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(Radius.md),
            )
            Spacer(Modifier.width(Spacing.lg))
            Column {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(stringResource(R.string.tagline), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun MoreRow(icon: ImageVector, title: Int, desc: Int, tint: Color, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = Spacing.lg, vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(Radius.md).padding(0.dp), contentAlignment = Alignment.Center) {
                Surface(shape = Radius.md, color = tint.copy(alpha = 0.12f), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = tint) }
                }
            }
            Spacer(Modifier.width(Spacing.lg))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
