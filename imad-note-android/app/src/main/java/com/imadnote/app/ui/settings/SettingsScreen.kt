package com.imadnote.app.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.imadnote.app.BuildConfigInfo
import com.imadnote.app.R
import com.imadnote.app.data.HomeSection
import com.imadnote.app.data.ThemeMode
import com.imadnote.app.data.UserPreferences
import com.imadnote.app.reminders.Notifications
import com.imadnote.app.ui.LocalAppContainer
import com.imadnote.app.ui.components.InfoText
import com.imadnote.app.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit, onArchive: () -> Unit) {
    val container = LocalAppContainer.current
    val prefs by container.settings.preferences.collectAsStateWithLifecycle(UserPreferences())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Bumped on resume so permission states refresh after returning from system settings.
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refresh++
        scope.launch { container.reminderScheduler.rescheduleAll() }
    }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Header(stringResource(R.string.settings_appearance))
            ThemeMode.entries.forEach { mode ->
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.RadioButton) {
                        scope.launch { container.settings.setThemeMode(mode) }
                    }.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = prefs.themeMode == mode, onClick = null)
                    Spacer(Modifier.width(Spacing.md))
                    Text(
                        stringResource(
                            when (mode) {
                                ThemeMode.SYSTEM -> R.string.theme_system
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                            },
                        ),
                    )
                }
            }

            Divider()
            Header(stringResource(R.string.settings_home))
            HomeSection.entries.forEach { section ->
                val visible = section !in prefs.hiddenSections
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Switch) {
                        scope.launch { container.settings.setSectionVisible(section, !visible) }
                    }.padding(horizontal = Spacing.lg, vertical = Spacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(sectionLabel(section)), modifier = Modifier.weight(1f))
                    Switch(checked = visible, onCheckedChange = null)
                }
            }

            Divider()
            Header(stringResource(R.string.settings_reminders))
            key(refresh) {
                val notificationsOk = Notifications.canPost(context)
                StatusRow(
                    title = stringResource(R.string.notifications),
                    status = stringResource(if (notificationsOk) R.string.status_allowed else R.string.status_blocked),
                    actionLabel = if (notificationsOk) null else stringResource(R.string.action_allow),
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        }
                    },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val exactOk = container.reminderScheduler.canScheduleExact()
                    StatusRow(
                        title = stringResource(R.string.exact_alarms),
                        status = stringResource(if (exactOk) R.string.status_allowed else R.string.exact_alarms_off),
                        actionLabel = if (exactOk) null else stringResource(R.string.action_allow),
                        onAction = {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                            )
                        },
                    )
                }
            }
            InfoText(stringResource(R.string.reminders_info), Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm))

            Divider()
            Header(stringResource(R.string.settings_data))
            Row(
                Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onArchive).padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.archived_notes), modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
            }
            InfoText(stringResource(R.string.data_local_info), Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm))

            Divider()
            Header(stringResource(R.string.settings_about))
            InfoText(
                stringResource(R.string.about_version, BuildConfigInfo.versionName(context)),
                Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
            Spacer(Modifier.padding(Spacing.xl))
        }
    }
}

private fun sectionLabel(s: HomeSection): Int = when (s) {
    HomeSection.OVERDUE -> R.string.section_overdue
    HomeSection.TODAY -> R.string.section_today
    HomeSection.UPCOMING -> R.string.section_upcoming
    HomeSection.PINNED -> R.string.section_pinned
    HomeSection.RECENT -> R.string.section_recent_notes
    HomeSection.PROJECTS -> R.string.section_active_projects
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.lg, bottom = Spacing.sm),
    )
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(top = Spacing.sm), color = MaterialTheme.colorScheme.outlineVariant)

@Composable
private fun StatusRow(title: String, status: String, actionLabel: String?, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title)
            Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (actionLabel != null) {
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

