package com.imadnote.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import com.imadnote.app.ui.LocalAppContainer
import com.imadnote.app.ui.components.Feedback
import com.imadnote.app.ui.components.LocalFeedback
import com.imadnote.app.ui.theme.Elevation
import com.imadnote.app.ui.theme.Motion
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import com.imadnote.app.ui.more.MoreScreen
import com.imadnote.app.ui.calendar.CalendarScreen
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
    const val MORE = "more"
    const val CALENDAR = "calendar"
    const val NOTE = "note/{id}?mode={mode}&projectId={projectId}"
    const val TASK = "task/{id}?projectId={projectId}&noteId={noteId}&today={today}&date={date}"
    const val PROJECT = "project/{id}"
    const val PROJECT_EDIT = "project-edit/{id}"
    const val VIEWER = "viewer/{noteId}/{index}"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ARCHIVE = "archive"

    fun note(id: String = "new", mode: String = "", projectId: String = "") = "note/$id?mode=$mode&projectId=$projectId"
    fun task(id: String = "new", projectId: String = "", noteId: String = "", today: Boolean = false, date: String = "") =
        "task/$id?projectId=$projectId&noteId=$noteId&today=$today&date=$date"
    fun project(id: String) = "project/$id"
    fun projectEdit(id: String = "new") = "project-edit/$id"
    fun viewer(noteId: String, index: Int) = "viewer/$noteId/$index"
}

private data class TopLevel(val route: String, val label: Int, val icon: ImageVector, val selectedIcon: ImageVector)

private val topLevel = listOf(
    TopLevel(Routes.HOME, R.string.tab_home, Icons.Outlined.Home, Icons.Filled.Home),
    TopLevel(Routes.NOTES, R.string.tab_notes, Icons.AutoMirrored.Outlined.StickyNote2, Icons.AutoMirrored.Filled.StickyNote2),
    TopLevel(Routes.TASKS, R.string.tab_tasks, Icons.Outlined.CheckCircle, Icons.Filled.CheckCircle),
    TopLevel(Routes.MORE, R.string.tab_more, Icons.Outlined.Menu, Icons.Filled.Menu),
)

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

private val topLevelRoutes = topLevel.map { it.route }.toSet()

private fun isTopLevelRoute(route: String?) = route in topLevelRoutes

/**
 * [pendingRoute] is a destination requested from outside the UI (reminder notification, launcher
 * shortcut). [quickCaptureRequested] opens the capture sheet.
 */
@Composable
fun ImadNoteRoot(pendingRoute: String?, onPendingRouteHandled: () -> Unit) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    val isTopLevel = isTopLevelRoute(currentRoute)
    var showCapture by rememberSaveable { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val feedback = remember(snackbarHost, scope) { Feedback(snackbarHost, scope) }
    val container = LocalAppContainer.current
    val noteSaved = stringResource(R.string.note_saved)
    val taskSaved = stringResource(R.string.task_saved)
    val openLabel = stringResource(R.string.action_open)

    LaunchedEffect(pendingRoute) {
        when (pendingRoute) {
            null -> Unit
            QUICK_CAPTURE -> showCapture = true
            else -> nav.navigate(pendingRoute)
        }
        if (pendingRoute != null) onPendingRouteHandled()
    }

    CompositionLocalProvider(LocalFeedback provides feedback) {
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = {
            SnackbarHost(snackbarHost, modifier = if (isTopLevel) Modifier else Modifier.navigationBarsPadding().imePadding())
        },
        bottomBar = {
            if (isTopLevel) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        topLevel.forEachIndexed { index, item ->
                            // Quick Capture sits in the middle of the bar, within thumb reach.
                            if (index == 2) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                    CaptureButton(onClick = { showCapture = true })
                                }
                            }
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { nav.switchTab(item.route) },
                                icon = { Icon(if (selected) item.selectedIcon else item.icon, contentDescription = null) },
                                label = { Text(stringResource(item.label), maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            // Tabs cross-fade; screens pushed on top slide in slightly from the reading direction.
            enterTransition = {
                if (isTopLevelRoute(initialState.destination.route) && isTopLevelRoute(targetState.destination.route)) {
                    fadeIn(tween(Motion.STANDARD))
                } else {
                    fadeIn(tween(Motion.SCREEN, easing = Motion.EmphasizedDecelerate)) +
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(Motion.SCREEN, easing = Motion.EmphasizedDecelerate)) { it / 16 }
                }
            },
            exitTransition = { fadeOut(tween(Motion.FAST)) },
            popEnterTransition = { fadeIn(tween(Motion.STANDARD)) },
            popExitTransition = {
                fadeOut(tween(Motion.STANDARD, easing = Motion.EmphasizedAccelerate)) +
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(Motion.STANDARD, easing = Motion.EmphasizedAccelerate)) { it / 16 }
            },
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenTask = { nav.navigate(Routes.task(it)) },
                    onOpenNote = { nav.navigate(Routes.note(it)) },
                    onOpenProject = { nav.navigate(Routes.project(it)) },
                    onSeeTasks = { nav.switchTab(Routes.TASKS) },
                    onSeeNotes = { nav.switchTab(Routes.NOTES) },
                    onSeeProjects = { nav.navigate(Routes.PROJECTS) },
                    onCalendar = { nav.navigate(Routes.CALENDAR) },
                    onSearch = { nav.navigate(Routes.SEARCH) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.TASKS) {
                TasksScreen(
                    onOpenTask = { nav.navigate(Routes.task(it)) },
                    onNewTask = { nav.navigate(Routes.task(today = true)) },
                    onCalendar = { nav.navigate(Routes.CALENDAR) },
                )
            }
            composable(Routes.MORE) {
                MoreScreen(
                    onProjects = { nav.navigate(Routes.PROJECTS) },
                    onCalendar = { nav.navigate(Routes.CALENDAR) },
                    onArchive = { nav.navigate(Routes.ARCHIVE) },
                    onSearch = { nav.navigate(Routes.SEARCH) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.CALENDAR) {
                CalendarScreen(
                    onBack = { nav.popBackStack() },
                    onOpenTask = { nav.navigate(Routes.task(it)) },
                    onNewTask = { date -> nav.navigate(Routes.task(date = date)) },
                )
            }
            composable(Routes.NOTES) {
                NotesScreen(
                    onOpenNote = { nav.navigate(Routes.note(it)) },
                    onNewNote = { nav.navigate(Routes.note()) },
                    onSearch = { nav.navigate(Routes.SEARCH) },
                    onArchive = { nav.navigate(Routes.ARCHIVE) },
                )
            }
            composable(Routes.PROJECTS) {
                ProjectsScreen(
                    onBack = { nav.popBackStack() },
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
                    navArgument("date") { type = NavType.StringType; defaultValue = "" },
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
    }

    if (showCapture) {
        QuickCaptureSheet(
            onDismiss = { showCapture = false },
            onQuickSave = { text, asTask ->
                showCapture = false
                scope.launch {
                    val id = if (asTask) container.quickCapture.saveTask(text) else container.quickCapture.saveNote(text)
                    feedback.show(if (asTask) taskSaved else noteSaved, openLabel) {
                        nav.navigate(if (asTask) Routes.task(id) else Routes.note(id))
                    }
                }
            },
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

const val QUICK_CAPTURE = "quick-capture"

/** Raised circular Quick Capture button embedded in the navigation bar. */
@Composable
private fun CaptureButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shadowElevation = Elevation.float,
        modifier = Modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, stringResource(R.string.quick_capture), modifier = Modifier.size(28.dp))
        }
    }
}
