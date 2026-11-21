package es.ulpgc.pamn.atalaya.core.security

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultSessionManagerTest {

    @Test
    fun `estado inicial esta bloqueado y withDek lanza VaultLockedException`() {
        val manager = VaultSessionManager()

        assertFalse(manager.isUnlocked.value)
        assertThrows(CryptoException.VaultLockedException::class.java) {
            manager.withDek { }
        }
    }

    @Test
    fun `isUnlocked emite secuencia false true false al desbloquear y bloquear`() = runTest {
        val manager = VaultSessionManager()
        val dek = ByteArray(32) { (it + 1).toByte() }

        manager.isUnlocked.test {
            assertFalse(awaitItem())

            manager.unlock(dek)
            assertTrue(awaitItem())

            manager.lock()
            assertFalse(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `unlock guarda copia defensiva y withDek limpia la copia temporal tras ejecutarse`() {
        val manager = VaultSessionManager()
        val externalDek = ByteArray(32) { 0x5A }

        manager.unlock(externalDek)
        externalDek.fill(0)

        var capturedCopy: ByteArray? = null
        val result = manager.withDek { dek ->
            capturedCopy = dek
            assertArrayEquals(ByteArray(32) { 0x5A }, dek)
            "ok"
        }

        assertEquals("ok", result)
        assertArrayEquals(ByteArray(32) { 0 }, capturedCopy)

        manager.lock()
        assertThrows(CryptoException.VaultLockedException::class.java) {
            manager.withDek { }
        }
    }

    @Test
    fun `unlock rechaza clave de longitud distinta a 32 bytes`() {
        val manager = VaultSessionManager()

        assertThrows(IllegalArgumentException::class.java) {
            manager.unlock(ByteArray(16))
        }
    }
}
