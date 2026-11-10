package app.imadnote.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.imadnote.BuildConfig
import app.imadnote.R
import app.imadnote.data.ThemeMode
import app.imadnote.data.db.WorkspaceEntity
import app.imadnote.ui.components.AppCard
import app.imadnote.ui.components.AppIconButton
import app.imadnote.ui.components.ConfirmDialog
import app.imadnote.ui.components.EmojiTile
import app.imadnote.ui.components.ListRow
import app.imadnote.ui.components.ScreenScaffold
import app.imadnote.ui.components.SectionHeader
import app.imadnote.ui.components.TextInputDialog
import app.imadnote.ui.editor.EmojiPickerDialog
import app.imadnote.ui.shell.ShellViewModel
import app.imadnote.ui.theme.ImadTheme

@Composable
fun SettingsScreen(viewModel: ShellViewModel, onOpenMenu: (() -> Unit)?, onOpenTrash: () -> Unit) {
    val colors = ImadTheme.colors
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val current by viewModel.workspace.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<WorkspaceEntity?>(null) }
    var deleting by remember { mutableStateOf<WorkspaceEntity?>(null) }
    var pickingIcon by remember { mutableStateOf<WorkspaceEntity?>(null) }

    ScreenScaffold(title = stringResource(R.string.nav_settings), onOpenMenu = onOpenMenu) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(stringResource(R.string.settings_appearance))
            AppCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.titleSmall, color = colors.text)
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf(ThemeMode.SYSTEM to R.string.settings_theme_system, ThemeMode.LIGHT to R.string.settings_theme_light, ThemeMode.DARK to R.string.settings_theme_dark)
                        .map { it.first to stringResource(it.second) },
                    selected = theme ?: ThemeMode.SYSTEM,
                    onSelect = viewModel::setThemeMode,
                )
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleSmall, color = colors.text)
                Spacer(Modifier.height(10.dp))
                val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags().substringBefore('-').substringBefore(',')
                Segmented(
                    // Language names are shown in their own language so they stay recognisable.
                    options = listOf("" to stringResource(R.string.settings_language_system), "en" to "English", "ar" to "العربية"),
                    selected = tag,
                    onSelect = { choice ->
                        AppCompatDelegate.setApplicationLocales(if (choice.isEmpty()) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(choice))
                    },
                )
            }

            Spacer(Modifier.height(8.dp))
            SectionHeader(stringResource(R.string.workspace_section)) {
                AppIconButton(Icons.Rounded.Add, stringResource(R.string.workspace_new), { creating = true })
            }
            AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 4.dp)) {
                workspaces.forEachIndexed { index, workspace ->
                    if (index > 0) HorizontalDivider(color = colors.border, modifier = Modifier.padding(horizontal = 16.dp))
                    ListRow(
                        title = workspace.name,
                        leading = {
                            Box(Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = stringResource(R.string.page_choose_icon)) { pickingIcon = workspace }) {
                                EmojiTile(workspace.icon, background = if (workspace.id == current?.id) colors.primarySoft else colors.surfaceAlt)
                            }
                        },
                        trailing = {
                            Row {
                                AppIconButton(Icons.Rounded.Edit, stringResource(R.string.workspace_rename), { renaming = workspace })
                                AppIconButton(Icons.Rounded.DeleteOutline, stringResource(R.string.workspace_delete), { deleting = workspace }, enabled = workspaces.size > 1)
                            }
                        },
                        onClick = { viewModel.selectWorkspace(workspace.id) },
                    )
                }
            }
            if (workspaces.size <= 1) {
                Text(stringResource(R.string.workspace_delete_last), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary, modifier = Modifier.padding(horizontal = 4.dp))
            }

            Spacer(Modifier.height(8.dp))
            SectionHeader(stringResource(R.string.settings_data))
            AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 4.dp)) {
                ListRow(
                    title = stringResource(R.string.nav_trash),
                    leading = { Icon(Icons.Rounded.DeleteOutline, null, tint = colors.textSecondary) },
                    trailing = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.textTertiary) },
                    onClick = onOpenTrash,
                )
            }
            Text(stringResource(R.string.settings_storage_note), style = MaterialTheme.typography.bodySmall, color = colors.textTertiary, modifier = Modifier.padding(horizontal = 4.dp))

            Spacer(Modifier.height(8.dp))
            SectionHeader(stringResource(R.string.settings_about))
            AppCard(Modifier.fillMaxWidth(), padding = PaddingValues(vertical = 4.dp)) {
                ListRow(title = stringResource(R.string.app_name), subtitle = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME))
            }
        }
    }

    if (creating) {
        TextInputDialog(
            title = stringResource(R.string.workspace_new), confirmLabel = stringResource(R.string.action_create), dismissLabel = stringResource(R.string.action_cancel),
            placeholder = stringResource(R.string.workspace_name_hint),
            onConfirm = { creating = false; viewModel.createWorkspace(it) }, onDismiss = { creating = false },
        )
    }
    renaming?.let { workspace ->
        TextInputDialog(
            title = stringResource(R.string.workspace_rename), confirmLabel = stringResource(R.string.action_save), dismissLabel = stringResource(R.string.action_cancel),
            initial = workspace.name, placeholder = stringResource(R.string.workspace_name_hint),
            onConfirm = { renaming = null; viewModel.renameWorkspace(workspace, it) }, onDismiss = { renaming = null },
        )
    }
    deleting?.let { workspace ->
        ConfirmDialog(
            title = stringResource(R.string.workspace_delete), message = stringResource(R.string.workspace_delete_message, workspace.name),
            confirmLabel = stringResource(R.string.action_delete), dismissLabel = stringResource(R.string.action_cancel), destructive = true,
            onConfirm = { deleting = null; viewModel.deleteWorkspace(workspace.id) }, onDismiss = { deleting = null },
        )
    }
    pickingIcon?.let { workspace ->
        EmojiPickerDialog(
            title = stringResource(R.string.page_choose_icon), allowRemove = false,
            onPick = { emoji -> pickingIcon = null; if (emoji != null) viewModel.setWorkspaceIcon(workspace, emoji) },
            onDismiss = { pickingIcon = null },
        )
    }
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    val colors = ImadTheme.colors
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surfaceAlt).padding(4.dp)) {
        for ((value, label) in options) {
            val active = value == selected
            Text(
                label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium),
                color = if (active) colors.text else colors.textSecondary, textAlign = TextAlign.Center, maxLines = 1,
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(9.dp)).background(if (active) colors.surface else Color.Transparent)
                    .clickable(role = Role.RadioButton) { onSelect(value) }.semantics { this.selected = active }.padding(vertical = 10.dp),
            )
        }
    }
}
