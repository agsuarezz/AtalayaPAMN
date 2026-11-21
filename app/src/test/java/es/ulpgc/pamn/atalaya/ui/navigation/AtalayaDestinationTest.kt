package es.ulpgc.pamn.atalaya.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AtalayaDestinationTest {

    @Test
    fun `todas las rutas de navegacion son unicas y no vacias`() {
        val routes = AtalayaDestination.entries.map { it.route }

        assertTrue(routes.all { it.isNotBlank() })
        assertEquals(routes.size, routes.toSet().size)
    }
}
