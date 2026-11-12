package app.ghars.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.ghars.R

/**
 * Top-level screens reachable from the side menu (S20).
 * Screen IDs refer to docs/recon.md.
 */
enum class GharsDestination(
    val route: String,
    @param:StringRes val title: Int,
    @param:DrawableRes val icon: Int,
) {
    Home("home", R.string.nav_home, R.drawable.ic_house),                       // S02
    Forest("forest", R.string.nav_forest, R.drawable.ic_trees),                 // S08
    Timeline("timeline", R.string.nav_timeline, R.drawable.ic_history),         // S09
    Stats("stats", R.string.nav_stats, R.drawable.ic_chart_column),             // S10
    Tags("tags", R.string.nav_tags, R.drawable.ic_tags),                        // S11
    Store("store", R.string.nav_store, R.drawable.ic_store),                    // S12
    Challenges("challenges", R.string.nav_challenges, R.drawable.ic_target),    // S13
    Settings("settings", R.string.nav_settings, R.drawable.ic_settings);        // S18

    companion object {
        fun fromRoute(route: String?): GharsDestination =
            entries.firstOrNull { it.route == route } ?: Home
    }
}
