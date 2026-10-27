package com.lumalab.photo.ui.editor

import android.content.Intent
import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Details
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumalab.photo.LumaLabApp
import com.lumalab.photo.R
import com.lumalab.photo.data.Flag
import com.lumalab.photo.data.PhotoEntry
import com.lumalab.photo.ui.components.CropOverlay
import com.lumalab.photo.ui.components.CropRect
import com.lumalab.photo.ui.components.HistogramView
import com.lumalab.photo.ui.components.AdjustSlider
import com.lumalab.photo.ui.theme.Accent
import com.lumalab.photo.ui.theme.Gold
import com.lumalab.photo.ui.theme.Surface1
import com.lumalab.photo.ui.theme.TextMuted
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date
import kotlin.math.roundToInt

private data class ToolItem(val tool: EditorTool?, val icon: ImageVector, @StringRes val label: Int)

private val toolItems = listOf(
    ToolItem(EditorTool.PRESETS, Icons.Filled.Style, R.string.tool_presets),
    ToolItem(null, Icons.Filled.AutoAwesome, R.string.tool_auto),
    ToolItem(EditorTool.CROP, Icons.Filled.Crop, R.string.tool_crop),
    ToolItem(EditorTool.LIGHT, Icons.Filled.WbSunny, R.string.tool_light),
    ToolItem(EditorTool.COLOR, Icons.Filled.Palette, R.string.tool_color),
    ToolItem(EditorTool.EFFECTS, Icons.Filled.BlurOn, R.string.tool_effects),
    ToolItem(EditorTool.DETAIL, Icons.Filled.Details, R.string.tool_detail),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(photoId: String, onBack: () -> Unit) {
    val vm: EditorViewModel = viewModel(key = photoId) { EditorViewModel(this[APPLICATION_KEY] as LumaLabApp, photoId) }
    val context = LocalContext.current
    val loading by vm.loading.collectAsStateWithLifecycle()
    val failed by vm.failed.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val tool by vm.tool.collectAsStateWithLifecycle()
    val preview by vm.preview.collectAsStateWithLifecycle()
    val original by vm.original.collectAsStateWithLifecycle()
    val histogram by vm.histogram.collectAsStateWithLifecycle()
    val canUndo by vm.canUndo.collectAsStateWithLifecycle()
    val canRedo by vm.canRedo.collectAsStateWithLifecycle()
    val canPaste by vm.canPaste.collectAsStateWithLifecycle()
    val exporting by vm.exporting.collectAsStateWithLifecycle()
    val applied by vm.appliedPreset.collectAsStateWithLifecycle()
    val thumbs by vm.presetThumbs.collectAsStateWithLifecycle()
    val userPresets by vm.userPresets.collectAsStateWithLifecycle()
    val entry by vm.entry.collectAsStateWithLifecycle()

    var showOriginal by remember { mutableStateOf(false) }
    var showHistogram by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showExport by rememberSaveable { mutableStateOf(false) }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showSavePreset by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is EditorEvent.Message -> scope.launch { snackbar.showSnackbar(context.getString(event.text)) }
                is EditorEvent.Share -> {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "image/jpeg"
                        putExtra(Intent.EXTRA_STREAM, event.uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(send, context.getString(R.string.share)))
                }
            }
        }
    }

    val actions = remember(vm) {
        PanelActions(
            change = { transform -> vm.setDragging(true); vm.update(transform) },
            commit = vm::commit,
        )
    }

    Scaffold(
        containerColor = Color.Black,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
                title = {},
                actions = {
                    HoldButton(
                        icon = Icons.Filled.Compare,
                        description = stringResource(R.string.compare),
                        onHold = { showOriginal = it },
                    )
                    IconButton(onClick = { showHistogram = !showHistogram }) {
                        Icon(Icons.Filled.BarChart, stringResource(R.string.histogram), tint = if (showHistogram) Accent else Color.White)
                    }
                    IconButton(onClick = vm::undo, enabled = canUndo) { Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo)) }
                    IconButton(onClick = vm::redo, enabled = canRedo) { Icon(Icons.AutoMirrored.Filled.Redo, stringResource(R.string.redo)) }
                    IconButton(onClick = { showExport = true }) { Icon(Icons.Filled.IosShare, stringResource(R.string.export)) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more)) }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.info)) }, onClick = { menuOpen = false; showInfo = true })
                            DropdownMenuItem(text = { Text(stringResource(R.string.copy_settings)) }, onClick = { menuOpen = false; vm.copySettings() })
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.paste_settings)) },
                                enabled = canPaste,
                                onClick = { menuOpen = false; vm.pasteSettings() },
                            )
                            DropdownMenuItem(text = { Text(stringResource(R.string.save_preset)) }, onClick = { menuOpen = false; showSavePreset = true })
                            DropdownMenuItem(text = { Text(stringResource(R.string.reset_all)) }, onClick = { menuOpen = false; vm.resetAll() })
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    failed -> Text(stringResource(R.string.error_load), color = TextMuted, modifier = Modifier.align(Alignment.Center))
                    loading -> CircularProgressIndicator(color = Accent, modifier = Modifier.align(Alignment.Center))
                    else -> {
                        val image = if (showOriginal && tool != EditorTool.CROP) original else preview
                        Viewport(
                            bitmap = image,
                            cropMode = tool == EditorTool.CROP,
                            crop = CropRect(settings.crop.left, settings.crop.top, settings.crop.right, settings.crop.bottom),
                            aspect = settings.crop.aspectRatio,
                            onCropChange = { r ->
                                vm.setDragging(true)
                                vm.update { it.copy(crop = it.crop.copy(left = r.left, top = r.top, right = r.right, bottom = r.bottom)) }
                            },
                            onCropCommit = vm::commit,
                        )
                    }
                }
                val hist = histogram
                if (showHistogram && hist != null) {
                    HistogramView(hist, Modifier.align(Alignment.TopEnd).padding(8.dp).size(width = 150.dp, height = 80.dp))
                }
                if (exporting) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.align(Alignment.Center),
                    ) {
                        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Accent, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(stringResource(R.string.exporting))
                        }
                    }
                }
            }
            if (!loading && !failed) {
                val current = tool
                if (current != null) {
                    Surface(color = Surface1) {
                        Column(Modifier.fillMaxWidth().heightIn(max = 320.dp).verticalScroll(rememberScrollState()).padding(vertical = 6.dp)) {
                            when (current) {
                                EditorTool.PRESETS -> PresetsPanel(applied, thumbs, userPresets, vm, actions)
                                EditorTool.CROP -> CropPanel(settings, actions, vm)
                                EditorTool.LIGHT -> LightPanel(settings, actions)
                                EditorTool.COLOR -> ColorPanel(settings, actions)
                                EditorTool.EFFECTS -> EffectsPanel(settings, actions)
                                EditorTool.DETAIL -> DetailPanel(settings, actions)
                            }
                        }
                    }
                }
                ToolBar(selected = current, onSelect = { item ->
                    if (item.tool == null) vm.autoTone() else vm.selectTool(item.tool)
                })
            }
        }
    }

    if (showExport) ExportDialog(onDismiss = { showExport = false }, onExport = { target, size, quality ->
        showExport = false
        vm.export(target, size, quality)
    })
    val photo = entry
    if (showInfo && photo != null) InfoDialog(photo, onDismiss = { showInfo = false }, onRating = vm::setRating, onFlag = vm::setFlag)
    if (showSavePreset) SavePresetDialog(onDismiss = { showSavePreset = false }, onSave = { showSavePreset = false; vm.savePreset(it) })
}

@Composable
private fun HoldButton(icon: ImageVector, description: String, onHold: (Boolean) -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    onHold(true)
                    try {
                        tryAwaitRelease()
                    } finally {
                        onHold(false)
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description)
    }
}

/** Displays the rendered image fitted to the available space, with pinch-zoom or the crop overlay. */
@Composable
private fun Viewport(
    bitmap: Bitmap?,
    cropMode: Boolean,
    crop: CropRect,
    aspect: Float,
    onCropChange: (CropRect) -> Unit,
    onCropCommit: () -> Unit,
) {
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(cropMode) { zoom = 1f; pan = Offset.Zero }
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds().padding(if (cropMode) 20.dp else 0.dp)) {
        val bmp = bitmap ?: return@BoxWithConstraints
        val cw = constraints.maxWidth.toFloat()
        val ch = constraints.maxHeight.toFloat()
        val scale = minOf(cw / bmp.width, ch / bmp.height)
        val dw = bmp.width * scale
        val dh = bmp.height * scale
        val left = (cw - dw) / 2f
        val top = (ch - dh) / 2f
        val image = remember(bmp) { bmp.asImageBitmap() }

        Canvas(
            Modifier
                .fillMaxSize()
                .then(
                    if (cropMode) Modifier else Modifier
                        .pointerInput(Unit) {
                            detectTapGestures(onDoubleTap = {
                                if (zoom > 1.01f) { zoom = 1f; pan = Offset.Zero } else zoom = 2.5f
                            })
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { _, panChange, zoomChange, _ ->
                                zoom = (zoom * zoomChange).coerceIn(1f, 8f)
                                val maxX = (zoom - 1f) * cw / 2f
                                val maxY = (zoom - 1f) * ch / 2f
                                pan = Offset(
                                    (pan.x + panChange.x).coerceIn(-maxX, maxX),
                                    (pan.y + panChange.y).coerceIn(-maxY, maxY),
                                )
                            }
                        }
                        .graphicsLayer {
                            scaleX = zoom
                            scaleY = zoom
                            translationX = pan.x
                            translationY = pan.y
                        },
                ),
        ) {
            drawImage(
                image,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                filterQuality = FilterQuality.High,
            )
        }
        if (cropMode) {
            with(density) {
                CropOverlay(
                    rect = crop,
                    pixelAspect = aspect,
                    frameAspect = bmp.width.toFloat() / bmp.height,
                    onChange = onCropChange,
                    onDragStart = {},
                    onCommit = onCropCommit,
                    modifier = Modifier
                        .offset(left.toDp(), top.toDp())
                        .size(dw.toDp(), dh.toDp()),
                )
            }
        }
    }
}

@Composable
private fun ToolBar(selected: EditorTool?, onSelect: (ToolItem) -> Unit) {
    Surface(color = Color.Black) {
        LazyRow(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items(toolItems) { item ->
                val active = item.tool != null && item.tool == selected
                Column(
                    Modifier
                        .width(72.dp)
                        .clickable { onSelect(item) }
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(item.icon, null, tint = if (active) Accent else Color.White)
                    Text(
                        stringResource(item.label),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) Accent else TextMuted,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportDialog(onDismiss: () -> Unit, onExport: (ExportTarget, Int, Int) -> Unit) {
    val sizes = listOf(2048 to R.string.size_small, 3072 to R.string.size_medium, 4096 to R.string.size_full)
    var size by rememberSaveable { mutableStateOf(4096) }
    var quality by rememberSaveable { mutableFloatStateOf(92f) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export)) },
        text = {
            Column {
                Text(stringResource(R.string.export_size), style = MaterialTheme.typography.titleSmall)
                sizes.forEach { (px, label) ->
                    Row(
                        Modifier.fillMaxWidth().selectable(selected = size == px, role = Role.RadioButton, onClick = { size = px }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = size == px, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(label))
                    }
                }
                AdjustSlider(stringResource(R.string.quality), quality, 60f..100f, { quality = it }, {}, default = 92f, signed = false)
            }
        },
        confirmButton = {
            TextButton(onClick = { onExport(ExportTarget.DEVICE, size, quality.roundToInt()) }) { Text(stringResource(R.string.save_to_device)) }
        },
        dismissButton = {
            TextButton(onClick = { onExport(ExportTarget.SHARE, size, quality.roundToInt()) }) { Text(stringResource(R.string.share)) }
        },
    )
}

@Composable
private fun InfoDialog(photo: PhotoEntry, onDismiss: () -> Unit, onRating: (Int) -> Unit, onFlag: (Flag) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.info)) },
        text = {
            Column {
                InfoRow(stringResource(R.string.dimensions), "${photo.width} × ${photo.height}")
                InfoRow(
                    stringResource(R.string.imported_on),
                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(photo.importedAt)),
                )
                Text(stringResource(R.string.rating), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                Row {
                    for (i in 1..5) {
                        IconButton(onClick = { onRating(if (photo.rating == i) 0 else i) }) {
                            Icon(
                                if (i <= photo.rating) Icons.Filled.Star else Icons.Filled.StarBorder,
                                null,
                                tint = if (i <= photo.rating) Gold else TextMuted,
                            )
                        }
                    }
                }
                Text(stringResource(R.string.flag), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(Flag.PICK to R.string.flag_pick, Flag.NONE to R.string.flag_none, Flag.REJECT to R.string.flag_reject).forEach { (flag, label) ->
                        FilterChip(
                            selected = photo.flag == flag,
                            onClick = { onFlag(flag) },
                            label = { Text(stringResource(label)) },
                            leadingIcon = if (flag == Flag.NONE) null else {
                                { Icon(Icons.Filled.Flag, null, tint = if (flag == Flag.REJECT) Color(0xFFFF5A5A) else Color.White, modifier = Modifier.size(16.dp)) }
                            },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) } },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, color = TextMuted, modifier = Modifier.weight(1f))
        Text(value)
    }
}

@Composable
private fun SavePresetDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.save_preset)) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(stringResource(R.string.preset_name)) })
        },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
