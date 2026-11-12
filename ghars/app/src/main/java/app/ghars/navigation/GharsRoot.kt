package app.ghars.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.ghars.R
import app.ghars.core.designsystem.GharsTheme
import app.ghars.core.ui.GharsTopBar
import app.ghars.feature.home.HomeScreen
import app.ghars.feature.placeholder.PlaceholderScreen
import kotlinx.coroutines.launch

/** App shell: side menu (S20) around the navigation graph. */
@Composable
fun GharsRoot() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val current = GharsDestination.fromRoute(backStackEntry?.destination?.route)

    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = GharsTheme.colors.night.copy(alpha = 0.4f),
        drawerContent = {
            GharsDrawer(
                current = current,
                onSelect = { destination ->
                    scope.launch { drawerState.close() }
                    navController.navigate(destination.route) {
                        // Home stays at the root; every other screen sits directly above it.
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(GharsTheme.colors.bg)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            val isHome = current == GharsDestination.Home
            GharsTopBar(
                title = stringResource(current.title),
                navIcon = if (isHome) R.drawable.ic_menu else R.drawable.ic_arrow_back,
                navIconDescription = stringResource(if (isHome) R.string.cd_open_menu else R.string.cd_back),
                onNavClick = {
                    if (isHome) scope.launch { drawerState.open() } else navController.popBackStack()
                },
            )
            NavHost(
                navController = navController,
                startDestination = GharsDestination.Home.route,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(GharsDestination.Home.route) { HomeScreen() }
                GharsDestination.entries.filter { it != GharsDestination.Home }.forEach { destination ->
                    composable(destination.route) {
                        PlaceholderScreen(
                            message = stringResource(
                                if (destination == GharsDestination.Forest) R.string.forest_empty
                                else R.string.placeholder_body,
                            ),
                        )
                    }
                }
            }
        }
    }
}
