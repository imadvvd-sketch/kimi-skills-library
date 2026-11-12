package app.ghars.navigation

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.ghars.domain.session.SessionState
import app.ghars.feature.session.ResultScreen
import app.ghars.feature.session.SessionScreen
import app.ghars.feature.session.SessionViewModel
import app.ghars.feature.species.SpeciesPickerScreen
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

private const val ROUTE_SPECIES = "species"

/**
 * Decides what the whole screen shows. A live session or an unseen result always wins over
 * browsing, so they survive rotation and process death without any back-stack bookkeeping.
 */
@Composable
fun GharsRoot(viewModel: SessionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dark = state is SessionState.Active || state is SessionState.Finished

    val activity = LocalActivity.current as? ComponentActivity
    LaunchedEffect(dark) {
        val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        activity?.enableEdgeToEdge(bars, bars)
    }

    when (val current = state) {
        // Still reading the saved session; the window background covers this instant.
        null -> Unit
        SessionState.Idle -> BrowsingShell()
        is SessionState.Active -> GharsTheme(focusMode = true) {
            SessionScreen(state = current, now = viewModel::now, onGiveUp = viewModel::giveUp)
        }
        is SessionState.Finished -> GharsTheme(focusMode = true) {
            ResultScreen(state = current, onDone = viewModel::acknowledgeResult)
        }
    }
}

/** Side menu (S20) around the navigation graph. */
@Composable
private fun BrowsingShell() {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val current = GharsDestination.fromRoute(route)
    val onSpeciesPicker = route == ROUTE_SPECIES

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
            val isHome = current == GharsDestination.Home && !onSpeciesPicker
            GharsTopBar(
                title = stringResource(if (onSpeciesPicker) R.string.species_picker_title else current.title),
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
                composable(GharsDestination.Home.route) {
                    HomeScreen(onChooseSpecies = { navController.navigate(ROUTE_SPECIES) { launchSingleTop = true } })
                }
                composable(ROUTE_SPECIES) { SpeciesPickerScreen(onDone = { navController.popBackStack() }) }
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
