package app.ghars.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class GharsDestinationTest {
    @Test
    fun `routes are unique`() {
        val routes = GharsDestination.entries.map { it.route }
        assertEquals(routes.size, routes.toSet().size)
    }

    @Test
    fun `unknown or missing route falls back to home`() {
        assertEquals(GharsDestination.Home, GharsDestination.fromRoute(null))
        assertEquals(GharsDestination.Home, GharsDestination.fromRoute("nope"))
        assertEquals(GharsDestination.Store, GharsDestination.fromRoute("store"))
    }
}
