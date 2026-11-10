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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
import app.imadnote.R
import app.imadnote.data.db.PageKind
import app.imadnote.ui.editor.EditorScreen
import app.imadnote.ui.home.HomeScreen
import app.imadnote.ui.inbox.InboxScreen
import app.imadnote.ui.notes.NotesScreen
import app.imadnote.ui.settings.SettingsScreen
import app.imadnote.ui.settings.TrashScreen
import app.imadnote.ui.theme.ImadTheme
import kotlinx.coroutines.launch

/** Top-level destinations. Destinations are added here as their modules are implemented. */
enum class Destination(val route: String, @StringRes val label: Int, val icon: ImageVector) {
    HOME("home", R.string.nav_home, Icons.Rounded.Home),
    INBOX("inbox", R.string.nav_inbox, Icons.Rounded.Inbox),
    NOTES("notes", R.string.nav_notes, Icons.AutoMirrored.Rounded.Notes),
    SETTINGS("settings", R.string.nav_settings, Icons.Rounded.Settings),
}

object Routes {
    const val TRASH = "trash"
    const val PAGE = "page/{pageId}"
    fun page(id: String) = "page/$id"
}

private fun NavHostController.navigateTop(route: String) = navigate(route) {
    popUpTo(graph.startDestinationId) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/**
 * Adaptive frame: bottom bar plus a slide-in sidebar on phones, a permanent sidebar from 840dp.
 * The sidebar holds the workspace switcher and the page tree at every size.
 */
@Composable
fun AppShell(viewModel: ShellViewModel, navController: NavHostController = rememberNavController()) {
    val colors = ImadTheme.colors
    val workspaces by viewModel.workspaces.collectAsStateWithLifecycle()
    val workspace by viewModel.workspace.collectAsStateWithLifecycle()
    val tree by viewModel.tree.collectAsStateWithLifecycle()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    fun openPage(id: String) = navController.navigate(Routes.page(id)) { launchSingleTop = true }
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
        val inEditor = route == Routes.PAGE

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
                onOpenTrash = { afterAction(); navController.navigate(Routes.TRASH) { launchSingleTop = true } },
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
                        val openMenu: (() -> Unit)? = if (wide) null else ({ scope.launch { drawer.open() } })
                        composable(Destination.HOME.route) {
                            HomeScreen(workspace, tree, actions, openMenu, onNewPage = { viewModel.createPage { openPage(it.id) } })
                        }
                        composable(Destination.INBOX.route) { InboxScreen(tree, actions, openMenu, onCapture = viewModel::capture) }
                        composable(Destination.NOTES.route) {
                            NotesScreen(tree, actions, openMenu, onNewPage = { viewModel.createPage { openPage(it.id) } }, onNewFolder = { viewModel.createPage(kind = PageKind.FOLDER, title = it) })
                        }
                        composable(Destination.SETTINGS.route) {
                            SettingsScreen(viewModel, openMenu, onOpenTrash = { navController.navigate(Routes.TRASH) { launchSingleTop = true } })
                        }
                        composable(Routes.TRASH) { TrashScreen(viewModel, onBack = { navController.popBackStack() }) }
                        composable(Routes.PAGE, arguments = listOf(navArgument("pageId") { type = NavType.StringType })) { entry ->
                            EditorScreen(
                                pageId = entry.arguments?.getString("pageId").orEmpty(),
                                onBack = { navController.popBackStack() },
                                onOpenPage = ::openPage,
                            )
                        }
                    }
                }
                if (!wide && !inEditor && route != Routes.TRASH) {
                    HorizontalDivider(color = colors.border)
                    NavigationBar(containerColor = colors.surface, tonalElevation = 0.dp) {
                        for (destination in Destination.entries) {
                            NavigationBarItem(
                                selected = route == destination.route,
                                onClick = { navController.navigateTop(destination.route) },
                                modifier = Modifier.testTag("tab-${destination.route}"),
                                icon = { Icon(destination.icon, null) },
                                label = { Text(stringResource(destination.label), style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = colors.primary, selectedTextColor = colors.primary, indicatorColor = colors.primarySoft,
                                    unselectedIconColor = colors.textSecondary, unselectedTextColor = colors.textSecondary,
                                ),
                            )
                        }
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
            ModalNavigationDrawer(
                drawerState = drawer,
                gesturesEnabled = !inEditor,
                drawerContent = {
                    ModalDrawerSheet(drawerContainerColor = colors.surface, modifier = Modifier.width(304.dp)) {
                        sidebar(Modifier) { scope.launch { drawer.close() } }
                    }
                },
                content = content,
            )
        }
    }
}
