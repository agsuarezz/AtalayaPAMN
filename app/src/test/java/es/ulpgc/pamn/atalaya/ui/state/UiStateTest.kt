package es.ulpgc.pamn.atalaya.ui.state

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class UiStateTest {

    @Test
    fun `stateflow emite transiciones Loading Success y Error en orden`() = runTest {
        val state = MutableStateFlow<UiState<String>>(UiState.Loading)

        state.test {
            assertEquals(UiState.Loading, awaitItem())

            state.value = UiState.Success("vault_ready")
            assertEquals(UiState.Success("vault_ready"), awaitItem())

            state.value = UiState.Error("Error de prueba")
            assertEquals(UiState.Error("Error de prueba"), awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
