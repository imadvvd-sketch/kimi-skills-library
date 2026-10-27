package com.lumalab.photo.ui.editor

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Rotate90DegreesCw
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lumalab.core.CurveChannel
import com.lumalab.core.EditSettings
import com.lumalab.core.HslAdjustment
import com.lumalab.core.HslBand
import com.lumalab.core.PresetCategory
import com.lumalab.core.Presets
import com.lumalab.core.ToneCurveSettings
import com.lumalab.photo.R
import com.lumalab.photo.data.UserPreset
import com.lumalab.photo.ui.components.AdjustSlider
import com.lumalab.photo.ui.components.GradingRange
import com.lumalab.photo.ui.components.ToneCurveEditor
import com.lumalab.photo.ui.components.Tracks
import com.lumalab.photo.ui.theme.Accent
import com.lumalab.photo.ui.theme.Surface2
import com.lumalab.photo.ui.theme.TextMuted

/** Callbacks shared by every panel: live changes while dragging, and a commit when the gesture ends. */
class PanelActions(
    val change: ((EditSettings) -> EditSettings) -> Unit,
    val commit: () -> Unit,
)

@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Accent.copy(alpha = 0.22f),
    selectedLabelColor = Accent,
)

@Composable
fun LightPanel(s: EditSettings, a: PanelActions) {
    var showCurve by rememberSaveable { mutableStateOf(false) }
    var channel by rememberSaveable { mutableStateOf(CurveChannel.MASTER) }
    Column {
        AdjustSlider(stringResource(R.string.exposure), s.exposure, -5f..5f, { v -> a.change { it.copy(exposure = v) } }, a.commit, decimals = 2)
        AdjustSlider(stringResource(R.string.contrast), s.contrast, -100f..100f, { v -> a.change { it.copy(contrast = v) } }, a.commit)
        AdjustSlider(stringResource(R.string.highlights), s.highlights, -100f..100f, { v -> a.change { it.copy(highlights = v) } }, a.commit)
        AdjustSlider(stringResource(R.string.shadows), s.shadows, -100f..100f, { v -> a.change { it.copy(shadows = v) } }, a.commit)
        AdjustSlider(stringResource(R.string.whites), s.whites, -100f..100f, { v -> a.change { it.copy(whites = v) } }, a.commit)
        AdjustSlider(stringResource(R.string.blacks), s.blacks, -100f..100f, { v -> a.change { it.copy(blacks = v) } }, a.commit)
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = showCurve,
                onClick = { showCurve = !showCurve },
                label = { Text(stringResource(R.string.curve)) },
                colors = chipColors(),
            )
            if (showCurve) {
                CurveChannel.entries.forEach { c ->
                    val dot = when (c) {
                        CurveChannel.MASTER -> Color.White
                        CurveChannel.RED -> Color(0xFFFF5A5A)
                        CurveChannel.GREEN -> Color(0xFF5AE05A)
                        CurveChannel.BLUE -> Color(0xFF5A9BFF)
                    }
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (channel == c) Surface2 else Color.Transparent)
                            .clickable { channel = c },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(14.dp).background(dot, CircleShape))
                    }
                }
            }
        }
        if (showCurve) {
            ToneCurveEditor(
                points = s.toneCurve.points(channel),
                channel = channel,
                onChange = { pts -> a.change { it.copy(toneCurve = it.toneCurve.withPoints(channel, pts)) } },
                onCommit = a.commit,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.curve_hint), style = MaterialTheme.typography.labelSmall, color = TextMuted, modifier = Modifier.weight(1f))
                TextButton(onClick = {
                    a.change { it.copy(toneCurve = it.toneCurve.withPoints(channel, ToneCurveSettings.IDENTITY)) }
                    a.commit()
                }) { Text(stringResource(R.string.reset_curve)) }
            }
        }
    }
}

private val bandLabels = mapOf(
    HslBand.RED to R.string.red, HslBand.ORANGE to R.string.orange, HslBand.YELLOW to R.string.yellow,
    HslBand.GREEN to R.string.green, HslBand.AQUA to R.string.aqua, HslBand.BLUE to R.string.blue,
    HslBand.PURPLE to R.string.purple, HslBand.MAGENTA to R.string.magenta,
)

@Composable
fun ColorPanel(s: EditSettings, a: PanelActions) {
    var section by rememberSaveable { mutableStateOf(0) } // 0 = none, 1 = mix, 2 = grading
    var band by rememberSaveable { mutableStateOf(HslBand.RED) }
    Column {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = s.monochrome,
                onClick = { a.change { it.copy(monochrome = !it.monochrome) }; a.commit() },
                label = { Text(stringResource(R.string.black_white)) },
                colors = chipColors(),
            )
            FilterChip(selected = section == 1, onClick = { section = if (section == 1) 0 else 1 }, label = { Text(stringResource(R.string.color_mix)) }, colors = chipColors())
            FilterChip(selected = section == 2, onClick = { section = if (section == 2) 0 else 2 }, label = { Text(stringResource(R.string.color_grading)) }, colors = chipColors())
        }
        AdjustSlider(stringResource(R.string.temperature), s.temperature, -100f..100f, { v -> a.change { it.copy(temperature = v) } }, a.commit, track = Tracks.temperature)
        AdjustSlider(stringResource(R.string.tint), s.tint, -100f..100f, { v -> a.change { it.copy(tint = v) } }, a.commit, track = Tracks.tint)
        AdjustSlider(stringResource(R.string.vibrance), s.vibrance, -100f..100f, { v -> a.change { it.copy(vibrance = v) } }, a.commit)
        AdjustSlider(stringResource(R.string.saturation), s.saturation, -100f..100f, { v -> a.change { it.copy(saturation = v) } }, a.commit)

        if (section == 1) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HslBand.entries.forEach { b ->
                    val color = Tracks.hueColor(b.centerHue)
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .then(if (band == b) Modifier.border(2.dp, Color.White, CircleShape) else Modifier)
                            .padding(5.dp)
                            .background(color, CircleShape)
                            .clickable { band = b },
                    )
                }
            }
            val adj = s.hslBand(band)
            val center = Tracks.hueColor(band.centerHue)
            Text(stringResource(bandLabels.getValue(band)), modifier = Modifier.padding(horizontal = 16.dp), color = center)
            fun set(value: HslAdjustment) = a.change { it.withHslBand(band, value) }
            AdjustSlider(stringResource(R.string.hue), adj.hue, -100f..100f, { set(adj.copy(hue = it)) }, a.commit, track = Tracks.hueShift(band.centerHue))
            AdjustSlider(stringResource(R.string.saturation), adj.saturation, -100f..100f, { set(adj.copy(saturation = it)) }, a.commit, track = Tracks.saturation(center))
            AdjustSlider(stringResource(R.string.luminance), adj.luminance, -100f..100f, { set(adj.copy(luminance = it)) }, a.commit, track = Tracks.luminance)
        }
        if (section == 2) {
            val g = s.grading
            val hue = stringResource(R.string.hue)
            val sat = stringResource(R.string.saturation)
            GradingRange(stringResource(R.string.shadows), g.shadowsHue, g.shadowsSat, { h, v ->
                a.change { it.copy(grading = it.grading.copy(shadowsHue = h, shadowsSat = v)) }
            }, a.commit, hue, sat, defaultHue = 220f)
            GradingRange(stringResource(R.string.midtones), g.midtonesHue, g.midtonesSat, { h, v ->
                a.change { it.copy(grading = it.grading.copy(midtonesHue = h, midtonesSat = v)) }
            }, a.commit, hue, sat, defaultHue = 40f)
            GradingRange(stringResource(R.string.highlights), g.highlightsHue, g.highlightsSat, { h, v ->
                a.change { it.copy(grading = it.grading.copy(highlightsHue = h, highlightsSat = v)) }
            }, a.commit, hue, sat, defaultHue = 45f)
            AdjustSlider(stringResource(R.string.blending), g.blending, 0f..100f, { v ->
                a.change { it.copy(grading = it.grading.copy(blending = v)) }
            }, a.commit, default = 50f)
            AdjustSlider(stringResource(R.string.balance), g.balance, -100f..100f, { v ->
                a.change { it.copy(grading = it.grading.copy(balance = v)) }
            }, a.commit)
        }
    }
}

@Composable
fun EffectsPanel(s: EditSettings, a: PanelActions) {
    Column {
        AdjustSlider(stringResource(R.string.texture), s.texture, -100f..100f, { v -> a.change { it.copy(texture = v) } }, a.commit)
        AdjustSlider(stringResource(R.string.clarity), s.clarity, -100f..100f, { v -> a.change { it.copy(clarity = v) } }, a.commit)
        AdjustSlider(stringResource(R.string.dehaze), s.dehaze, -100f..100f, { v -> a.change { it.copy(dehaze = v) } }, a.commit)
        AdjustSlider(stringResource(R.string.vignette), s.vignette, -100f..100f, { v -> a.change { it.copy(vignette = v) } }, a.commit)
        if (s.vignette != 0f) {
            AdjustSlider(stringResource(R.string.midpoint), s.vignetteMidpoint, 0f..100f, { v -> a.change { it.copy(vignetteMidpoint = v) } }, a.commit, default = 50f)
            AdjustSlider(stringResource(R.string.feather), s.vignetteFeather, 0f..100f, { v -> a.change { it.copy(vignetteFeather = v) } }, a.commit, default = 50f)
        }
        AdjustSlider(stringResource(R.string.grain), s.grain, 0f..100f, { v -> a.change { it.copy(grain = v) } }, a.commit)
        if (s.grain > 0f) {
            AdjustSlider(stringResource(R.string.size), s.grainSize, 0f..100f, { v -> a.change { it.copy(grainSize = v) } }, a.commit, default = 25f)
        }
    }
}

@Composable
fun DetailPanel(s: EditSettings, a: PanelActions) {
    Column {
        AdjustSlider(stringResource(R.string.sharpening), s.sharpening, 0f..150f, { v -> a.change { it.copy(sharpening = v) } }, a.commit)
        AdjustSlider(stringResource(R.string.radius), s.sharpenRadius, 0.5f..3f, { v -> a.change { it.copy(sharpenRadius = v) } }, a.commit, default = 1f, decimals = 1)
        AdjustSlider(stringResource(R.string.noise_reduction), s.noiseReduction, 0f..100f, { v -> a.change { it.copy(noiseReduction = v) } }, a.commit)
        AdjustSlider(stringResource(R.string.color_noise), s.colorNoiseReduction, 0f..100f, { v -> a.change { it.copy(colorNoiseReduction = v) } }, a.commit)
    }
}

private data class AspectOption(@StringRes val label: Int?, val text: String?, val ratio: Float)

@Composable
fun CropPanel(s: EditSettings, a: PanelActions, vm: EditorViewModel) {
    val frame = vm.cropFrameSize()
    val original = frame?.let { it.first.toFloat() / it.second } ?: 0f
    val options = listOf(
        AspectOption(R.string.aspect_free, null, 0f),
        AspectOption(R.string.aspect_original, null, original),
        AspectOption(null, "1:1", 1f),
        AspectOption(null, "4:5", 4f / 5f),
        AspectOption(null, "5:4", 5f / 4f),
        AspectOption(null, "3:2", 3f / 2f),
        AspectOption(null, "2:3", 2f / 3f),
        AspectOption(null, "16:9", 16f / 9f),
        AspectOption(null, "9:16", 9f / 16f),
    )
    Column {
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { opt ->
                val selected = kotlin.math.abs(s.crop.aspectRatio - opt.ratio) < 0.001f
                FilterChip(
                    selected = selected,
                    onClick = { vm.setAspect(opt.ratio) },
                    label = { Text(opt.label?.let { stringResource(it) } ?: opt.text.orEmpty()) },
                    colors = chipColors(),
                )
            }
        }
        AdjustSlider(stringResource(R.string.straighten), s.crop.angle, -45f..45f, { v -> a.change { it.copy(crop = it.crop.copy(angle = v)) } }, a.commit, decimals = 1)
        Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            ToolAction(Icons.Filled.Rotate90DegreesCw, R.string.rotate, vm::rotate)
            ToolAction(Icons.Filled.Flip, R.string.flip_horizontal, vm::flipHorizontal)
            ToolAction(Icons.Filled.Flip, R.string.flip_vertical, vm::flipVertical, rotateIcon = true)
            ToolAction(Icons.Filled.RestartAlt, R.string.reset, vm::resetCrop)
        }
    }
}

@Composable
private fun ToolAction(icon: androidx.compose.ui.graphics.vector.ImageVector, @StringRes label: Int, onClick: () -> Unit, rotateIcon: Boolean = false) {
    Column(
        Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, modifier = if (rotateIcon) Modifier.rotate(90f) else Modifier)
        Text(stringResource(label), style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

private val categoryLabels = mapOf(
    PresetCategory.RECOMMENDED to R.string.category_recommended,
    PresetCategory.PORTRAIT to R.string.category_portrait,
    PresetCategory.LANDSCAPE to R.string.category_landscape,
    PresetCategory.CINEMATIC to R.string.category_cinematic,
    PresetCategory.VINTAGE to R.string.category_vintage,
    PresetCategory.BLACK_AND_WHITE to R.string.category_black_and_white,
)

private val presetLabels = mapOf(
    "auto_pop" to R.string.preset_auto_pop, "bright_airy" to R.string.preset_bright_airy, "moody" to R.string.preset_moody,
    "portrait_soft" to R.string.preset_portrait_soft, "portrait_warm" to R.string.preset_portrait_warm,
    "portrait_clean" to R.string.preset_portrait_clean, "landscape_vivid" to R.string.preset_landscape_vivid,
    "landscape_haze" to R.string.preset_landscape_haze, "landscape_autumn" to R.string.preset_landscape_autumn,
    "cine_teal_orange" to R.string.preset_cine_teal_orange, "cine_noir" to R.string.preset_cine_noir,
    "cine_matte" to R.string.preset_cine_matte, "vintage_70s" to R.string.preset_vintage_70s,
    "vintage_polaroid" to R.string.preset_vintage_polaroid, "vintage_sepia" to R.string.preset_vintage_sepia,
    "bw_classic" to R.string.preset_bw_classic, "bw_high_contrast" to R.string.preset_bw_high_contrast,
    "bw_soft" to R.string.preset_bw_soft, "bw_infrared" to R.string.preset_bw_infrared,
)

/** -1 selects the user's own presets. */
private const val USER_CATEGORY = -1

@Composable
fun PresetsPanel(
    applied: AppliedPreset?,
    thumbs: Map<String, Bitmap>,
    userPresets: List<UserPreset>,
    vm: EditorViewModel,
    a: PanelActions,
) {
    var category by rememberSaveable { mutableStateOf(PresetCategory.RECOMMENDED.ordinal) }
    Column(Modifier.padding(vertical = 4.dp)) {
        if (applied != null) {
            AdjustSlider(
                stringResource(R.string.preset_amount), applied.amount * 100f, 0f..200f,
                { v -> vm.setDragging(true); vm.setPresetAmount(v / 100f) }, a.commit, default = 100f, signed = false,
            )
        }
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = category == USER_CATEGORY, onClick = { category = USER_CATEGORY },
                    label = { Text(stringResource(R.string.category_user)) }, colors = chipColors(),
                )
            }
            items(PresetCategory.entries) { c ->
                FilterChip(
                    selected = category == c.ordinal, onClick = { category = c.ordinal },
                    label = { Text(stringResource(categoryLabels.getValue(c))) }, colors = chipColors(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        if (category == USER_CATEGORY) {
            if (userPresets.isEmpty()) {
                Text(stringResource(R.string.no_user_presets), color = TextMuted, modifier = Modifier.padding(16.dp))
            } else {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(userPresets, key = { it.id }) { p ->
                        val key = "user:" + p.id
                        LaunchedEffect(key) { vm.presetThumbFor(key, p.settings) }
                        PresetTile(p.name, thumbs[key], applied?.key == key, onClick = { vm.applyPreset(key, p.settings) }, onLongClick = { vm.deletePreset(p.id) })
                    }
                }
            }
        } else {
            val list = Presets.all.filter { it.category.ordinal == category }
            LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(list, key = { it.id }) { p ->
                    val label = presetLabels[p.id]?.let { stringResource(it) } ?: p.name
                    PresetTile(label, thumbs[p.id], applied?.key == p.id, onClick = { vm.applyPreset(p.id, p.settings) })
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetTile(name: String, thumb: Bitmap?, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Column(
        Modifier.width(84.dp).combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(84.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Surface2)
                .then(if (selected) Modifier.border(2.dp, Accent, RoundedCornerShape(6.dp)) else Modifier),
        ) {
            thumb?.let { Image(it.asImageBitmap(), null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        }
        Text(
            name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall,
            color = if (selected) Accent else Color.White, modifier = Modifier.padding(top = 4.dp),
        )
    }
}
