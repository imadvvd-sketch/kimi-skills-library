package com.imadnote.app.ui.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.imadnote.app.AppContainer
import com.imadnote.app.R
import com.imadnote.app.data.AttachmentEntity
import com.imadnote.app.ui.appViewModel
import com.imadnote.app.ui.components.AttachmentImage
import com.imadnote.app.ui.components.ConfirmDialog
import com.imadnote.app.ui.components.EmptyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ImageViewerViewModel(private val c: AppContainer, handle: SavedStateHandle) : ViewModel() {
    private val noteId: String = handle["noteId"] ?: ""
    val startIndex: Int = handle["index"] ?: 0
    val attachments = c.notes.observeAttachments(noteId)
        .map<List<AttachmentEntity>, List<AttachmentEntity>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun fileFor(a: AttachmentEntity): File = c.notes.fileFor(a)

    fun remove(a: AttachmentEntity) {
        viewModelScope.launch { c.notes.removeAttachment(a) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(onBack: () -> Unit) {
    val vm = appViewModel { c, h -> ImageViewerViewModel(c, h) }
    val list by vm.attachments.collectAsStateWithLifecycle()
    val images = list
    var confirmDelete by remember { mutableStateOf<AttachmentEntity?>(null) }
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    titleContentColor = Color.White,
                ),
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.Close, stringResource(R.string.action_close)) }
                },
                actions = {},
            )
        },
    ) { padding ->
        if (images == null) return@Scaffold
        if (images.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(Icons.Outlined.ImageNotSupported, stringResource(R.string.no_images), "")
            }
            return@Scaffold
        }
        val pager = rememberPagerState(initialPage = vm.startIndex.coerceIn(0, images.lastIndex)) { images.size }
        Box(Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                ZoomableImage(vm.fileFor(images[page]))
            }
            val current = images.getOrNull(pager.currentPage)
            Text(
                "${pager.currentPage + 1} / ${images.size}",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
            )
            if (current != null) {
                IconButton(
                    onClick = { confirmDelete = current },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                ) {
                    Icon(Icons.Outlined.Delete, stringResource(R.string.remove_image), tint = Color.White)
                }
            }
        }
    }
    confirmDelete?.let { a ->
        ConfirmDialog(
            title = stringResource(R.string.remove_image),
            text = stringResource(R.string.remove_image_body),
            confirmLabel = stringResource(R.string.action_delete),
            onConfirm = { vm.remove(a) },
            onDismiss = { confirmDelete = null },
        )
    }
}

@Composable
private fun ZoomableImage(file: File) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transform = rememberTransformableState { zoom, pan, _ ->
        scale = (scale * zoom).coerceIn(1f, 5f)
        offset = if (scale > 1f) offset + pan else Offset.Zero
    }
    LaunchedEffect(file) {
        scale = 1f
        offset = Offset.Zero
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                    }
                })
            }
            // Only claim pan gestures while zoomed so the pager can still swipe between images.
            .transformable(transform, canPan = { scale > 1f }),
        contentAlignment = Alignment.Center,
    ) {
        AttachmentImage(
            file,
            modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
            contentScale = ContentScale.Fit,
        )
    }
}
