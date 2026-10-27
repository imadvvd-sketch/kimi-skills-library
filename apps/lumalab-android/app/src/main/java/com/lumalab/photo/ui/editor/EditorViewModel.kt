package com.lumalab.photo.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumalab.core.AutoTone
import com.lumalab.core.CropSettings
import com.lumalab.core.EditHistory
import com.lumalab.core.EditSettings
import com.lumalab.core.Geometry
import com.lumalab.core.Histogram
import com.lumalab.core.ImageBuffer
import com.lumalab.core.Processor
import com.lumalab.photo.LumaLabApp
import com.lumalab.photo.R
import com.lumalab.photo.data.Flag
import com.lumalab.photo.data.PhotoEntry
import com.lumalab.photo.data.UserPreset
import com.lumalab.photo.image.BitmapIO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EditorTool { PRESETS, CROP, LIGHT, COLOR, EFFECTS, DETAIL }

enum class ExportTarget { DEVICE, SHARE }

sealed interface EditorEvent {
    data class Message(@StringRes val text: Int) : EditorEvent
    data class Share(val uri: Uri) : EditorEvent
}

/** A preset currently applied, so its strength can still be adjusted. */
data class AppliedPreset(val key: String, val look: EditSettings, val base: EditSettings, val amount: Float = 1f)

class EditorViewModel(app: LumaLabApp, private val photoId: String) : ViewModel() {
    private val repo = app.repository
    private val context = app.applicationContext

    private var source: ImageBuffer? = null
    private var interactiveSource: ImageBuffer? = null
    private var presetSource: ImageBuffer? = null
    private var history = EditHistory(EditSettings())

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    private val _settings = MutableStateFlow(EditSettings())
    val settings: StateFlow<EditSettings> = _settings.asStateFlow()

    private val _tool = MutableStateFlow<EditorTool?>(EditorTool.LIGHT)
    val tool: StateFlow<EditorTool?> = _tool.asStateFlow()

    /** True while a slider or crop handle is being dragged; renders at reduced size for responsiveness. */
    private val dragging = MutableStateFlow(false)

    private val _preview = MutableStateFlow<Bitmap?>(null)
    val preview: StateFlow<Bitmap?> = _preview.asStateFlow()
    private val _original = MutableStateFlow<Bitmap?>(null)
    val original: StateFlow<Bitmap?> = _original.asStateFlow()
    private val _histogram = MutableStateFlow<Histogram?>(null)
    val histogram: StateFlow<Histogram?> = _histogram.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    private val _appliedPreset = MutableStateFlow<AppliedPreset?>(null)
    val appliedPreset: StateFlow<AppliedPreset?> = _appliedPreset.asStateFlow()

    private val _presetThumbs = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val presetThumbs: StateFlow<Map<String, Bitmap>> = _presetThumbs.asStateFlow()

    private val _events = MutableSharedFlow<EditorEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<EditorEvent> = _events.asSharedFlow()

    val entry: StateFlow<PhotoEntry?> = repo.library.map { doc -> doc.photos.firstOrNull { it.id == photoId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, repo.photo(photoId))
    val userPresets: StateFlow<List<UserPreset>> = repo.library.map { it.presets }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val canPaste: StateFlow<Boolean> = repo.clipboard.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val photo = repo.photo(photoId)
        val decoded = photo?.let {
            withContext(Dispatchers.IO) { runCatching { BitmapIO.decode(repo.fileFor(it), PREVIEW_SIZE) }.getOrNull() }
        }
        if (photo == null || decoded == null) {
            _failed.value = true
            _loading.value = false
            return
        }
        source = decoded
        withContext(Dispatchers.Default) {
            interactiveSource = decoded.downscaled(INTERACTIVE_SIZE)
            presetSource = decoded.downscaled(PRESET_THUMB_SIZE)
            _original.value = BitmapIO.toBitmap(decoded)
        }
        history = EditHistory(photo.edits)
        _settings.value = photo.edits
        syncHistoryFlags()
        _loading.value = false

        combine(_settings, _tool, dragging) { s, t, d -> RenderRequest(s, t == EditorTool.CROP, d) }
            .conflate()
            .collect { render(it) }
    }

    private data class RenderRequest(val settings: EditSettings, val cropMode: Boolean, val interactive: Boolean)

    private suspend fun render(request: RenderRequest) {
        val src = (if (request.interactive) interactiveSource else source) ?: return
        val ctx = currentCoroutineContext()
        try {
            val (bitmap, hist) = withContext(Dispatchers.Default) {
                val out = Processor.process(src, request.settings, includeCropRect = !request.cropMode) { ctx.ensureActive() }
                BitmapIO.toBitmap(out) to Histogram.of(out, step = 3)
            }
            _preview.value = bitmap
            _histogram.value = hist
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Preview render ran out of memory", e)
        }
    }

    // region editing

    /** Live update while interacting; call [commit] when the gesture ends. */
    fun update(transform: (EditSettings) -> EditSettings) {
        _settings.value = transform(_settings.value)
        if (_appliedPreset.value != null && !presetAmountChanging) _appliedPreset.value = null
    }

    fun setDragging(value: Boolean) {
        dragging.value = value
    }

    fun commit() {
        dragging.value = false
        if (history.push(_settings.value)) {
            repo.updateEdits(photoId, _settings.value)
            syncHistoryFlags()
        }
    }

    fun undo() {
        _settings.value = history.undo()
        _appliedPreset.value = null
        repo.updateEdits(photoId, _settings.value)
        syncHistoryFlags()
    }

    fun redo() {
        _settings.value = history.redo()
        _appliedPreset.value = null
        repo.updateEdits(photoId, _settings.value)
        syncHistoryFlags()
    }

    private fun syncHistoryFlags() {
        _canUndo.value = history.canUndo
        _canRedo.value = history.canRedo
    }

    fun selectTool(value: EditorTool?) {
        _tool.value = if (_tool.value == value) null else value
        if (value == EditorTool.PRESETS && _presetThumbs.value.isEmpty()) renderPresetThumbs()
    }

    fun resetAll() {
        update { EditSettings() }
        commit()
    }

    fun autoTone() {
        val src = source ?: return
        viewModelScope.launch {
            val auto = withContext(Dispatchers.Default) { AutoTone.compute(src, _settings.value) }
            update { auto }
            commit()
            _events.emit(EditorEvent.Message(R.string.auto_applied))
        }
    }

    fun copySettings() {
        repo.copySettings(_settings.value)
        _events.tryEmit(EditorEvent.Message(R.string.settings_copied))
    }

    fun pasteSettings() {
        val look = repo.clipboard.value ?: return
        update { EditSettings.applyLook(it, look) }
        commit()
        _events.tryEmit(EditorEvent.Message(R.string.settings_pasted))
    }

    fun setRating(value: Int) = repo.setRating(photoId, value)

    fun setFlag(value: Flag) = repo.setFlag(photoId, value)

    // endregion

    // region presets

    private var presetAmountChanging = false

    fun applyPreset(key: String, look: EditSettings) {
        val current = _appliedPreset.value
        val base = if (current != null) current.base else _settings.value
        if (current?.key == key) return
        val preset = AppliedPreset(key, look, base)
        presetAmountChanging = true
        update { EditSettings.applyLook(base, look) }
        presetAmountChanging = false
        _appliedPreset.value = preset
        commit()
    }

    fun setPresetAmount(amount: Float) {
        val preset = _appliedPreset.value ?: return
        presetAmountChanging = true
        update { EditSettings.applyLook(preset.base, preset.look.scaled(amount)) }
        presetAmountChanging = false
        _appliedPreset.value = preset.copy(amount = amount)
    }

    fun savePreset(name: String) {
        if (name.isBlank()) return
        repo.savePreset(name, _settings.value)
        _events.tryEmit(EditorEvent.Message(R.string.preset_saved))
    }

    fun deletePreset(id: String) = repo.deletePreset(id)

    /** Renders a small preview of a user preset for the presets panel. */
    fun presetThumbFor(key: String, look: EditSettings) {
        if (_presetThumbs.value.containsKey(key)) return
        val src = presetSource ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val bmp = BitmapIO.toBitmap(Processor.process(src, look.copy(crop = orientationOnly())))
            _presetThumbs.value = _presetThumbs.value + (key to bmp)
        }
    }

    private fun renderPresetThumbs() {
        viewModelScope.launch(Dispatchers.Default) {
            val src = presetSource ?: return@launch
            val geometry = orientationOnly()
            for (preset in com.lumalab.core.Presets.all) {
                val bmp = BitmapIO.toBitmap(Processor.process(src, preset.settings.copy(crop = geometry)))
                _presetThumbs.value = _presetThumbs.value + (preset.id to bmp)
            }
        }
    }

    private fun orientationOnly(): CropSettings = _settings.value.crop.let {
        CropSettings(quarterTurns = it.quarterTurns, flipHorizontal = it.flipHorizontal, flipVertical = it.flipVertical)
    }

    // endregion

    // region crop

    /** Pixel size of the frame the crop rectangle refers to. */
    fun cropFrameSize(): Pair<Int, Int>? {
        val src = source ?: return null
        return Geometry.orientedSize(src.width, src.height, _settings.value.crop.quarterTurns)
    }

    fun setAspect(ratio: Float) {
        val (fw, fh) = cropFrameSize() ?: return
        update {
            if (ratio <= 0f) {
                it.copy(crop = it.crop.copy(aspectRatio = 0f))
            } else {
                val r = Geometry.centeredAspectRect(fw, fh, ratio)
                it.copy(crop = it.crop.copy(aspectRatio = ratio, left = r[0], top = r[1], right = r[2], bottom = r[3]))
            }
        }
        commit()
    }

    fun rotate() {
        update { it.copy(crop = it.crop.copy(quarterTurns = (it.crop.quarterTurns + 1) % 4).withoutRect().copy(aspectRatio = 0f)) }
        _presetThumbs.value = emptyMap()
        commit()
    }

    fun flipHorizontal() {
        update { s -> s.copy(crop = s.crop.copy(flipHorizontal = !s.crop.flipHorizontal, left = 1f - s.crop.right, right = 1f - s.crop.left)) }
        _presetThumbs.value = emptyMap()
        commit()
    }

    fun flipVertical() {
        update { s -> s.copy(crop = s.crop.copy(flipVertical = !s.crop.flipVertical, top = 1f - s.crop.bottom, bottom = 1f - s.crop.top)) }
        _presetThumbs.value = emptyMap()
        commit()
    }

    fun resetCrop() {
        update { it.copy(crop = CropSettings()) }
        _presetThumbs.value = emptyMap()
        commit()
    }

    // endregion

    // region export

    fun export(target: ExportTarget, maxDim: Int, quality: Int) {
        val photo = entry.value ?: return
        if (_exporting.value) return
        viewModelScope.launch {
            _exporting.value = true
            try {
                val name = "LumaLab_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val uri = withContext(Dispatchers.Default) {
                    val full = BitmapIO.decode(repo.fileFor(photo), maxDim) ?: error("Decode failed")
                    val out = Processor.process(full, _settings.value)
                    when (target) {
                        ExportTarget.DEVICE -> BitmapIO.saveToGallery(context, out, quality, name)
                        ExportTarget.SHARE -> BitmapIO.writeShareable(context, out, quality, name)
                    }
                }
                _events.emit(if (target == ExportTarget.SHARE) EditorEvent.Share(uri) else EditorEvent.Message(R.string.export_done))
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                Log.e(TAG, "Export failed", t)
                _events.emit(EditorEvent.Message(R.string.export_failed))
            } finally {
                _exporting.value = false
            }
        }
    }

    // endregion

    override fun onCleared() {
        if (!_loading.value && !_failed.value) {
            repo.updateEdits(photoId, history.current)
        }
        super.onCleared()
    }

    companion object {
        private const val TAG = "EditorViewModel"
        const val PREVIEW_SIZE = 1600
        const val INTERACTIVE_SIZE = 900
        const val PRESET_THUMB_SIZE = 200
    }
}
