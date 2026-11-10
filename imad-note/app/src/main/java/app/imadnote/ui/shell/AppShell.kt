package app.imadnote.ui.shell

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.imadnote.LocalAppContainer
import app.imadnote.R
import app.imadnote.data.db.PageKind
import app.imadnote.data.repo.SearchIndexer
import app.imadnote.ui.analytics.AnalyticsScreen
import app.imadnote.ui.calendar.CalendarScreen
import app.imadnote.ui.databases.DatabaseScreen
import app.imadnote.ui.databases.DatabasesScreen
import app.imadnote.ui.editor.EditorScreen
import app.imadnote.ui.editor.openAttachment
import app.imadnote.ui.home.HomeScreen
import app.imadnote.ui.inbox.InboxScreen
import app.imadnote.ui.notes.NotesScreen
import app.imadnote.ui.projects.ProjectDetailScreen
import app.imadnote.ui.projects.ProjectsScreen
import app.imadnote.ui.search.SearchScreen
import app.imadnote.ui.settings.SettingsScreen
import app.imadnote.ui.settings.TrashScreen
import app.imadnote.ui.tasks.TaskDetailScreen
import app.imadnote.ui.tasks.TasksScreen
import app.imadnote.ui.theme.ImadTheme
import kotlinx.coroutines.launch

/** Top-level destinations, all listed in the sidebar. */
enum class Destination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    HOME("home", R.string.nav_home, Icons.Rounded.Home),
    INBOX("inbox", R.string.nav_inbox, Icons.Rounded.Inbox),
    NOTES("notes", R.string.nav_notes, Icons.AutoMirrored.Rounded.Notes),
    TASKS("tasks", R.string.nav_tasks, Icons.Rounded.TaskAlt),
    PROJECTS("projects", R.string.nav_projects, Icons.Rounded.RocketLaunch),
    DATABASES("databases", R.string.nav_databases, Icons.Rounded.Storage),
    CALENDAR("calendar", R.string.nav_calendar, Icons.Rounded.CalendarMonth),
    ANALYTICS("analytics", R.string.nav_analytics, Icons.Rounded.Insights),
    SETTINGS("settings", R.string.nav_settings, Icons.Rounded.Settings),
}

/** Phones show four primary destinations plus "More", which opens the sidebar with everything else. */
private val bottomBar = listOf(Destination.HOME, Destination.NOTES, Destination.TASKS, Destination.CALENDAR)

object Routes {
    const val TRASH = "trash"
    const val SEARCH = "search"
    const val PAGE = "page/{pageId}"
    const val TASK = "task/{taskId}"
    const val PROJECT = "project/{projectId}"
    const val DATABASE = "database/{collectionId}"
    fun page(id: String) = "page/$id"
    fun task(id: String) = "task/$id"
    fun project(id: String) = "project/$id"
    fun database(id: String) = "database/$id"

    /** Notification keys are `task:<id>` or `event:<id>`. */
    fun fromReminderKey(key: String): String? = when {
        key.startsWith("task:") -> task(key.removePrefix("task:"))
        key.startsWith("event:") -> Destination.CALENDAR.route
        else -> null
    }
}

private val detailRoutes = setOf(Routes.PAGE, Routes.TASK, Routes.PROJECT, Routes.DATABASE, Routes.SEARCH, Routes.TRASH)

private fun NavHostController.navigateTop(route: String) = navigate(route) {
    popUpTo(graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/**
 * Adaptive frame: bottom bar plus a slide-in sidebar on phones, a permanent sidebar from 840dp.
 * The sidebar holds the workspace switcher, every destination and the page tree at every size.
 */
@Composable
fun AppShell(viewModel: ShellViewModel, navController: NavHostController = rememberNavController()) {
    val colors = ImadTheme.colors
    val context = LocalContext.current
    val attachments = LocalAppContainer.current.attachments
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val workspace by viewModel.workspace.collectAsStateWithLifecycle()
    val tree by viewModel.tree.collectAsStateWithLifecycle()
    val pending by viewModel.pendingRoute.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pending) {
        pending?.let { navController.navigate(it) { launchSingleTop = true } }
        viewModel.pendingRoute.value = null
    }

    fun go(r: String) = navController.navigate(r) { launchSingleTop = true }
    fun openPage(id: String) = go(Routes.page(id))
    val actions = remember(viewModel, navController) {
        PageActions(
            onOpen = { openPage(it.id) },
            onCreateInside = { parent -> viewModel.createPage(parentId = parent.id) { openPage(it.id) } },
            onEdit = viewModel::editPage,
            onMove = viewModel::movePage,
            onDuplicate = viewModel::duplicatePage,
            onTrash = viewModel::trashPage,
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(colors.background)) {
        val wide = maxWidth >= 840.dp
        val inDetail = route in detailRoutes

        val sidebar: @Composable (Modifier, () -> Unit) -> Unit = { modifier, afterAction ->
            Sidebar(
                workspaces = workspaces, current = workspace, tree = tree, currentRoute = route,
                actions = remember(actions, afterAction) {
                    PageActions(
                        onOpen = { afterAction(); actions.onOpen(it) },
                        onCreateInside = { afterAction(); actions.onCreateInside(it) },
                        onEdit = actions.onEdit, onMove = actions.onMove, onDuplicate = actions.onDuplicate, onTrash = actions.onTrash,
                    )
                },
                onNavigate = { afterAction(); navController.navigateTop(it.route) },
                onOpenTrash = { afterAction(); go(Routes.TRASH) },
                onOpenSearch = { afterAction(); go(Routes.SEARCH) },
                onSelectWorkspace = viewModel::selectWorkspace,
                onCreateWorkspace = viewModel::createWorkspace,
                onNewPage = { afterAction(); viewModel.createPage { openPage(it.id) } },
                onNewFolder = { name -> viewModel.createPage(kind = PageKind.FOLDER, title = name) },
                modifier = modifier,
            )
        }

        val content: @Composable () -> Unit = {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    NavHost(navController, startDestination = Destination.HOME.route) {
                        val openMenu: (() -> Unit)? = if (wide) null else ({ drawerOpen = true })
                        composable(Destination.HOME.route) {
                            HomeScreen(
                                workspace, tree, actions, openMenu, onNewPage = { viewModel.createPage { openPage(it.id) } },
                                onSearch = { go(Routes.SEARCH) }, onOpenTask = { go(Routes.task(it)) }, onOpenTasks = { navController.navigateTop(Destination.TASKS.route) },
                            )
                        }
                        composable(Destination.INBOX.route) { InboxScreen(tree, actions, openMenu, viewModel) }
                        composable(Destination.NOTES.route) {
                            NotesScreen(tree, actions, openMenu, onNewPage = { viewModel.createPage { openPage(it.id) } }, onNewFolder = { viewModel.createPage(kind = PageKind.FOLDER, title = it) })
                        }
                        composable(Destination.TASKS.route) { TasksScreen(openMenu, onOpenTask = { go(Routes.task(it)) }, onSearch = { go(Routes.SEARCH) }) }
                        composable(Destination.PROJECTS.route) { ProjectsScreen(openMenu, onOpenProject = { go(Routes.project(it)) }) }
                        composable(Destination.DATABASES.route) { DatabasesScreen(openMenu, onOpen = { go(Routes.database(it)) }) }
                        composable(Destination.CALENDAR.route) { CalendarScreen(openMenu, onOpenTask = { go(Routes.task(it)) }, onOpenProject = { go(Routes.project(it)) }) }
                        composable(Destination.ANALYTICS.route) { AnalyticsScreen(openMenu) }
                        composable(Destination.SETTINGS.route) { SettingsScreen(viewModel, openMenu, onOpenTrash = { go(Routes.TRASH) }) }
                        composable(Routes.TRASH) { TrashScreen(viewModel, onBack = { navController.popBackStack() }) }
                        composable(Routes.SEARCH) {
                            SearchScreen(onBack = { navController.popBackStack() }) { result ->
                                when (result.kind) {
                                    SearchIndexer.PAGE -> openPage(result.refId)
                                    SearchIndexer.BLOCK -> openPage(result.parentRef)
                                    SearchIndexer.TASK -> go(Routes.task(result.refId))
                                    SearchIndexer.PROJECT -> go(Routes.project(result.refId))
                                    SearchIndexer.RECORD -> go(Routes.database(result.parentRef))
                                    SearchIndexer.EVENT -> navController.navigateTop(Destination.CALENDAR.route)
                                    SearchIndexer.ATTACHMENT -> scope.launch {
                                        attachments.get(result.refId)?.let { openAttachment(context, attachments.shareUri(it), it.mimeType) }
                                    }
                                }
                            }
                        }
                        composable(Routes.PAGE, arguments = listOf(navArgument("pageId") { type = NavType.StringType })) { entry ->
                            EditorScreen(entry.arguments?.getString("pageId").orEmpty(), onBack = { navController.popBackStack() }, onOpenPage = ::openPage)
                        }
                        composable(Routes.TASK, arguments = listOf(navArgument("taskId") { type = NavType.StringType })) { entry ->
                            TaskDetailScreen(entry.arguments?.getString("taskId").orEmpty(), onBack = { navController.popBackStack() }, onOpenTask = { go(Routes.task(it)) })
                        }
                        composable(Routes.PROJECT, arguments = listOf(navArgument("projectId") { type = NavType.StringType })) { entry ->
                            ProjectDetailScreen(entry.arguments?.getString("projectId").orEmpty(), onBack = { navController.popBackStack() }, onOpenTask = { go(Routes.task(it)) }, onOpenPage = ::openPage)
                        }
                        composable(Routes.DATABASE, arguments = listOf(navArgument("collectionId") { type = NavType.StringType })) { entry ->
                            DatabaseScreen(entry.arguments?.getString("collectionId").orEmpty(), onBack = { navController.popBackStack() })
                        }
                    }
                }
                if (!wide && !inDetail) {
                    HorizontalDivider(color = colors.border)
                    NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                        val itemColors = NavigationBarItemDefaults.colors(
                            selectedIconColor = colors.primary, selectedTextColor = colors.primary, indicatorColor = colors.primarySoft,
                            unselectedIconColor = colors.textSecondary, unselectedTextColor = colors.textSecondary,
                        )
                        for (destination in bottomBar) {
                            NavigationBarItem(
                                selected = route == destination.route,
                                onClick = { navController.navigateTop(destination.route) },
                                modifier = Modifier.testTag("tab-${destination.route}"),
                                icon = { Icon(destination.icon, null) },
                                label = { Text(stringResource(destination.label), style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                                colors = itemColors,
                            )
                        }
                        NavigationBarItem(
                            selected = route != null && bottomBar.none { it.route == route },
                            onClick = { drawerOpen = true },
                            modifier = Modifier.testTag("tab-more"),
                            icon = { Icon(Icons.Rounded.Menu, null) },
                            label = { Text(stringResource(R.string.nav_more), style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                            colors = itemColors,
                        )
                    }
                }
            }
        }

        if (wide) {
            Row(Modifier.fillMaxSize()) {
                sidebar(Modifier.width(288.dp).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))) {}
                Box(Modifier.width(1.dp).fillMaxHeight().background(colors.border))
                Box(Modifier.weight(1f)) { content() }
            }
        } else {
            SideDrawer(
                open = drawerOpen,
                onClose = { drawerOpen = false },
                drawer = { sidebar(Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))) { drawerOpen = false } },
                content = content,
            )
        }
    }
}
