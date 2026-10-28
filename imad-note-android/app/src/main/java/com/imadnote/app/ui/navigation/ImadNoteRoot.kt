package com.imadnote.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.imadnote.app.R
import com.imadnote.app.ui.capture.CaptureType
import com.imadnote.app.ui.capture.QuickCaptureSheet
import com.imadnote.app.ui.home.HomeScreen
import com.imadnote.app.ui.notes.ArchiveScreen
import com.imadnote.app.ui.notes.ImageViewerScreen
import com.imadnote.app.ui.notes.NoteEditorScreen
import com.imadnote.app.ui.notes.NoteEditorViewModel
import com.imadnote.app.ui.notes.NotesScreen
import com.imadnote.app.ui.projects.ProjectDetailScreen
import com.imadnote.app.ui.projects.ProjectEditScreen
import com.imadnote.app.ui.projects.ProjectsScreen
import com.imadnote.app.ui.search.SearchScreen
import com.imadnote.app.ui.settings.SettingsScreen
import com.imadnote.app.ui.tasks.TaskEditorScreen
import com.imadnote.app.ui.tasks.TasksScreen

object Routes {
    const val HOME = "home"
    const val TASKS = "tasks"
    const val NOTES = "notes"
    const val PROJECTS = "projects"
    const val NOTE = "note/{id}?mode={mode}&projectId={projectId}"
    const val TASK = "task/{id}?projectId={projectId}&noteId={noteId}&today={today}"
    const val PROJECT = "project/{id}"
    const val PROJECT_EDIT = "project-edit/{id}"
    const val VIEWER = "viewer/{noteId}/{index}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ARCHIVE = "archive"

    fun note(id: String = "new", mode: String = "", projectId: String = "") = "note/$id?mode=$mode&projectId=$projectId"
    fun task(id: String = "new", projectId: String = "", noteId: String = "", today: Boolean = false) =
        "task/$id?projectId=$projectId&noteId=$noteId&today=$today"
    fun project(id: String) = "project/$id"
    fun projectEdit(id: String = "new") = "project-edit/$id"
    fun viewer(noteId: String, index: Int) = "viewer/$noteId/$index"
}

private data class TopLevel(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

private val topLevel = listOf(
    TopLevel(Routes.HOME, R.string.tab_home, Icons.Outlined.Home, Icons.Filled.Home),
    TopLevel(Routes.TASKS, R.string.tab_tasks, Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
    TopLevel(Routes.NOTES, R.string.tab_notes, Icons.AutoMirrored.Outlined.StickyNote2, Icons.AutoMirrored.Filled.StickyNote2),
    TopLevel(Routes.PROJECTS, R.string.tab_projects, Icons.Outlined.Folder, Icons.Filled.Folder),
)

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
fun ImadNoteRoot(pendingTaskId: String?, onPendingTaskHandled: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val isTopLevel = topLevel.any { it.route == currentRoute }
    var showCapture by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(pendingTaskId) {
        if (pendingTaskId != null) {
            nav.navigate(Routes.task(pendingTaskId))
            onPendingTaskHandled()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (isTopLevel) {
                NavigationBar {
                    topLevel.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.switchTab(item.route) },
                            icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (isTopLevel) {
                FloatingActionButton(onClick = { showCapture = true }) {
                    Icon(Icons.Filled.Add, stringResource(R.string.quick_capture))
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenTask = { nav.navigate(Routes.task(it)) },
                    onOpenNote = { nav.navigate(Routes.note(it)) },
                    onOpenProject = { nav.navigate(Routes.project(it)) },
                    onSeeTasks = { nav.switchTab(Routes.TASKS) },
                    onSeeNotes = { nav.switchTab(Routes.NOTES) },
                    onSeeProjects = { nav.switchTab(Routes.PROJECTS) },
                    onSearch = { nav.navigate(Routes.SEARCH) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.TASKS) {
                TasksScreen(onOpenTask = { nav.navigate(Routes.task(it)) }, onSearch = { nav.navigate(Routes.SEARCH) })
            }
            composable(Routes.NOTES) {
                NotesScreen(
                    onOpenNote = { nav.navigate(Routes.note(it)) },
                    onSearch = { nav.navigate(Routes.SEARCH) },
                    onArchive = { nav.navigate(Routes.ARCHIVE) },
                )
            }
            composable(Routes.PROJECTS) {
                ProjectsScreen(
                    onOpenProject = { nav.navigate(Routes.project(it)) },
                    onNewProject = { nav.navigate(Routes.projectEdit()) },
                    onSearch = { nav.navigate(Routes.SEARCH) },
                )
            }
            composable(
                Routes.NOTE,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("mode") { type = NavType.StringType; defaultValue = "" },
                    navArgument("projectId") { type = NavType.StringType; defaultValue = "" },
                ),
            ) {
                NoteEditorScreen(
                    onBack = { nav.popBackStack() },
                    onOpenImage = { noteId, index -> nav.navigate(Routes.viewer(noteId, index)) },
                    onOpenTask = { nav.navigate(Routes.task(it)) },
                    onNewLinkedTask = { noteId -> nav.navigate(Routes.task(noteId = noteId)) },
                )
            }
            composable(
                Routes.TASK,
                arguments = listOf(
                    navArgument("id") { type = NavType.StringType },
                    navArgument("projectId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("noteId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("today") { type = NavType.BoolType; defaultValue = false },
                ),
            ) {
                TaskEditorScreen(onBack = { nav.popBackStack() }, onOpenNote = { nav.navigate(Routes.note(it)) })
            }
            composable(Routes.PROJECT, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                ProjectDetailScreen(
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.projectEdit(it)) },
                    onOpenTask = { nav.navigate(Routes.task(it)) },
                    onOpenNote = { nav.navigate(Routes.note(it)) },
                    onNewTask = { nav.navigate(Routes.task(projectId = it)) },
                    onNewNote = { nav.navigate(Routes.note(projectId = it)) },
                    onOpenImage = { noteId, index -> nav.navigate(Routes.viewer(noteId, index)) },
                )
            }
            composable(Routes.PROJECT_EDIT, arguments = listOf(navArgument("id") { type = NavType.StringType })) {
                ProjectEditScreen(
                    onBack = { nav.popBackStack() },
                    onSaved = { id, isNew ->
                        nav.popBackStack()
                        if (isNew) nav.navigate(Routes.project(id))
                    },
                )
            }
            composable(
                Routes.VIEWER,
                arguments = listOf(
                    navArgument("noteId") { type = NavType.StringType },
                    navArgument("index") { type = NavType.IntType },
                ),
            ) {
                ImageViewerScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { nav.popBackStack() },
                    onOpenNote = { nav.navigate(Routes.note(it)) },
                    onOpenTask = { nav.navigate(Routes.task(it)) },
                    onOpenProject = { nav.navigate(Routes.project(it)) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(onBack = { nav.popBackStack() }, onArchive = { nav.navigate(Routes.ARCHIVE) })
            }
            composable(Routes.ARCHIVE) {
                ArchiveScreen(onBack = { nav.popBackStack() }, onOpenNote = { nav.navigate(Routes.note(it)) })
            }
        }
    }

    if (showCapture) {
        QuickCaptureSheet(
            onDismiss = { showCapture = false },
            onPick = { type ->
                showCapture = false
                val route = when (type) {
                    CaptureType.NOTE -> Routes.note()
                    CaptureType.TASK -> Routes.task(today = currentRoute == Routes.TASKS || currentRoute == Routes.HOME)
                    CaptureType.CHECKLIST -> Routes.note(mode = NoteEditorViewModel.MODE_CHECKLIST)
                    CaptureType.IMAGE -> Routes.note(mode = NoteEditorViewModel.MODE_IMAGE)
                    CaptureType.IDEA -> Routes.note(mode = NoteEditorViewModel.MODE_IDEA)
                }
                nav.navigate(route)
            },
        )
    }
}
