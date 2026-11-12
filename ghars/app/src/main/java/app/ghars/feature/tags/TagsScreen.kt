package app.ghars.feature.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.ghars.R
import app.ghars.core.designsystem.GharsTagColors
import app.ghars.core.designsystem.GharsTheme
import app.ghars.core.ui.GharsButton
import app.ghars.core.ui.GharsButtonStyle
import app.ghars.data.db.TagEntity
import app.ghars.data.prefs.SettingsStore
import app.ghars.data.repo.TagRepository
import app.ghars.feature.history.TagDot
import app.ghars.feature.history.tagLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TagsUiState(val tags: List<TagEntity> = emptyList(), val selectedId: Long? = null)

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val repository: TagRepository,
    private val settings: SettingsStore,
) : ViewModel() {

    val ui = combine(repository.observeAll(), settings.tagId) { tags, selected ->
        // A selection pointing at a deleted tag simply reads as "no tag".
        TagsUiState(tags, selected?.takeIf { id -> tags.any { it.id == id } })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TagsUiState())

    fun select(id: Long?) {
        viewModelScope.launch { settings.setTagId(id) }
    }

    fun save(id: Long?, name: String, color: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            if (id == null) repository.create(name, color) else repository.update(id, name, color)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }
}

/** What the editor dialog is working on. */
private sealed interface Editing {
    data object None : Editing
    data object New : Editing
    data class Existing(val id: Long) : Editing
}

/**
 * S04 and S11 in one screen.
 * With [onPicked] it is the picker before planting: tap a tag to use it.
 * Without, it is tag management from the menu: tap a custom tag to edit it.
 */
@Composable
fun TagsScreen(onPicked: (() -> Unit)? = null, viewModel: TagsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val spacing = GharsTheme.spacing
    val picking = onPicked != null
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val editing: Editing = when {
        creating -> Editing.New
        editingId != null -> Editing.Existing(editingId!!)
        else -> Editing.None
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(spacing.space4),
        verticalArrangement = Arrangement.spacedBy(spacing.space2),
    ) {
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(spacing.space2)) {
            if (picking) {
                TagRow(tag = null, selected = ui.selectedId == null, selectable = true) {
                    viewModel.select(null)
                    onPicked?.invoke()
                }
            }
            ui.tags.forEach { tag ->
                TagRow(
                    tag = tag,
                    selected = picking && tag.id == ui.selectedId,
                    selectable = picking,
                    // Built-in tags are fixed, so in management they are not tappable.
                    onClick = when {
                        picking -> ({ viewModel.select(tag.id); onPicked?.invoke() })
                        !tag.isBuiltin -> ({ editingId = tag.id })
                        else -> null
                    },
                )
            }
        }
        if (!picking && ui.tags.none { !it.isBuiltin }) {
            Text(
                text = stringResource(R.string.tags_empty_custom),
                style = GharsTheme.type.body,
                color = GharsTheme.colors.inkMuted,
                modifier = Modifier.padding(vertical = spacing.space2),
            )
        }
        Spacer(Modifier.height(spacing.space2))
        GharsButton(
            text = stringResource(R.string.tag_new),
            onClick = { creating = true },
            modifier = Modifier.fillMaxWidth(),
            style = GharsButtonStyle.Secondary,
        )
    }

    if (editing != Editing.None) {
        val existing = (editing as? Editing.Existing)?.let { e -> ui.tags.firstOrNull { it.id == e.id } }
        val close = {
            creating = false
            editingId = null
        }
        TagEditorDialog(
            existing = existing,
            onSave = { name, color ->
                viewModel.save(existing?.id, name, color)
                close()
            },
            onDelete = existing?.let { tag -> { viewModel.delete(tag.id); close() } },
            onDismiss = close,
        )
    }
}

@Composable
private fun TagRow(tag: TagEntity?, selected: Boolean, selectable: Boolean, onClick: (() -> Unit)?) {
    val colors = GharsTheme.colors
    val shape = GharsTheme.radius.mdShape
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GharsTheme.sizes.touchTarget)
            .clip(shape)
            .background(if (selected) colors.brandSoft else colors.surface)
            .then(if (selected) Modifier.border(GharsTheme.sizes.focusRing, colors.brand, shape) else Modifier)
            .then(
                when {
                    onClick == null -> Modifier
                    selectable -> Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
                    else -> Modifier.clickable(onClickLabel = stringResource(R.string.tag_edit), onClick = onClick)
                },
            )
            .padding(horizontal = GharsTheme.spacing.space4, vertical = GharsTheme.spacing.space3),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagDot(tag)
        Spacer(Modifier.width(GharsTheme.spacing.space3))
        Text(text = tagLabel(tag), style = GharsTheme.type.body, color = colors.ink, modifier = Modifier.weight(1f))
        if (selected) {
            Icon(
                painterResource(R.drawable.ic_check), contentDescription = null,
                tint = colors.brand, modifier = Modifier.size(GharsTheme.sizes.icon),
            )
        }
    }
}

@Composable
private fun TagEditorDialog(
    existing: TagEntity?,
    onSave: (String, Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = GharsTheme.colors
    val spacing = GharsTheme.spacing
    val palette = GharsTagColors.map { it.toArgb() }
    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var color by rememberSaveable { mutableIntStateOf(existing?.color ?: palette.first()) }
    val save = { if (name.isNotBlank()) onSave(name, color) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(GharsTheme.radius.lgShape)
                .background(colors.surface)
                .verticalScroll(rememberScrollState())
                .padding(spacing.space6),
        ) {
            Text(
                text = stringResource(if (existing == null) R.string.tag_new else R.string.tag_edit),
                style = GharsTheme.type.heading,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(spacing.space4))
            val hint = stringResource(R.string.tag_name_hint)
            BasicTextField(
                value = name,
                onValueChange = { name = it.take(TagRepository.MAX_NAME).replace("\n", "") },
                singleLine = true,
                textStyle = GharsTheme.type.body.copy(color = colors.ink),
                cursorBrush = SolidColor(colors.brand),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { save() }),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
                decorationBox = { field ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = GharsTheme.sizes.touchTarget)
                            .clip(GharsTheme.radius.smShape)
                            .background(colors.surfaceSunk)
                            .padding(horizontal = spacing.space3, vertical = spacing.space3),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (name.isEmpty()) Text(text = hint, style = GharsTheme.type.body, color = colors.inkMuted)
                        field()
                    }
                },
            )
            Spacer(Modifier.height(spacing.space4))
            Text(text = stringResource(R.string.tag_color), style = GharsTheme.type.label, color = colors.inkMuted)
            Spacer(Modifier.height(spacing.space1))
            Row(modifier = Modifier.selectableGroup()) {
                palette.forEachIndexed { index, option ->
                    val selected = option == color
                    val label = stringResource(R.string.tag_color_option, index + 1, palette.size)
                    Box(
                        modifier = Modifier
                            .size(GharsTheme.sizes.touchTarget)
                            .clip(CircleShape)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { color = option })
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(GharsTheme.spacing.space8)
                                .clip(CircleShape)
                                .background(androidx.compose.ui.graphics.Color(option)),
                            contentAlignment = Alignment.Center,
                        ) {
                            // A check mark as well as position: the choice is never colour alone.
                            if (selected) {
                                Icon(
                                    painterResource(R.drawable.ic_check), contentDescription = null,
                                    tint = colors.surface, modifier = Modifier.size(GharsTheme.sizes.iconSmall),
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(spacing.space6))
            GharsButton(text = stringResource(R.string.tag_save), onClick = save, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(spacing.space2))
            GharsButton(
                text = stringResource(R.string.tag_cancel), onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(), style = GharsButtonStyle.Secondary,
            )
            if (onDelete != null) {
                Spacer(Modifier.height(spacing.space2))
                GharsButton(
                    text = stringResource(R.string.tag_delete), onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(), style = GharsButtonStyle.Danger,
                )
            }
        }
    }
}
