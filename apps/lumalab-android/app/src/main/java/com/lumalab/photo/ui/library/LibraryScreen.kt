package com.lumalab.photo.ui.library

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumalab.photo.LumaLabApp
import com.lumalab.photo.R
import com.lumalab.photo.data.Flag
import com.lumalab.photo.data.PhotoEntry
import com.lumalab.photo.ui.theme.Accent
import com.lumalab.photo.ui.theme.Gold
import com.lumalab.photo.ui.theme.Surface2
import com.lumalab.photo.ui.theme.TextMuted
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(onOpenPhoto: (String) -> Unit) {
    val vm: LibraryViewModel = viewModel { LibraryViewModel(this[APPLICATION_KEY] as LumaLabApp) }
    val state by vm.state.collectAsStateWithLifecycle()
    var showNewAlbum by rememberSaveable { mutableStateOf(false) }
    var newAlbumForSelection by rememberSaveable { mutableStateOf(false) }
    var showAlbumPicker by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(100)) { uris ->
        vm.import(uris)
    }
    val launchPicker = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (state.selecting) {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface2),
                    navigationIcon = {
                        IconButton(onClick = vm::clearSelection) { Icon(Icons.Filled.Close, stringResource(R.string.cancel)) }
                    },
                    title = { Text(stringResource(R.string.selected_count, state.selection.size)) },
                    actions = {
                        IconButton(onClick = vm::selectAll) { Icon(Icons.Filled.SelectAll, stringResource(R.string.select_all)) }
                        if (state.canPaste) {
                            IconButton(onClick = vm::pasteToSelected) { Icon(Icons.Filled.ContentPaste, stringResource(R.string.paste_settings)) }
                        }
                        if (state.filter is LibraryFilter.InAlbum) {
                            IconButton(onClick = vm::removeSelectedFromAlbum) {
                                Icon(Icons.Filled.RemoveCircleOutline, stringResource(R.string.remove_from_album))
                            }
                        } else {
                            IconButton(onClick = { showAlbumPicker = true }) {
                                Icon(Icons.Filled.CreateNewFolder, stringResource(R.string.add_to_album))
                            }
                        }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, stringResource(R.string.delete)) }
                    },
                )
            } else {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.SemiBold) },
                    actions = {
                        if (state.filter is LibraryFilter.InAlbum) {
                            Box {
                                IconButton(onClick = { menuOpen = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.more)) }
                                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.delete_album)) },
                                        onClick = { menuOpen = false; vm.deleteCurrentAlbum() },
                                    )
                                }
                            }
                        }
                    },
                )
            }
        },
        floatingActionButton = {
            if (!state.selecting) {
                ExtendedFloatingActionButton(
                    onClick = launchPicker,
                    icon = { Icon(Icons.Filled.Add, null) },
                    text = { Text(stringResource(R.string.add_photos)) },
                    containerColor = Accent,
                    contentColor = Color.Black,
                )
            }
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            if (state.importing) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Accent)
            FilterRow(
                state = state,
                onSelect = vm::setFilter,
                onNewAlbum = { newAlbumForSelection = false; showNewAlbum = true },
            )
            when {
                state.totalCount == 0 -> EmptyLibrary(onImport = launchPicker)
                state.photos.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.empty_filter), color = TextMuted)
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(112.dp),
                    contentPadding = PaddingValues(start = 2.dp, end = 2.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.photos, key = { it.id }) { photo ->
                        PhotoCell(
                            entry = photo,
                            file = vm.fileFor(photo),
                            selected = photo.id in state.selection,
                            selecting = state.selecting,
                            onClick = { if (state.selecting) vm.toggleSelection(photo.id) else onOpenPhoto(photo.id) },
                            onLongClick = { vm.toggleSelection(photo.id) },
                        )
                    }
                }
            }
        }
    }

    if (showNewAlbum) {
        NewAlbumDialog(
            onDismiss = { showNewAlbum = false },
            onCreate = { name ->
                vm.createAlbum(name, addSelection = newAlbumForSelection)
                showNewAlbum = false
            },
        )
    }
    if (showAlbumPicker) {
        AlertDialog(
            onDismissRequest = { showAlbumPicker = false },
            title = { Text(stringResource(R.string.add_to_album)) },
            text = {
                Column {
                    state.albums.forEach { album ->
                        TextButton(onClick = { vm.addSelectedToAlbum(album.id); showAlbumPicker = false }) {
                            Text(album.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    TextButton(onClick = { showAlbumPicker = false; newAlbumForSelection = true; showNewAlbum = true }) {
                        Icon(Icons.Filled.Add, null)
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.new_album), modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showAlbumPicker = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { vm.deleteSelected(); confirmDelete = false }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun FilterRow(state: LibraryUiState, onSelect: (LibraryFilter) -> Unit, onNewAlbum: () -> Unit) {
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = Accent.copy(alpha = 0.22f),
        selectedLabelColor = Accent,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    ) {
        val fixed = listOf(
            LibraryFilter.All to R.string.all_photos,
            LibraryFilter.Picks to R.string.picks,
            LibraryFilter.Edited to R.string.edited,
            LibraryFilter.Rated to R.string.rated,
        )
        items(fixed) { (filter, label) ->
            FilterChip(
                selected = state.filter == filter,
                onClick = { onSelect(filter) },
                label = { Text(stringResource(label)) },
                colors = chipColors,
            )
        }
        items(state.albums, key = { it.id }) { album ->
            FilterChip(
                selected = state.filter == LibraryFilter.InAlbum(album.id),
                onClick = { onSelect(LibraryFilter.InAlbum(album.id)) },
                label = { Text(album.name) },
                leadingIcon = { Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(16.dp)) },
                colors = chipColors,
            )
        }
        item {
            FilterChip(
                selected = false,
                onClick = onNewAlbum,
                label = { Text(stringResource(R.string.new_album)) },
                leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) },
                colors = chipColors,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoCell(
    entry: PhotoEntry,
    file: File,
    selected: Boolean,
    selecting: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Box(
        Modifier
            .aspectRatio(1f)
            .background(Surface2)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .then(if (selected) Modifier.border(3.dp, Accent) else Modifier),
    ) {
        Thumbnail(entry, file, Modifier.fillMaxSize())
        Row(
            Modifier.align(Alignment.BottomStart).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (entry.flag == Flag.PICK) Icon(Icons.Filled.Flag, null, tint = Color.White, modifier = Modifier.size(14.dp))
            if (entry.flag == Flag.REJECT) Icon(Icons.Filled.Flag, null, tint = Color(0xFFFF5A5A), modifier = Modifier.size(14.dp))
            if (entry.rating > 0) {
                Icon(Icons.Filled.Star, null, tint = Gold, modifier = Modifier.size(14.dp))
                Text(entry.rating.toString(), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (entry.isEdited) {
            Icon(
                Icons.Filled.AutoFixHigh, null, tint = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(14.dp),
            )
        }
        if (selecting) {
            Icon(
                Icons.Filled.CheckCircle, null,
                tint = if (selected) Accent else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(20.dp).background(Color.Black.copy(alpha = 0.3f), CircleShape),
            )
        }
    }
}

@Composable
fun Thumbnail(entry: PhotoEntry, file: File, modifier: Modifier = Modifier) {
    val cache = (LocalContext.current.applicationContext as LumaLabApp).thumbnails
    val bitmap by produceState<Bitmap?>(initialValue = cache.cached(entry), entry.id, entry.edits) {
        value = cache.load(entry, file)
    }
    bitmap?.let {
        Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = modifier)
    }
}

@Composable
private fun EmptyLibrary(onImport: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.AddPhotoAlternate, null, tint = Accent, modifier = Modifier.size(72.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.empty_library_title), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.empty_library_body), color = TextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onImport) { Text(stringResource(R.string.add_photos), color = Accent) }
    }
}

@Composable
private fun NewAlbumDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_album)) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, label = { Text(stringResource(R.string.album_name)) })
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text(stringResource(R.string.create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
